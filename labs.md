# AZ-401 – 15-minute labs
 URLs you provide. Learn more in our Webhooks Guide.

One lab = one thing. Everything builds on hrv-backend (github.com/kizouker/hrv-backend) and rg-hrv-dev.
★ = do these first if time is short (8 labs, about 2 hours). All 15 take about 4 hours.
 
Status: [ ] not done · [~] in progress · [x] done
 
## Before you start
 
Step 0. All terminal blocks below assume you are in the repo folder:
 
```bash
cd ~/kod/001_AZ401/HRV/hrv-backend
```
 
Do the Azure labs in **the same terminal window**, because some blocks save values (like `APP_ID`) that later blocks use.
 
Pushing a change (used in several labs). First the lab's own `git add` block, then:
 
```bash
git commit -m "Lab update"
```
 
```bash
git push
```
 
---
 
## Saturday – GitHub Actions and GitHub security
 
### [ ] Lab 1 ★ Manual trigger (workflow_dispatch)
Exam area: Pipelines. Goal: get the "Run workflow" button.
 
New file `.github/workflows/hello.yml`:
 
```yaml
name: hello
 
on:
  workflow_dispatch:
    inputs:
      name:
        description: Who to greet
        default: Rickard
 
jobs:
  say-hello:
    runs-on: ubuntu-latest
    steps:
      - run: echo "Hello ${{ inputs.name }}"
```
 
```bash
git add .github/workflows/hello.yml
```
 
Then commit and push (see top). Start it from the terminal:
 
```bash
gh workflow run hello.yml -f name=Azure
```
 
- `gh workflow run` = start a workflow by hand (this is what the button does)
- `hello.yml` = which workflow
- `-f name=Azure` = fill in the input called `name`
```bash
gh run list --workflow hello.yml --limit 1 PC
```
 
Done when: the run is green and its log says "Hello Azure".
 
### [ ] Lab 2 ★ Environment with approval
Exam area: Pipelines (go / no-go before deploy).
 
```bash
open https://github.com/kizouker/hrv-backend/settings/environments
```
 
New environment → `dev` → Required reviewers → pick yourself → Save.
(Free on public repos. Private repos need a paid plan for this.)
 
Replace `.github/workflows/hello.yml` with:
 
```yaml
name: hello
 
on:
  workflow_dispatch:
    inputs:
      name:
        description: Who to greet
        default: Rickard
 
jobs:
  say-hello:
    runs-on: ubuntu-latest
    steps:
      - run: echo "Hello ${{ inputs.name }}"
 
  deploy:
    needs: say-hello
    runs-on: ubuntu-latest
    environment: dev
    steps:
      - run: echo "Deploying to dev"
```
 
```bash
git add .github/workflows/hello.yml
```
 
Commit, push, run it like in Lab 1. Open the run in the Actions tab and click Review deployments → Approve.
 
Done when: the run stops at "Waiting for review", you approve, and `deploy` runs.
 
### [ ] Lab 3 ★ Secret scanning + push protection
Exam area: Security.
 
GitHub renamed the old "Code security and analysis" page. Use **Advanced Security**
(repo is `hrv-service`, not `hrv-backend`):
 
```bash
open https://github.com/kizouker/hrv-service/settings/advanced-security
```
 
Or: repo → **Settings** → sidebar **Security and quality** → **Advanced Security**.
 
Turn on **Secret Protection**, then **Push protection**. Then look at the alerts:
 
```bash
open https://github.com/kizouker/hrv-service/security/secret-scanning
```
 
Or: repo → **Security and quality** → **Secret scanning**.
 
Exam point: scanning finds **known token formats** (GitHub, Azure, AWS keys…). Your old random JWT string probably won't show up, because it has no known pattern. That's why you still rotated it by hand.
 
Done when: both settings are on and you know what the alerts page shows.
 
---
 
## Saturday evening – Azure security
 
### [ ] Lab 4 ★ OIDC login from GitHub Actions (no secret)
Exam area: Security. Uses the `dev` environment from Lab 2.
 
Check you're on the right account (should say Azure subscription 1):
 
```bash
az account show --query "{name:name, user:user.name}" -o table PC
```
 
Create an app identity for GitHub and save its id:
 
```bash
APP_ID=$(az ad app create --display-name gh-hrv-backend --query appId -o tsv)
```
 
- `az ad app create` = make an app registration (an identity for a program)
- `--query appId -o tsv` = keep only its id, as plain text
- `APP_ID=$(...)` = save it in a variable for the next blocks
```bash
az ad sp create --id "$APP_ID"
```
 
