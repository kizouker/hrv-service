#!/usr/bin/env bash
# GitHub only: set Actions variables + environment secrets from .env
# Run on your machine after: ./scripts/setup-azure-entra.sh && gh auth login
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT/.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE — run ./scripts/setup-azure-entra.sh first." >&2
  exit 1
fi

# shellcheck disable=SC1090
set -a
source "$ENV_FILE"
set +a

: "${REPO:?REPO missing in .env}"
: "${ENV_NAME:?ENV_NAME missing in .env}"
: "${AZURE_RG:?AZURE_RG missing in .env}"
: "${AZURE_LOCATION:?AZURE_LOCATION missing in .env}"
: "${AZURE_CLIENT_ID:?AZURE_CLIENT_ID missing in .env}"
: "${AZURE_TENANT_ID:?AZURE_TENANT_ID missing in .env}"
: "${AZURE_SUBSCRIPTION_ID:?AZURE_SUBSCRIPTION_ID missing in .env}"
: "${JWT_SECRET:?JWT_SECRET missing in .env}"

echo "==> GitHub variables/secrets on $REPO (environment=$ENV_NAME)"

gh variable set AZURE_RG --repo "$REPO" --body "$AZURE_RG"
gh variable set AZURE_LOCATION --repo "$REPO" --body "$AZURE_LOCATION"

gh secret set AZURE_CLIENT_ID --repo "$REPO" --env "$ENV_NAME" --body "$AZURE_CLIENT_ID"
gh secret set AZURE_TENANT_ID --repo "$REPO" --env "$ENV_NAME" --body "$AZURE_TENANT_ID"
gh secret set AZURE_SUBSCRIPTION_ID --repo "$REPO" --env "$ENV_NAME" --body "$AZURE_SUBSCRIPTION_ID"
gh secret set JWT_SECRET --repo "$REPO" --env "$ENV_NAME" --body "$JWT_SECRET"

echo
echo "Done (GitHub)."
echo "1) Enable workflow if needed:  gh workflow enable \"Deploy to Azure\" --repo $REPO"
echo "2) Run deploy:                 gh workflow run \"Deploy to Azure\" --repo $REPO"
echo "3) Approve the $ENV_NAME environment if required reviewers are set."
