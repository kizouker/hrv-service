#!/usr/bin/env bash
# Lab 6, after Lab 5. Points hrv-backend's JWT_SECRET at Key Vault.
# Prints the secret id, never the value. Does not run unless you start it.
set -euo pipefail

APP=hrv-backend
RG=rg-hrv-dev
VAULT=kv6iafhxcwzncrc

if [[ -z "${JWT_SECRET:-}" && -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi
if [[ -z "${JWT_SECRET:-}" ]]; then
  echo "JWT_SECRET is empty. Export it or put it in .env. Nothing was sent to Azure." >&2
  exit 1
fi

echo "Lab 5: system-assigned identity on ${APP}"
az webapp identity assign --name "$APP" --resource-group "$RG" --query principalId -o tsv

echo "Who can write secrets in ${VAULT}"
az role assignment create \
  --assignee "$(az ad signed-in-user show --query id -o tsv)" \
  --role "Key Vault Secrets Officer" \
  --scope "$(az keyvault show --name "$VAULT" --query id -o tsv)" \
  >/dev/null || true

echo "Store jwt-secret (id only)"
az keyvault secret set \
  --vault-name "$VAULT" \
  --name jwt-secret \
  --value "$JWT_SECRET" \
  --query id -o tsv

PRINCIPAL=$(az webapp identity show --name "$APP" --resource-group "$RG" --query principalId -o tsv)
az role assignment create \
  --assignee-object-id "$PRINCIPAL" \
  --assignee-principal-type ServicePrincipal \
  --role "Key Vault Secrets User" \
  --scope "$(az keyvault show --name "$VAULT" --query id -o tsv)" \
  >/dev/null || true

az webapp config appsettings set \
  --name "$APP" \
  --resource-group "$RG" \
  --settings "JWT_SECRET=@Microsoft.KeyVault(VaultName=${VAULT};SecretName=jwt-secret)" \
  --output none

echo "JWT_SECRET on ${APP} now points at ${VAULT}/jwt-secret."
