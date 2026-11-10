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
   gh workflow run "Deploy to Azure"
   ```
   Godkänn miljön `development` om required reviewers är på.

### Wrapper (valfritt)
`./scripts/setup-azure-oidc.sh` kör steg 1 + 2 i följd.

## Övrigt
- `check-config.sh` — lokal config-koll (separat från Azure-setup).
