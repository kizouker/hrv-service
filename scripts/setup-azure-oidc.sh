#!/usr/bin/env bash
# One-time Entra + GitHub setup for Deploy to Azure (OIDC, no client secret).
# Run on your machine after: az login && gh auth login
set -euo pipefail

REPO="${REPO:-kizouker/hrv-service}"
ENV_NAME="${ENV_NAME:-development}"
APP_NAME="${APP_NAME:-gh-hrv-service}"
RG="${RG:-rg-hrv-dev}"
LOCATION="${LOCATION:-swedencentral}"

echo "==> Using repo=$REPO environment=$ENV_NAME rg=$RG location=$LOCATION"

echo "==> Ensure resource group"
az group create --name "$RG" --location "$LOCATION" --output none

echo "==> Create (or reuse) Entra app registration"
APP_ID=$(az ad app list --display-name "$APP_NAME" --query "[0].appId" -o tsv)
if [[ -z "$APP_ID" || "$APP_ID" == "null" ]]; then
  APP_ID=$(az ad app create --display-name "$APP_NAME" --query appId -o tsv)
fi
echo "APP_ID=$APP_ID"

echo "==> Ensure service principal"
az ad sp show --id "$APP_ID" >/dev/null 2>&1 || az ad sp create --id "$APP_ID" --output none

RG_ID=$(az group show --name "$RG" --query id -o tsv)

echo "==> Role assignments on $RG"
az role assignment create --assignee "$APP_ID" --role Contributor --scope "$RG_ID" >/dev/null || true
az role assignment create --assignee "$APP_ID" --role "Key Vault Secrets Officer" --scope "$RG_ID" >/dev/null || true

SUBJECT="repo:${REPO}:environment:${ENV_NAME}"
echo "==> Federated credential subject: $SUBJECT"
# Recreate named credential idempotently
az ad app federated-credential delete --id "$APP_ID" --federated-credential-id "gh-${ENV_NAME}" >/dev/null 2>&1 || true
az ad app federated-credential create --id "$APP_ID" --parameters "{
  \"name\": \"gh-${ENV_NAME}\",
  \"issuer\": \"https://token.actions.githubusercontent.com\",
  \"subject\": \"${SUBJECT}\",
  \"audiences\": [\"api://AzureADTokenExchange\"]
}" >/dev/null

TENANT_ID=$(az account show --query tenantId -o tsv)
SUB_ID=$(az account show --query id -o tsv)
JWT_SECRET_VALUE=$(openssl rand -base64 48)

echo "==> GitHub environment secrets/vars on $REPO ($ENV_NAME)"
gh variable set AZURE_RG --repo "$REPO" --body "$RG"
gh variable set AZURE_LOCATION --repo "$REPO" --body "$LOCATION"

gh secret set AZURE_CLIENT_ID --repo "$REPO" --env "$ENV_NAME" --body "$APP_ID"
gh secret set AZURE_TENANT_ID --repo "$REPO" --env "$ENV_NAME" --body "$TENANT_ID"
gh secret set AZURE_SUBSCRIPTION_ID --repo "$REPO" --env "$ENV_NAME" --body "$SUB_ID"
gh secret set JWT_SECRET --repo "$REPO" --env "$ENV_NAME" --body "$JWT_SECRET_VALUE"

echo
echo "Done."
echo "1) Enable workflow if needed:  gh workflow enable \"Deploy to Azure\" --repo $REPO"
echo "2) Run deploy:                 gh workflow run \"Deploy to Azure\" --repo $REPO"
echo "3) Approve the development environment if required reviewers are set."
echo "JWT_SECRET was generated and stored only in GitHub (not printed)."
