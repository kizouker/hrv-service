# AI-200 – Developing AI Cloud Solutions on Azure – 15-minute labs

Study guide: https://learn.microsoft.com/en-us/credentials/certifications/resources/study-guides/ai-200

One lab = one thing. The product is the HRV Backend API (Java 21, Spring Boot) in [github.com/kizouker/hrv-service](https://github.com/kizouker/hrv-service). Azure labs use resource group **rg-hrv-dev** in **swedencentral**. With `environmentName=hrv-dev`, `infra/main.bicep` names the workspace **hrv-dev-logs**, the Container App **hrv-dev-api**, the environment **hrv-dev-cae**, and the identity **hrv-dev-identity**. ACR and Key Vault names are generated (`acr…`, `kv…`); look them up.

★ = do these four first if time is short (about 1 hour). All eight take about 2 hours.

Status: [ ] not done · [~] in progress · [x] done

## Before you start

Commands below assume the repo root:

```bash
cd "$(git rev-parse --show-toplevel)"
```

Read-only `az` commands only print what is already there. A step that spends money or changes a live setting is marked **Live setting**. Leave those for when you choose to run them.

`docs/AZURE.md` still shows a sample group `hrv-dev-rg` and `az acr build`. These labs use **rg-hrv-dev**. ACR Tasks are blocked on this subscription (`TasksOperationsNotAllowed`), so the image path is `docker build` plus `docker push`.

---

## Containers

### [ ] Lab 1 ★ ACR, and why `az acr build` is the wrong command

Exam area: Develop containerized solutions on Azure (ACR).

`infra/main.bicep` creates a Basic registry with `adminUserEnabled: false`. The Container App pulls with the user-assigned identity and the AcrPull role. No registry password is stored.

```bash
az group show --name rg-hrv-dev --query "{name:name, location:location}" -o table
```

```bash
az acr list --resource-group rg-hrv-dev --query "[].{name:name, loginServer:loginServer, sku:sku.name, adminUserEnabled:adminUserEnabled}" -o table
```

Read `Dockerfile`. Stage 1 is `maven:3.9.9-eclipse-temurin-21` (build). Stage 2 is `eclipse-temurin:21-jre-alpine` (run, non-root user `app`). The image name used in `.github/workflows/azure-deploy.yml` on main is `hrv-backend`.

`az acr build` starts an ACR Task. This subscription returns **TasksOperationsNotAllowed**. Build locally instead:

```bash
docker build -t hrv-backend:lab .
```

**Live setting.** The next block logs into ACR and pushes a tag. That writes to the Basic registry (storage on a registry you already pay for). Do not run it until you choose to. It does not roll `hrv-dev-api`.

```bash
ACR=$(az acr list --resource-group rg-hrv-dev --query "[0].loginServer" -o tsv)
az acr login --name "${ACR%%.azurecr.io}"
docker tag hrv-backend:lab "$ACR/hrv-backend:lab"
docker push "$ACR/hrv-backend:lab"
```

Done when: the list shows one registry, admin user is false, and you can say why the build is `docker build` rather than `az acr build`.

### [ ] Lab 2 ★ Container App revisions

Exam area: Develop containerized solutions on Azure (Container Apps revisions).

`infra/main.bicep` sets `activeRevisionsMode: 'Single'` on `${environmentName}-api`. One revision takes traffic. A new image or a template change creates another revision.

```bash
az containerapp list --resource-group rg-hrv-dev --query "[].name" -o tsv
```

```bash
az containerapp show --name hrv-dev-api --resource-group rg-hrv-dev --query "{mode:properties.configuration.activeRevisionsMode, fqdn:properties.configuration.ingress.fqdn, image:properties.template.containers[0].image}" -o json
```

```bash
az containerapp revision list --name hrv-dev-api --resource-group rg-hrv-dev --query "[].{name:name, active:properties.active, created:properties.createdTime, replicas:properties.replicas}" -o table
```

If the app name differs, use the name from the list. These three commands only read.

**Live setting.** `az containerapp update` creates a revision and changes what is running. Do not run it in this lab.

Done when: you can point at the active revision and say what Single mode means.

### [ ] Lab 3 KEDA on this app, AKS only as a contrast

Exam area: Develop containerized solutions on Azure (KEDA, and AKS as a contrast).

Container Apps scale with KEDA. This app does not add a scaler. The template only sets a replica range, because the database file is SQLite on local disk (`jdbc:sqlite:/tmp/hrv.sqlite` in `infra/main.bicep`). A second replica would have its own empty file.

```bicep
scale: { minReplicas: 0, maxReplicas: 1 }
```

```bash
az containerapp show --name hrv-dev-api --resource-group rg-hrv-dev --query "properties.template.scale" -o json
```

`bridge/base/app-deployment.yaml` is a local Kubernetes Deployment (`namespace: hrv-backend`, image `hrv-backend-app`). It is a contrast, not a cluster in rg-hrv-dev.

```bash
az aks list --resource-group rg-hrv-dev -o table
```

**Live setting.** `az aks create` bills a cluster. An HTTP scale rule on `hrv-dev-api` can start more than one replica and break the SQLite file. Do not run either change.

Done when: scale min is 0 and max is 1, the AKS list is empty, and you can say why this app stays on one replica.

---

## Data for AI

### [ ] Lab 4 pgvector on a note, without Azure PostgreSQL

Exam area: Develop AI solutions with Azure data management (PostgreSQL pgvector / RAG).

The Container App uses SQLite. `docker-compose.yml` uses `postgres:16-alpine` for local CRUD. Neither image is a vector store. RAG here means: store an embedding of `HrvReading.notes`, then read the nearest note back. There is no second product.

File you could add later as `labs/hrv_notes_pgvector.sql` (do not add it for this sitting):

```sql
-- Throwaway local database only. postgres:16-alpine has no pgvector.
CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE hrv_note_chunk (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  reading_id bigint NOT NULL,
  note text NOT NULL,
  embedding vector(3)
);
INSERT INTO hrv_note_chunk (reading_id, note, embedding)
VALUES (1, 'after morning meditation', '[0.1, 0.2, 0.3]');
SELECT note
FROM hrv_note_chunk
ORDER BY embedding <=> '[0.1, 0.2, 0.4]'
LIMIT 1;
```

The three-number vector stands in for a model call so this sitting needs no Azure OpenAI resource.

**Live setting.** Provisioning Azure Database for PostgreSQL in rg-hrv-dev bills a server. Do not run that. Do not point this script at the compose volume `hrv-pgdata`.

Done when: you can say what the `<=>` line returns, and you did not create an Azure database.

### [ ] Lab 5 Cosmos DB vector policy, as a file you do not deploy

Exam area: Develop AI solutions with Azure data management (Cosmos DB vectors).

Same note, different store. A Cosmos container needs a vector embedding policy and a vector index on the same path. This fragment is the policy only. It is not a full template, so it cannot be deployed by accident. Save it later as `labs/hrv-notes-vector.bicep` if you want the text on disk.

```bicep
// Fragment. Not a complete template. Do not deploy.
vectorEmbeddingPolicy: {
  vectorEmbeddings: [
    {
      path: '/embedding'
      dataType: 'float32'
      dimensions: 3
      distanceFunction: 'cosine'
    }
  ]
}
indexingPolicy: {
  indexingMode: 'consistent'
  automatic: true
  includedPaths: [
    { path: '/*' }
  ]
  excludedPaths: [
    { path: '/embedding/*' }
  ]
  vectorIndexes: [
    {
      path: '/embedding'
      type: 'quantizedFlat'
    }
  ]
}
```

A document you would store later:

```json
{
  "id": "reading-42",
  "readingId": 42,
  "note": "after morning meditation",
  "embedding": [0.1, 0.2, 0.3]
}
```

**Live setting.** A Cosmos account in rg-hrv-dev bills throughput and storage. Do not run `az cosmos create` or `az deployment group create` for this fragment.

Done when: you can name the path that both the embedding policy and the vector index use (`/embedding`).

---

## Connect a reading to another service

### [ ] Lab 6 One reading, three triggers, nothing created

Exam area: Connect to and consume Azure services (Service Bus, Event Grid, Functions triggers).

`HrvController` already accepts `POST /api/hrv`. This lab is the message that fact would become. It does not add a queue, a topic, or a Function App.

Service Bus queue message:

```json
{
  "type": "hrv.reading.created",
  "readingId": 42,
  "rmssd": 45.2,
  "notes": "after morning meditation"
}
```

Event Grid uses a CloudEvent for the same fact. `source` stays on this API:

```json
{
  "specversion": "1.0",
  "type": "hrv.reading.created",
  "source": "/api/hrv",
  "id": "lab-6",
  "data": {
    "readingId": 42,
    "rmssd": 45.2
  }
}
```

A Functions trigger would consume that queue. Study-guide bullet: Functions triggers. Shape only, not a project:

```java
// labs/HrvReadingLogger.java — do not add this file and do not deploy it.
// Service Bus trigger, queue hrv-readings.
// public void log(String message) { /* log readingId only */ }
```

**Live setting.** A Service Bus namespace, an Event Grid topic, or a Function App is a paid resource in the subscription. Do not create them in this lab.

Done when: you can say which snippet is the queue message, which is the event, and which is the trigger, and rg-hrv-dev has no new resource from you.

---

## Secrets, config, and logs

### [ ] Lab 7 ★ Key Vault names, and where App Configuration would sit

Exam area: Secure, monitor, troubleshoot (Key Vault, App Configuration).

`infra/main.bicep` turns on RBAC (`enableRbacAuthorization: true`), stores a secret named `jwt-secret`, and points the Container App secret `jwt-secret` at the vault URI. The identity has Key Vault Secrets User. The container reads it as `JWT_SECRET`.

```bash
az keyvault list --resource-group rg-hrv-dev --query "[].{name:name, rbac:properties.enableRbacAuthorization}" -o table
```

```bash
KV=$(az keyvault list --resource-group rg-hrv-dev --query "[0].name" -o tsv)
az keyvault secret list --vault-name "$KV" --query "[].name" -o tsv
```

The second command prints names. `az keyvault secret show` prints the JWT. Do not run `secret show` or `secret set`.

App Configuration is the study-guide place for non-secret settings. This repo keeps those as Container App env vars (`DATABASE_URL`, `PORT` in `infra/main.bicep`). There is no App Configuration store.

**Live setting.** `az appconfig create` bills a store and adds a live resource. Do not run it.

Done when: RBAC is true, the secret list shows `jwt-secret` and not its value, and you can say why `PORT` is not a Key Vault secret.

### [ ] Lab 8 ★ KQL in hrv-dev-logs, and what OpenTelemetry would add

Exam area: Secure, monitor, troubleshoot (OpenTelemetry, KQL).

`infra/main.bicep` sends the Container Apps environment to the workspace `${environmentName}-logs` (hrv-dev-logs). `docs/AZURE.md` queries `ContainerAppConsoleLogs_CL`.

```bash
az monitor log-analytics workspace list --resource-group rg-hrv-dev --query "[].name" -o tsv
```

Portal → **hrv-dev-logs** → Logs. Paste this KQL. If the table list uses another console-log name, use that table. Do not create a workspace.

```kql
ContainerAppConsoleLogs_CL
| where TimeGenerated > ago(1d)
| project TimeGenerated, Log_s
| take 20
```

The liveness probe in `infra/main.bicep` already calls `/actuator/health`. `pom.xml` includes `spring-boot-starter-actuator`. It does not include an OpenTelemetry exporter. Traces would leave the process only after an agent or an exporter exists. Do not add that dependency in this lab.

**Live setting.** An alert rule on this workspace can bill each month. Setting `OTEL_EXPORTER_OTLP_ENDPOINT` on `hrv-dev-api` makes a new revision. Do not add either one.

Done when: the query returns rows or an empty table from hrv-dev-logs, and you can say the health probe is not an OpenTelemetry trace.
