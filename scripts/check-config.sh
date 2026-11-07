#!/usr/bin/env bash
# check-config.sh – kollar att .env, application.yml och docker compose hänger ihop.
# Skriver bara ut NAMN, aldrig värden (inga lösenord syns).
# Kör från projektets rotmapp:  ./scripts/check-config.sh

YML="src/main/resources/application.yml"
ENV_FILE=".env"
errors=0

ok()   { echo "✅ $1"; }
fail() { echo "❌ $1"; errors=$((errors + 1)); }
info() { echo "   $1"; }

tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

echo "== 1. Filerna finns =="
[ -f "$YML" ]      && ok "$YML finns"      || { fail "$YML saknas"; exit 1; }
[ -f "$ENV_FILE" ] && ok "$ENV_FILE finns" || { fail "$ENV_FILE saknas"; exit 1; }

echo
echo "== 2. Trasiga platshållare i application.yml =="
# Hittar {NAMN} som saknar $ framför, t.ex. /{POSTGRES_DB}
broken=$(grep -nE '(^|[^$]){[A-Za-z_][A-Za-z0-9_]*}' "$YML")
# Hittar ${NAMN utan avslutande }
unclosed=$(grep -nE '\$\{[A-Za-z_][A-Za-z0-9_]*([^A-Za-z0-9_:}]|$)' "$YML")
if [ -z "$broken" ] && [ -z "$unclosed" ]; then
  ok "Inga trasiga \${...}"
else
  [ -n "$broken" ]   && fail "Saknar \$ framför {...}:"     && echo "$broken"   | sed 's/^/   rad /'
  [ -n "$unclosed" ] && fail "Saknar } efter \${...:"       && echo "$unclosed" | sed 's/^/   rad /'
fi

echo
echo "== 3. Variabler: application.yml jämfört med .env =="
# Krävs = ${NAMN}   (inget standardvärde)
grep -oE '\$\{[A-Za-z_][A-Za-z0-9_]*\}' "$YML" | sed 's/[${}]//g' | sort -u > "$tmp/required"
# Valfria = ${NAMN:standard}
grep -oE '\$\{[A-Za-z_][A-Za-z0-9_]*:' "$YML" | sed 's/[${:]//g' | sort -u > "$tmp/optional"
# Namnen i .env (hoppar över kommentarer och tomma rader)
grep -vE '^\s*(#|$)' "$ENV_FILE" | sed 's/^export //' | cut -d= -f1 | tr -d ' ' | sort -u > "$tmp/env"

missing=$(comm -23 "$tmp/required" "$tmp/env")
if [ -z "$missing" ]; then
  ok "Alla obligatoriska variabler finns i .env ($(wc -l < "$tmp/required" | tr -d ' ') st)"
else
  fail "Saknas i .env (appen kraschar utan dem):"
  echo "$missing" | sed 's/^/   - /'
fi

opt_missing=$(comm -23 "$tmp/optional" "$tmp/env")
[ -n "$opt_missing" ] && info "Valfria, använder standardvärdet i yml:" && echo "$opt_missing" | sed 's/^/     - /'

cat "$tmp/required" "$tmp/optional" | sort -u > "$tmp/used"
unused=$(comm -13 "$tmp/used" "$tmp/env")
[ -n "$unused" ] && info "Finns i .env men används inte i yml (kan vara till docker compose):" && echo "$unused" | sed 's/^/     - /'

echo
echo "== 4. Spring läser .env =="
if grep -q 'optional:file:.env\[.properties\]' "$YML"; then
  ok "spring.config.import med .env finns"
else
  info "Ingen import av .env i yml – går bra i Docker, men inte med mvn spring-boot:run"
fi

echo
echo "== 5. .env är skyddad från git =="
if git check-ignore -q "$ENV_FILE" 2>/dev/null; then
  ok ".env ignoreras av git"
else
  fail ".env ignoreras INTE – lägg till .env i .gitignore"
fi
if git ls-files --error-unmatch "$ENV_FILE" >/dev/null 2>&1; then
  fail ".env är redan committad! Kör: git rm --cached .env"
fi

echo
echo "== 6. docker compose =="
compose_file=""
for f in docker-compose.yml docker-compose.yaml compose.yml compose.yaml; do
  if [ -f "$f" ]; then compose_file="$f"; break; fi
done
if [ -n "$compose_file" ]; then
  if docker compose -f "$compose_file" config --quiet 2>"$tmp/compose_err"; then
    ok "docker compose config är giltig ($compose_file)"
  else
    fail "docker compose config gav fel:"
    sed 's/^/   /' "$tmp/compose_err"
  fi
else
  info "Ingen compose-fil hittad – hoppar över"
fi

echo
if [ "$errors" -eq 0 ]; then
  echo "🎉 Allt ser bra ut."
else
  echo "⚠️  $errors problem hittade."
fi
exit "$errors"
