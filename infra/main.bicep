// HRV backend infrastructure (resource-group scope).
// Provisions: Log Analytics, ACR, Key Vault, PostgreSQL Flexible Server,
// Container Apps Environment + Container App with a managed identity.
//
// Exam mapping (AZ-401 / AZ-400 DevOps):
//   - IaC with Bicep: declarative, idempotent, what-if before deploy
//   - Secrets in Key Vault, injected as Container App secrets (never in git)
//   - Managed identity + AcrPull role: no ACR admin credentials
//   - Log Analytics: centralized container logs for monitoring
targetScope = 'resourceGroup'

@description('Name prefix for all resources, e.g. hrv-dev or hrv-prod.')
param environmentName string

@description('Azure region for all resources.')
param location string = resourceGroup().location

@description('PostgreSQL admin username.')
param dbAdminLogin string = 'hrvadmin'

@description('PostgreSQL admin password. Pass via CLI or pipeline secret, never commit it.')
@secure()
param dbAdminPassword string

@description('JWT signing secret (Base64 256-bit). Pass via CLI or pipeline secret.')
@secure()
param jwtSecret string

@description('Container image for the API. Placeholder until the real image is pushed to ACR.')
param containerImage string = 'mcr.microsoft.com/azuredocs/containerapps-helloworld:latest'

@description('PostgreSQL SKU. B1ms burstable is the cheapest learning option.')
param postgresSku string = 'Standard_B1ms'

// Globally-unique names for ACR and Key Vault.
var acrName = 'acr${uniqueString(resourceGroup().id)}'
var kvName = 'kv${uniqueString(resourceGroup().id)}'

// ── Observability ─────────────────────────────────────────────────────────────
resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: '${environmentName}-logs'
  location: location
  properties: {
    sku: { name: 'PerGB2018' }
    retentionInDays: 30
  }
}

// ── Container registry (admin user OFF: managed identity pulls instead) ──────
resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: acrName
  location: location
  sku: { name: 'Basic' }
  properties: {
    adminUserEnabled: false
  }
}

// ── Secrets ───────────────────────────────────────────────────────────────────
resource kv 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: kvName
  location: location
  properties: {
    sku: { family: 'A', name: 'standard' }
    tenantId: subscription().tenantId
    enableRbacAuthorization: true
  }
}

resource dbPasswordSecret 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: kv
  name: 'db-password'
  properties: { value: dbAdminPassword }
}

resource jwtSecretEntry 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: kv
  name: 'jwt-secret'
  properties: { value: jwtSecret }
}

// ── Database ──────────────────────────────────────────────────────────────────
resource postgres 'Microsoft.DBforPostgreSQL/flexibleServers@2022-12-01' = {
  name: '${environmentName}-pg'
  location: location
  sku: { name: postgresSku, tier: 'Burstable' }
  properties: {
    version: '16'
    administratorLogin: dbAdminLogin
    administratorLoginPassword: dbAdminPassword
    storage: { storageSizeGB: 32 }
    backup: { backupRetentionDays: 7, geoRedundantBackup: 'Disabled' }
    // Public access is the default when no VNet is delegated.
    highAvailability: { mode: 'Disabled' }
  }
}

// Lets Azure services (incl. Container Apps) reach the database.
// For production, replace with private networking / tighter firewall rules.
resource allowAzureServices 'Microsoft.DBforPostgreSQL/flexibleServers/firewallRules@2022-12-01' = {
  parent: postgres
  name: 'AllowAzureServices'
  properties: { startIpAddress: '0.0.0.0', endIpAddress: '0.0.0.0' }
}

resource database 'Microsoft.DBforPostgreSQL/flexibleServers/databases@2022-12-01' = {
  parent: postgres
  name: 'hrvdb'
  properties: { charset: 'UTF8', collation: 'en_US.utf8' }
  dependsOn: [ allowAzureServices ]
}

// ── Container Apps ────────────────────────────────────────────────────────────
resource cae 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: '${environmentName}-cae'
  location: location
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalytics.properties.customerId
        sharedKey: logAnalytics.listKeys().primarySharedKey
      }
    }
  }
}

resource app 'Microsoft.App/containerApps@2024-03-01' = {
  name: '${environmentName}-api'
  location: location
  identity: { type: 'SystemAssigned' }
  properties: {
    managedEnvironmentId: cae.id
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: true
        targetPort: 8080
        transport: 'http'
      }
      registries: [
        {
          server: acr.properties.loginServer
          identity: 'system'
        }
      ]
      // Secrets are read from Key Vault at runtime via the managed identity
      // (Key Vault Secrets User role below) — they never appear in
      // deployment outputs or revision definitions.
      secrets: [
        {
          name: 'db-password'
          keyVaultUrl: '${kv.properties.vaultUri}secrets/db-password'
          identity: 'system'
        }
        {
          name: 'jwt-secret'
          keyVaultUrl: '${kv.properties.vaultUri}secrets/jwt-secret'
          identity: 'system'
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'api'
          image: containerImage
          resources: { cpu: 1, memory: '2Gi' }
          env: [
            {
              name: 'DATABASE_URL'
              value: 'jdbc:postgresql://${postgres.properties.fullyQualifiedDomainName}:5432/hrvdb?sslmode=require'
            }
            {
              name: 'DATABASE_USERNAME'
              value: dbAdminLogin
            }
            {
              name: 'DATABASE_PASSWORD'
              secretRef: 'db-password'
            }
            {
              name: 'JWT_SECRET'
              secretRef: 'jwt-secret'
            }
            {
              name: 'PORT'
              value: '8080'
            }
          ]
          probes: [
            {
              type: 'Liveness'
              httpGet: { path: '/actuator/health', port: 8080 }
              initialDelaySeconds: 30
              periodSeconds: 30
            }
          ]
        }
      ]
      scale: { minReplicas: 0, maxReplicas: 2 }
    }
  }
  dependsOn: [ database, dbPasswordSecret, jwtSecretEntry ]
}

// Managed identity -> AcrPull: the app pulls images without any stored password.
resource acrPull 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(acr.id, app.id, 'acrpull')
  scope: acr
  properties: {
    roleDefinitionId: subscriptionResourceId(
      'Microsoft.Authorization/roleDefinitions',
      '7f951dda-4ed3-4680-a7ca-43fe172d538d'
    )
    principalId: app.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

// Managed identity -> Key Vault Secrets User: the app reads its secrets
// from Key Vault at runtime without any stored credential.
resource kvSecretsUser 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(kv.id, app.id, 'kvsecretsuser')
  scope: kv
  properties: {
    roleDefinitionId: subscriptionResourceId(
      'Microsoft.Authorization/roleDefinitions',
      '4633458b-17de-408a-b874-0445c86b69e6'
    )
    principalId: app.identity.principalId
    principalType: 'ServicePrincipal'
  }
}

output apiUrl string = 'https://${app.properties.configuration.ingress.fqdn}'
output acrLoginServer string = acr.properties.loginServer
output keyVaultName string = kv.name
