# AZ-401 – status och to-do
Prov: måndag 9 november, Lexicon Malmö. Uppdaterad 2026-11-06 kl 22.
 
Status: [ ] inte gjort · [~] pågår · [x] klart
 
## Läget just nu (6 nov, kl 22)
- Konto: rickard.aaberg@outlook.com, gratiskontot "Azure subscription 1" ($200). Umeå-kontot för studenter används inte här.
- Allt ligger i rg-hrv-dev (swedencentral). West US 3 är borttaget (quota-fel; portalen föreslår West US 3 som standard – kolla alltid Region).
- Webbapp hrv-backend, plan asp-hrv-dev (F1 Free), **stoppad** (`az webapp start --name hrv-backend --resource-group rg-hrv-dev`):
  https://hrv-backend-cfb7b6drh9hqerh0.swedencentral-01.azurewebsites.net
- Övrigt i rg-hrv-dev: hrv-dev-logs (Log Analytics), kv6iafhxcwzncrc (Key Vault), acr6iafhxcwzncrc (Container Registry, ~5 USD/mån).
- Sitecontainers-läge: image och port sätts under Distributionscenter → Containrar → main. Portalens guide skrev över imagen med staticsite:latest – kolla alltid efteråt.
- Beslut: kör **H2** i Azure tills vidare (Postgres efter provet).
## Nästa steg (där vi slutade)
1. [ ] Ändra rad 10 i application.yml till `url: ${DATABASE_URL}` och lägg hela adressen (inkl. /databasnamn) i DATABASE_URL i .env
2. [ ] `./scripts/check-config.sh` – ska vara grönt
3. [ ] Kolla att `.env` finns i `.dockerignore`
4. [ ] Bygg + pusha `rickardzouk/hrv-backend:1.2` (buildx --platform linux/amd64 --push)
5. [ ] Lägg in miljövariabler i Azure (H2): DATABASE_URL=jdbc:h2:mem:hrv, DATABASE_DRIVER=org.h2.Driver, JPA_DIALECT=org.hibernate.dialect.H2Dialect, POSTGRES_USER=sa, POSTGRES_PASSWORD (valfritt värde), APP_PORT=8080, JWT_SECRET (ny, från .env – aldrig i chatt), JWT_EXPIRATION_MS=86400000
6. [ ] Byt image på main till 1.2, port 8080, starta appen
7. [ ] curl /actuator/health → {"status":"UP"}
8. [ ] Committa application.yml, pom.xml (H2 runtime) och scripts/check-config.sh
Klart ikväll: H2 ändrad till `<scope>runtime</scope>` i pom.xml; check-config.sh skapat och grönt; nytt JWT_SECRET och DB-lösenord i .env (gamla låg på GitHub – räknas som läckta).
Tester: kör med Java 21 lokalt – `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn verify`. Java 25 ger Mockito-fel i AuthServiceTest och JaCoCo-varningar (major version 69).
 
## Begränsningar för agenter
- [~] Ruleset `freeze-for-judging` på adaptive-IVR-agent (stoppar alla pushar)
- [ ] GitHub-appar når bara holdmate (github.com/settings/installations)
- [ ] Mjuk gräns: rad i holdmates AGENTS.md
- [ ] Efter 9 nov: ta bort rulesetet
## hrv-backend i Azure (fokus nu, Holdmate vilar)
Repo: github.com/kizouker/hrv-backend · lokalt: ~/kod/001_AZ401/HRV/hrv-backend
- [x] Azure-kontot kollat
- [x] CI finns redan (.github/workflows/ci.yml: mvn verify, JaCoCo, Docker-build)
- [x] Städa bort West US 3-resurser
- [x] Bygga amd64-image och pusha till Docker Hub
- [x] Webbapp i Sweden Central med egen container
- [~] Databas: H2 i Azure (se Nästa steg)
- [ ] /actuator/health svarar {"status":"UP"}
- [ ] Secret scanning + push protection på repot
- [ ] JWT_SECRET i Key Vault i stället för app-inställning
- [ ] check-config.sh som steg i CI
- [ ] Minimal workflow `hello.yml` (workflow_dispatch) för att förstå Actions
- [ ] Minimal Azure: Bicep med en resurs + workflow som loggar in och kör den
- [ ] Gå igenom infra/main.bicep två rader i taget (den skapar Container Apps, standardimage är en hello-world-platshållare)
- [ ] Kör what-if mot rg-hrv-dev, sedan deploy
- [ ] hrvctl: TUI-kontrollskript med watchdog (prompt finns i chatten)
- [ ] GitHub Action som tar bort PostgreSQL-databasen (hrv-dev-pg) automatiskt – schemalagd + knapp, OIDC-inloggning, aldrig RG eller Key Vault
- [ ] Efter provet: Postgres i Sweden Central
- [ ] Senare: byt till Java 25 (pom.xml, ci.yml, Dockerfile, Bicep, nyare Mockito + JaCoCo)
## react-flashcards
- [ ] Ta reda på om det är webb (React) eller mobil (React Native)
- [ ] CI-pipeline (npm ci + build) – får vara röd först
## Holdmate i Azure (vilar tills vidare)
- [ ] Steg 0: kolla Azure-kontot (portal + `az account show`)
- [ ] Pipelines: Bicep-deploy av board-appen (bygg image med `--platform linux/amd64`)
- [ ] Security: OIDC från GitHub Actions + `MODEL_API_KEY` i Key Vault
- [ ] Pipelines: GitHub Actions-deploy med miljön dev och godkännande
- [ ] Instrumentation: Log Analytics, App Insights, KQL-fråga, alert, dashboard
- [ ] Source control: branch protection på holdmates main (regler för Copilot-agenten)
- [ ] Processes: Azure Boards kopplat till holdmate
- [ ] Senare: byt gamla namnet i README, CI-länkar, Pages och image-namnet
## Läsning och quiz
- [ ] AZ-400-vägen modul 1–5 (Azure Pipelines – nytt)
- [ ] AZ-400-vägen modul 6–8 (skumma)
- [ ] Det nya i AZ-401: MCP-servrar, GitHub Agentic Workflows, Copilot quality gates
- [ ] Söndag: repetition och quiz
 