#!/usr/bin/env bash
# Lab 14. Creates the protect-main ruleset.
# The JSON keeps enforcement "disabled", so applying it does not block pushes.
# Set "enforcement" to "active" when you want the gate, then run this script.
set -euo pipefail
gh api --method POST repos/kizouker/hrv-service/rulesets \
  --input .github/rulesets/protect-main.json
echo "Ruleset protect-main created. It blocks nothing until enforcement is active."
