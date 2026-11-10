# Scripts

## Azure deploy setup (kör i denna ordning)

1. **`./scripts/setup-azure-entra.sh`** — kör först  
   Kräver: `az login`  
   Skapar/återanvänder Entra-app, service principal, roller, federated credential.  
   Skriver värden till gitignorerad `.env` i repo-roten.

2. **`./scripts/setup-github-deploy-secrets.sh`** — kör sedan  
   Kräver: `gh auth login` + att steg 1 är klart  
   Läser `.env` och sätter GitHub Actions variables + environment secrets.

3. Aktivera och kör deploy:
   ```bash
   gh workflow enable "Deploy to Azure"
   # Kör från branchen som har environment: development (PR #15), eller main efter merge:
   gh workflow run "Deploy to Azure" --ref cursor/azure-deploy-development-45b1
   ```
   Godkänn miljön `development` om required reviewers är på.

### Om Azure login failar med AADSTS700213
Detta repo har **immutable** OIDC (går inte att stänga av). Entra subject måste vara:
`repo:kizouker@19254086/hrv-service@1319982126:environment:development`

### Om `az acr build` failar med TasksOperationsNotAllowed
Prenumerationen tillåter inte ACR Tasks. Workflowet bygger imagen på runnern och pushar med `docker push`. Appen behöver rollen **AcrPush** på resursgruppen. `setup-azure-entra.sh` sätter den. Kör om skriptet, eller:

```bash
az role assignment create \
  --assignee "$AZURE_CLIENT_ID" \
  --role AcrPush \
  --scope "$(az group show -n rg-hrv-dev --query id -o tsv)"
```

### Om Bicep failar med roleAssignments/write
GitHub-appen behöver **User Access Administrator** på `rg-hrv-dev` (utöver Contributor),
annars kan den inte ge managed identity `AcrPull` / Key Vault-roller. Kör om steg 1
eller:
```bash
az role assignment create \
  --assignee "$AZURE_CLIENT_ID" \
  --role "User Access Administrator" \
  --scope "$(az group show -n rg-hrv-dev --query id -o tsv)"
```

### Wrapper (valfritt)
`./scripts/setup-azure-oidc.sh` kör steg 1 + 2 i följd.

## Övrigt
- `check-config.sh` — lokal config-koll (separat från Azure-setup).