- makes the service principal, the part that can actually get permissions
```bash
az role assignment create --assignee "$APP_ID" --role Reader --scope "$(az group show --name rg-hrv-dev --query id -o tsv)"
```
 
- gives it read-only rights on rg-hrv-dev, nothing else (least privilege)
```bash
az ad app federated-credential create --id "$APP_ID" --parameters '{"name":"gh-dev","issuer":"https://token.actions.githubusercontent.com","subject":"repo:kizouker/hrv-backend:environment:dev","audiences":["api://AzureADTokenExchange"]}'
```
 
- the federated credential = "trust tokens from GitHub, but only from this repo's `dev` environment"
- `subject` is the key part: another repo or environment gets nothing
Save the ids in GitHub (they are not secrets, so variables are fine):
 
```bash
gh variable set AZURE_CLIENT_ID --body "$APP_ID"
```
 
```bash
gh variable set AZURE_TENANT_ID --body "$(az account show --query tenantId -o tsv)"
```
 
```bash
gh variable set AZURE_SUBSCRIPTION_ID --body "$(az account show --query id -o tsv)"
```
 
Replace `.github/workflows/hello.yml` with:
 
```yaml
name: hello
 
on:
  workflow_dispatch:
    inputs:
      name:
        description: Who to greet
        default: Rickard
 
permissions:
  id-token: write
  contents: read
 
jobs:
  say-hello:
    runs-on: ubuntu-latest
    steps:
      - run: echo "Hello ${{ inputs.name }}"
 
  deploy:
    needs: say-hello
    runs-on: ubuntu-latest
    environment: dev
    steps:
      - uses: azure/login@v2
        with:
          client-id: ${{ vars.AZURE_CLIENT_ID }}
          tenant-id: ${{ vars.AZURE_TENANT_ID }}
          subscription-id: ${{ vars.AZURE_SUBSCRIPTION_ID }}
      - run: az group show --name rg-hrv-dev --output table
```
 
`id-token: write` = lets the workflow ask GitHub for the short-lived token.
 
```bash
git add .github/workflows/hello.yml
```
 
Commit, push, run, approve.
 
Done when: the deploy job prints rg-hrv-dev, and there is no secret anywhere in the repo.
 
### [ ] Lab 5 Managed identity on the web app
Exam area: Security.
 
```bash
az webapp identity assign --name hrv-backend --resource-group rg-hrv-dev --query principalId -o tsv PC
```
 
- `identity assign` = give hrv-backend its own Azure identity (system-assigned)
- prints its id; Azure handles the password, you never see one
Done when: you get an id back, and the portal shows Identity → Status: On.
 
### [ ] Lab 6 ★ Key Vault reference for JWT_SECRET
Exam area: Security. Needs Lab 5. Can replace step 5's JWT_SECRET in status.md.
 
Is the vault using roles (RBAC)?
 
```bash
az keyvault show --name kv6iafhxcwzncrc --query properties.enableRbacAuthorization PC
```
 
If it says `true`, give yourself the right to write secrets:
 
```bash
az role assignment create --assignee "$(az ad signed-in-user show --query id -o tsv)" --role "Key Vault Secrets Officer" --scope "$(az keyvault show --name kv6iafhxcwzncrc --query id -o tsv)"
```
 
