#!/usr/bin/env bash
# Back-compat wrapper. Prefer the split scripts:
#   1) ./scripts/setup-azure-entra.sh
#   2) ./scripts/setup-github-deploy-secrets.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
"$ROOT/scripts/setup-azure-entra.sh"
"$ROOT/scripts/setup-github-deploy-secrets.sh"
