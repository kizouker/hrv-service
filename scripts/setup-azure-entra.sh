#!/usr/bin/env bash
# Azure/Entra only: app registration, SP, roles, federated credential.
# Writes/updates values into .env (never prints JWT_SECRET).
# Run on your machine after: az login
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT/.env}"

REPO="${REPO:-kizouker/hrv-service}"
ENV_NAME="${ENV_NAME:-development}"
APP_NAME="${APP_NAME:-gh-hrv-service}"
RG="${RG:-rg-hrv-dev}"
LOCATION="${LOCATION:-swedencentral}"

upsert_env() {
  local key="$1" value="$2" tmp
  touch "$ENV_FILE"
  tmp="$(mktemp)"
  if grep -q "^${key}=" "$ENV_FILE" 2>/dev/null; then
    # Replace existing key; keep the rest of the file.
    awk -v k="$key" -v v="$value" '
      BEGIN { done=0 }
      $0 ~ "^"k"=" { print k"="v; done=1; next }
      { print }
      END { if (!done) print k"="v }
    ' "$ENV_FILE" >"$tmp"
  else
    cat "$ENV_FILE" >"$tmp"
    printf '%s=%s\n' "$key" "$value" >>"$tmp"
  fi
  mv "$tmp" "$ENV_FILE"
}

echo "==> Using repo=$REPO environment=$ENV_NAME rg=$RG location=$LOCATION"
echo "==> Writing Azure values to $ENV_FILE"

echo "==> Ensure resource group"
az group create --name "$RG" --location "$LOCATION" --output none

echo "==> Create (or reuse) Entra app registration"
APP_ID=$(az ad app list --display-name "$APP_NAME" --query "[0].appId" -o tsv)
if [[ -z "$APP_ID" || "$APP_ID" == "null" ]]; then
  APP_ID=$(az ad app create --display-name "$APP_NAME" --query appId -o tsv)
fi
echo "AZURE_CLIENT_ID=$APP_ID"

echo "==> Ensure service principal"
az ad sp show --id "$APP_ID" >/dev/null 2>&1 || az ad sp create --id "$APP_ID" --output none

RG_ID=$(az group show --name "$RG" --query id -o tsv)

echo "==> Role assignments on $RG"
# Contributor: create/update resources (ACR, KV, Container Apps, ...).
az role assignment create --assignee "$APP_ID" --role Contributor --scope "$RG_ID" >/dev/null || true
# User Access Administrator: Bicep assigns AcrPull + Key Vault Secrets User
# to the app managed identity (roleAssignments/write). Contributor alone cannot.
az role assignment create --assignee "$APP_ID" --role "User Access Administrator" --scope "$RG_ID" >/dev/null || true
# Key Vault data-plane for jwt-secret writes during deploy if needed.
az role assignment create --assignee "$APP_ID" --role "Key Vault Secrets Officer" --scope "$RG_ID" >/dev/null || true
# AcrPush: CI builds the image; deploy pushes that image to ACR.
# az acr build is blocked on this subscription (TasksOperationsNotAllowed).
az role assignment create --assignee "$APP_ID" --role AcrPush --scope "$RG_ID" >/dev/null || true

# Prefer the classic GitHub OIDC subject Entra UI docs show.
# If the repo later enables immutable subjects, rebuild as:
#   repo:OWNER@OWNER_ID/REPO@REPO_ID:environment:ENV
OIDC_SUB=$(gh api "repos/${REPO}/actions/oidc/customization/sub" --jq '{immutable:.use_immutable_subject, prefix:.sub_claim_prefix}')
echo "==> GitHub OIDC customization: $OIDC_SUB"
if gh api "repos/${REPO}/actions/oidc/customization/sub" --jq -e '.use_immutable_subject == true' >/dev/null 2>&1; then
  OWNER_LOGIN=$(gh api "repos/${REPO}" --jq .owner.login)
  OWNER_ID=$(gh api "repos/${REPO}" --jq .owner.id)
  REPO_NAME=$(gh api "repos/${REPO}" --jq .name)
  REPO_ID=$(gh api "repos/${REPO}" --jq .id)
  SUBJECT="repo:${OWNER_LOGIN}@${OWNER_ID}/${REPO_NAME}@${REPO_ID}:environment:${ENV_NAME}"
else
  SUBJECT="repo:${REPO}:environment:${ENV_NAME}"
fi
echo "==> Federated credential subject: $SUBJECT"
az ad app federated-credential delete --id "$APP_ID" --federated-credential-id "gh-${ENV_NAME}" >/dev/null 2>&1 || true
az ad app federated-credential create --id "$APP_ID" --parameters "{
  \"name\": \"gh-${ENV_NAME}\",
  \"issuer\": \"https://token.actions.githubusercontent.com\",
  \"subject\": \"${SUBJECT}\",
  \"audiences\": [\"api://AzureADTokenExchange\"]
}" >/dev/null
upsert_env "AZURE_OIDC_SUBJECT" "$SUBJECT"

TENANT_ID=$(az account show --query tenantId -o tsv)
SUB_ID=$(az account show --query id -o tsv)
JWT_SECRET_VALUE=$(openssl rand -base64 48)

upsert_env "REPO" "$REPO"
upsert_env "ENV_NAME" "$ENV_NAME"
upsert_env "APP_NAME" "$APP_NAME"
upsert_env "AZURE_RG" "$RG"
upsert_env "AZURE_LOCATION" "$LOCATION"
upsert_env "AZURE_CLIENT_ID" "$APP_ID"
upsert_env "AZURE_TENANT_ID" "$TENANT_ID"
upsert_env "AZURE_SUBSCRIPTION_ID" "$SUB_ID"
upsert_env "JWT_SECRET" "$JWT_SECRET_VALUE"

echo
echo "Done (Azure/Entra)."
echo "Values saved in $ENV_FILE (gitignored)."
echo "Next: ./scripts/setup-github-deploy-secrets.sh"