(If it says `false`, tell Claude — then it's the older access-policy way.)
 
Load your .env into this terminal, without printing anything:
 
```bash
source .env
```
 
Put the secret in the vault. Only the secret's id is printed, never the value:
 
```bash
az keyvault secret set --vault-name kv6iafhxcwzncrc --name jwt-secret --value "$JWT_SECRET" --query id -o tsv
```
 
Let the web app read secrets:
 
```bash
az role assignment create --assignee-object-id "$(az webapp identity show --name hrv-backend --resource-group rg-hrv-dev --query principalId -o tsv)" --assignee-principal-type ServicePrincipal --role "Key Vault Secrets User" --scope "$(az keyvault show --name kv6iafhxcwzncrc --query id -o tsv)"
```
 
- `Key Vault Secrets User` = can read secrets, not change them
Point the app setting at the vault:
 
```bash
az webapp config appsettings set --name hrv-backend --resource-group rg-hrv-dev --settings "JWT_SECRET=@Microsoft.KeyVault(VaultName=kv6iafhxcwzncrc;SecretName=jwt-secret)" --output none
```
 
- `@Microsoft.KeyVault(...)` = "fetch the value from the vault", the app setting only holds a pointer
- `--output none` = don't print the other settings (they include passwords)
Done when: Portal → hrv-backend → Environment variables → JWT_SECRET shows a green "Key vault Reference" check.
 
---
 
## Sunday morning – Infrastructure as code and monitoring
 
### [ ] Lab 7 ★ Bicep what-if
Exam area: Pipelines / IaC.
 
New file `infra/lab-storage.bicep`:
 
```bicep
param location string = resourceGroup().location
 
resource sa 'Microsoft.Storage/storageAccounts@2023-05-01' = {
  name: 'sthrvlab${uniqueString(resourceGroup().id)}'
  location: location
  sku: {
    name: 'Standard_LRS'
  }
  kind: 'StorageV2'
}
```
 
Dry run:
 
```bash
az deployment group what-if --resource-group rg-hrv-dev --template-file infra/lab-storage.bicep PC
```
 
Real run:
 
```bash
az deployment group create --resource-group rg-hrv-dev --template-file infra/lab-storage.bicep --output none
```
 
Dry run again — now it should say no change (same file = same result, "idempotent"):
 
```bash
az deployment group what-if --resource-group rg-hrv-dev --template-file infra/lab-storage.bicep PC
```
 
Clean up:
 
```bash
az storage account delete --resource-group rg-hrv-dev --name "$(az storage account list --resource-group rg-hrv-dev --query "[?starts_with(name,'sthrvlab')].name" -o tsv)" --yes
```
 
Exam point: default mode is **incremental** (only adds/changes). **Complete** mode deletes everything in the group that isn't in the file — never run that on rg-hrv-dev.
 
Done when: first what-if shows `+ Create`, second shows no change.
 
### [ ] Lab 8 ★ KQL in Log Analytics
Exam area: Instrumentation.
 
Start the app (it's stopped):
 
```bash
az webapp start --name hrv-backend --resource-group rg-hrv-dev
```
 
Send the web app's logs to hrv-dev-logs:
 
```bash
az monitor diagnostic-settings create --name to-logs --resource "$(az webapp show --name hrv-backend --resource-group rg-hrv-dev --query id -o tsv)" --workspace hrv-dev-logs --logs '[{"category":"AppServiceHTTPLogs","enabled":true},{"category":"AppServiceConsoleLogs","enabled":true}]'
```
 
- diagnostic setting = "copy this resource's logs into that workspace"
- if the Free (F1) plan refuses, note it — that's a real-world limit, skip to the query part in the portal's Log stream instead
Make some traffic (run it 3–4 times):
 
```bash
curl -s https://hrv-backend-cfb7b6drh9hqerh0.swedencentral-01.azurewebsites.net/actuator/health PC
```
 
Wait 5–10 minutes. Portal → hrv-dev-logs → Logs, paste this query (KQL, not a terminal command):
 
```kql
AppServiceHTTPLogs
| where TimeGenerated > ago(1h)
| summarize count() by ScStatus
```
 
Done when: you see a small table of status codes (200, 404…).
 
### [ ] Lab 9 Alert + action group
Exam area: Instrumentation. Needs Lab 8.
 
In hrv-dev-logs → Logs, run (KQL):
 
```kql
AppServiceHTTPLogs
| where TimeGenerated > ago(10m)
```
 
Click New alert rule → condition: number of results > 0, check every 5 minutes → Action group: create `ag-rickard` with your email → Create.
Run the curl from Lab 8 again and wait for the email.
 
Then **delete the alert rule** (it costs a little each month).
 
Exam point: the alert = query + threshold + schedule; the action group = who gets told or what runs.
 
Done when: you got the email and deleted the rule.
 
---
 
## Sunday midday – Azure DevOps
 
### [ ] Lab 10 Azure Boards linked to GitHub
Exam area: Processes.
 
```bash
open https://dev.azure.com
```
 
Create a project `hrv` → Project settings → GitHub connections → connect hrv-backend.
Boards → new work item "Try boards link" (it gets a number, like 1).
 
```bash
git commit --allow-empty -m "Try boards link AB#1"
```
 
```bash
git push
```
 
- `--allow-empty` = a commit with no file changes, just for the link
- `AB#1` = links the commit to work item 1 (use your number)
Exam point: `Fixes AB#1` in a PR moves the item to Done when merged.
 
Done when: the work item shows the commit under Development.
 
### [ ] Lab 11 Azure Pipelines: stage → job → step
Exam area: Pipelines.
 
New file `azure-pipelines.yml` (repo root):
 
```yaml
trigger: none
 
pool:
  vmImage: ubuntu-latest
 
stages:
  - stage: Build
    jobs:
      - job: Test
        steps:
          - script: echo "mvn verify would run here"
 
  - stage: Deploy
    dependsOn: Build
    jobs:
      - deployment: DeployDev
        environment: dev
        strategy:
          runOnce:
            deploy:
              steps:
                - script: echo "Deploying to dev"
```
 
```bash
git add azure-pipelines.yml
```
 
Commit, push. In dev.azure.com: Pipelines → New pipeline → GitHub → hrv-backend → Existing YAML → Run.
Bonus: Pipelines → Environments → dev → Approvals and checks → add yourself (same idea as Lab 2).
 
If you get "No hosted parallelism has been purchased or granted": new organizations have to ask Microsoft for free minutes, which takes days. Do Lab 12 instead.
 
Done when: you can point at the two stages, the jobs inside them, and the steps inside those.
 
### [ ] Lab 12 Self-hosted agent on your Mac
Exam area: Pipelines. Only if Lab 11 got the parallelism error (still a good exam topic).
 
Organization settings → Agent pools → Default → New agent → macOS → download. Also make a Personal Access Token with scope Agent Pools (Read & manage).
 
```bash
mkdir -p ~/azagent
```
 
```bash
cd ~/azagent
```
 
```bash
tar zxvf ~/Downloads/vsts-agent-osx-arm64-*.tar.gz
```
 
```bash
./config.sh
```
 
It asks for your org address (https://dev.azure.com/ and your org name), the token, and pool `Default`.
 
```bash
./run.sh
```
 
In `azure-pipelines.yml`, change the pool to:
 
```yaml
pool:
  name: Default
```
 
Run the pipeline again. Stop the agent afterwards with Ctrl+C.
 
Exam point: self-hosted = your machine, you keep it updated; Microsoft-hosted = a fresh VM every run.
 
---
 
## Sunday afternoon – Agentic DevOps (new in AZ-401)
 
### [ ] Lab 13 ★ MCP server in Copilot
Exam area: the new agent part.
 
In VS Code: Cmd+Shift+P → "MCP: Add Server" → HTTP → `https://api.githubcopilot.com/mcp/` → name it `github` → sign in.
Open Copilot Chat in **Agent** mode and ask: "List the open issues and the latest workflow runs in kizouker/hrv-backend."
Watch it ask permission before each tool call.
 
Bonus: try your own swedish-jobs-mcp the same way.
 
Exam point: you choose which servers an agent may use, and tool calls can need your approval.
 
Done when: Copilot answers using the GitHub tools, not a guess.
 
### [ ] Lab 14 Ruleset on main (the gate)
Exam area: Source control. Do it late, because after this you can't push straight to main.
 
```bash
open https://github.com/kizouker/hrv-backend/settings/rules
```
 
New branch ruleset → name `protect-main` → target: default branch → Require a pull request → Require status checks (pick the job from ci.yml) → Block force pushes → Active.
 
Test that a direct push is blocked:
 
```bash
git commit --allow-empty -m "Test ruleset"
```
 
```bash
git push PC
```
 
It should be rejected. Undo the test commit (keeps your files):
 
```bash
git reset HEAD~1
```
 
Exam point: branch protection/rulesets guard **main**; push protection guards against **secrets**.
 
### [ ] Lab 15 ★ Copilot coding agent behind the gate
Exam area: the new agent part + quality gates. Needs Lab 14.
 
```bash
gh issue create --title "Add a unit test for the health endpoint" --body "Add one JUnit test for /actuator/health. Run mvn verify."
```
 
Open the issue on GitHub → Assignees → Copilot. Watch it open a pull request.
CI runs on its PR (you may have to click "Approve and run workflows"). The ruleset stops the merge until CI is green and you have reviewed it.
 
Exam point: the agent works on its own branch; humans and CI are the quality gate.
 
Done when: you've reviewed and merged (or closed) Copilot's PR.
 
---
 
## Clean up after Monday
- [ ] Delete the alert rule and action group (Lab 9)
- [ ] Remove the GitHub app identity: `az ad app delete --id` with the APP_ID from Lab 4
- [ ] Stop hrv-backend again
- [ ] Remove the self-hosted agent (`./config.sh remove` in ~/azagent)