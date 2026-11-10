// HRV backend infrastructure (resource-group scope).
// Provisions: Log Analytics, ACR, Key Vault and a Container App with SQLite.
// SQLite is an ephemeral, single-replica learning database stored in the container.
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

@description('JWT signing secret (Base64 256-bit). Pass via CLI or pipeline secret.')
@secure()
param jwtSecret string

@description('Container image for the API. Placeholder until the real image is pushed to ACR.')
param containerImage string = 'mcr.microsoft.com/azuredocs/containerapps-helloworld:latest'

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

resource jwtSecretEntry 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: kv
  name: 'jwt-secret'
  properties: { value: jwtSecret }
}

// Created before the app so Key Vault permission exists before its first revision.
resource appIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: '${environmentName}-identity'
  location: location
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
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${appIdentity.id}': {}
    }
  }
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
          identity: appIdentity.id
        }
      ]
      // The JWT secret is read from Key Vault at runtime via the managed identity.
      secrets: [
        {
          name: 'jwt-secret'
          keyVaultUrl: '${kv.properties.vaultUri}secrets/jwt-secret'
          identity: appIdentity.id
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
            // Full JDBC URL (application.yml uses ${DATABASE_URL} as-is).
            {
              name: 'DATABASE_URL'
              value: 'jdbc:sqlite:/tmp/hrv.sqlite'
            }
            {
              name: 'DATABASE_DRIVER'
              value: 'org.sqlite.JDBC'
            }
            {
              name: 'JPA_DIALECT'
              value: 'org.hibernate.community.dialect.SQLiteDialect'
            }
            {
              name: 'POSTGRES_USER'
              value: 'sa'
            }
            {
              name: 'POSTGRES_PASSWORD'
              value: 'sa'
            }
            {
              name: 'APP_PORT'
              value: '8080'
            }
            {
              name: 'JWT_EXPIRATION_MS'
              value: '86400000'
            }
            {
              name: 'JWT_SECRET'
              secretRef: 'jwt-secret'
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
      // A SQLite file is local to one replica and disappears on restart/scale-down.
      scale: { minReplicas: 0, maxReplicas: 1 }
    }
  }
  // Role assignments must exist before the app resolves Key Vault secrets / ACR pull.
  dependsOn: [ jwtSecretEntry, kvSecretsUser, acrPull ]
}

// Managed identity -> AcrPull: the app pulls images without any stored password.
resource acrPull 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(acr.id, appIdentity.id, 'acrpull')
  scope: acr
  properties: {
    roleDefinitionId: subscriptionResourceId(
      'Microsoft.Authorization/roleDefinitions',
      '7f951dda-4ed3-4680-a7ca-43fe172d538d'
    )
    principalId: appIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

// Managed identity -> Key Vault Secrets User: the app reads its secrets
// from Key Vault at runtime without any stored credential.
resource kvSecretsUser 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(kv.id, appIdentity.id, 'kvsecretsuser')
  scope: kv
  properties: {
    roleDefinitionId: subscriptionResourceId(
      'Microsoft.Authorization/roleDefinitions',
      '4633458b-17de-408a-b874-0445c86b69e6'
    )
    principalId: appIdentity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

output apiUrl string = 'https://${app.properties.configuration.ingress.fqdn}'
output acrLoginServer string = acr.properties.loginServer
output keyVaultName string = kv.name
