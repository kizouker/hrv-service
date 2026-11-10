# Azure path for HRV backend (AZ-401 study guide)

Spring Boot 3.4 · Java 21 · PostgreSQL · JWT. Local run is Docker Compose;
Azure target is **Container Apps + ACR + PostgreSQL Flexible Server +
Key Vault + Log Analytics**, provisioned with Bicep and deployed via
GitHub Actions with OIDC.

## What was added and why

| File | Purpose | Exam topic |
|---|---|---|
| `Dockerfile` | Multi-stage build, cached deps, non-root user, healthcheck | Containers, secure images |
| `.dockerignore` | Keeps secrets and clutter out of the image | Container best practice |
| `docker-compose.yml` | Local app + Postgres, `.env` overrides | Dev inner loop |
| `.env.example` | Template for local overrides | Config vs secrets |
| `.github/workflows/ci.yml` | Build + test + coverage gate + Docker validation | CI pipelines |
| `.github/workflows/azure-deploy.yml` | OIDC login, Bicep deploy, ACR build, Container App update, smoke test | CD, IaC, OIDC |
| `infra/main.bicep` | All Azure resources, managed identity, Key Vault secrets | Bicep IaC |
| `infra/main.parameters.json` | Dev parameter defaults (secrets passed at deploy time) | Parameterization |

## 1. Local Docker (do this first)

```bash
docker compose up --build
curl -s http://localhost:8080/actuator/health
# {"status":"UP"}
# Swagger: http://localhost:8080/swagger-ui.html
```

Concepts to note: multi-stage image (build vs runtime), layer caching
(`pom.xml` before `src/`), non-root `USER`, `HEALTHCHECK` against
`/actuator/health` (permitAll in `SecurityConfig`).

## 2. Deploy to Azure manually (learn each step)

Run these in your own terminal (`az login` first). ~10 min + Postgres
provisioning time.

```bash
RG=hrv-dev-rg; LOC=westeurope  # or swedencentral
az group create -n $RG -l $LOC

DB_PASS=$(openssl rand -base64 24)
JWT=$(openssl rand -base64 32)

# Preview first (exam: what-if analysis)
az deployment group what-if -g $RG --template-file infra/main.bicep \
  --parameters environmentName=hrv-dev \
  --parameters dbAdminPassword="$DB_PASS" jwtSecret="$JWT"

# Deploy
DEPLOY=$(az deployment group create -g $RG --template-file infra/main.bicep \
  --parameters environmentName=hrv-dev \
  --parameters dbAdminPassword="$DB_PASS" jwtSecret="$JWT" \
  --query properties.outputs -o json)
ACR=$(echo "$DEPLOY" | python3 -c "import json,sys; print(json.load(sys.stdin)['acrLoginServer']['value'])")

# Build + push the image in Azure (no local Docker daemon needed)
az acr build -r "$ACR" -t hrv-backend:latest .

# Point the app at the real image (first deploy used a placeholder)
az containerapp update -n hrv-dev-api -g $RG -i "$ACR/hrv-backend:latest"

# Smoke test
FQDN=$(az containerapp show -n hrv-dev-api -g $RG \
  --query properties.configuration.ingress.fqdn -o tsv)
curl -s "https://$FQDN/actuator/health"
curl -s "https://$FQDN/swagger-ui.html" -o /dev/null -w "%{http_code}\n"
```

What to learn from each step: resource group scoping, Bicep `what-if`,
`az acr build` (cloud build), `az containerapp update` (new revision),
managed identity pulling from ACR with zero stored passwords
(`adminUserEnabled: false` + `AcrPull` role in `main.bicep`).

## 3. GitHub Actions with OIDC (core exam topic)

Deploy workflow uses GitHub environment **`development`** and image name
**`hrv-service`**. One-time setup on your machine:

```bash
az login
gh auth login
./scripts/setup-azure-oidc.sh
gh workflow enable "Deploy to Azure"
gh workflow run "Deploy to Azure"
```

The script creates the Entra app, federated credential
(`repo:kizouker/hrv-service:environment:development`), RG role
assignments, and GitHub env secrets/vars. Approve the deployment if
required reviewers are set on `development`.

## 4. Monitoring to look at

- Revisions: `az containerapp revision list -n hrv-dev-api -g $RG`
- Logs: `az containerapp logs show -n hrv-dev-api -g $RG --follow`
- Log Analytics workspace `<env>-logs` in the portal: query
  `ContainerAppConsoleLogs_CL | order by TimeGenerated desc`
- Health probe: `/actuator/health` drives the liveness probe and the
  smoke-test step in the pipeline.

## 5. Costs and cleanup

Cheapest learning shape is already chosen: ACR Basic, Postgres B1ms
burstable, Container Apps scale-to-zero. Postgres is the main cost
(~€15–25/month if left running). After each study session:

```bash
az group delete -n $RG --yes --no-wait   # deletes everything
```

Re-deploy from scratch next session — repetition is the exam prep.

## Weekend checklist to Monday

- [ ] `docker compose up` works and you can explain every Dockerfile line
- [ ] Manual Bicep deploy works; you can explain each resource block
- [ ] `az acr build` + `az containerapp update` works; smoke test green
- [ ] OIDC federated credential set up; pipeline deploys on push to main
- [ ] You can query logs and list revisions from the CLI
- [ ] Resource group deleted; re-deploy once more from memory
