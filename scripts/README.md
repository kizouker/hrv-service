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
Entra-federated credential måste matcha GitHubs **immutable** subject, t.ex.:
`repo:kizouker@19254086/hrv-service@1319982126:environment:development`

Kör om steg 1 (scriptet sätter rätt subject), sedan steg 3 igen.

### Wrapper (valfritt)
`./scripts/setup-azure-oidc.sh` kör steg 1 + 2 i följd.

## Övrigt
- `check-config.sh` — lokal config-koll (separat från Azure-setup).
