# hrvctl — kontrollverktyg för HRV-backenden på Azure

Interaktiv TUI (Kotlin + Mordant) med Diesel som maskot i huvudet.
Sköter det tunga Azure-jobbet: what-if, deploy, image-build/push,
status med nedräkning, Postgres-borttagning, idle-timeout och watchdog.

## Förutsättningar

- Ucheckad repo på `~/kod/001_AZ401/HRV/hrv-backend`, branch `feature/hrvctl`
- Temurin 21 (finns redan på maskinen)
- Inloggad Azure CLI för allt utom menyn i sig

Logga in på Azure (krävs för Deploy, Status, Delete och watchdog):

```bash
az login
```

## Bygg

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home mvn -B -q -f ~/kod/001_AZ401/HRV/hrv-backend/hrvctl/pom.xml package
```

`JAVA_HOME=` framför sätter Java 21 bara för det här kommandot. Första bygget laddar ner beroenden och tar någon minut.

Kör testerna (timeout-logik och config, 12 tester):

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home mvn -B -f ~/kod/001_AZ401/HRV/hrv-backend/hrvctl/pom.xml verify
```

## Kör

Stå i reporoten, verktyget letar upp `infra/main.bicep` därifrån.

```bash
cd ~/kod/001_AZ401/HRV/hrv-backend
```

Starta menyn:

```bash
/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java -jar hrvctl/target/hrvctl.jar
```

Full sökväg till Java eftersom Macens vanliga `java` är trasig.

## Menyn

| Val | Gör |
|---|---|
| 1 | What-if — visar vad Bicep-deployen skulle ändra, ändrar inget |
| 2 | Deploy — skapar resursgruppen om den saknas, genererar DB-lösenord och JWT-hemlighet (bara i minnet, maskas i utskriften), kör `az deployment group create`, visar outputs |
| 3 | Byggar imagen för linux/amd64, loggar in på ACR, pushar, uppdaterar Container App |
| 4 | Status — resurser, Postgres-state och minuter kvar till auto-radering |
| 5 | Raderar PostgreSQL-servern nu (bara den — aldrig resursgruppen eller Key Vault) |
| 6 | Ändrar idle-timeout (standard 15 min, sparas i `~/.hrvctl/config`) |
| 7 | Installerar/avinstallerar watchdog-jobbet (launchd, var 60:e sekund) |
| 8 | Avslutar |

Varje menyval skriver ut sitt `az`/`docker`-kommando innan det körs, och varje utförd åtgärd uppdaterar `~/.hrvctl/last-activity`.

Deploy återanvänder `JWT_SECRET` från miljön om den är satt (t.ex. efter `source ~/.hrvctl/azure-secrets.env`), annars genereras en ny. `DB_ADMIN_PASSWORD` behövs inte längre — infran använder SQLite och inget lösenord.

## Watchdog

`hrvctl watchdog` är kommandot som launchd-jobbet kör varje minut.
Finns `hrv-dev-pg` och idle-timeouten har passerats raderas servern.
Nås inte Azure (t.ex. utloggad) händer ingenting — den raderar aldrig blint.

Torrsim utan risk:

```bash
/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java -jar hrvctl/target/hrvctl.jar watchdog --dry-run
```

`--dry-run` skriver bara vad den skulle ha gjort.

Logg: `~/.hrvctl/watchdog.log`.

**Macen måste vara vaken.** Sömn pausar launchd-timers, så en sovande Mac varken räknar ner eller raderar något.
Varför launchd och inte cron: cron är avvecklat på macOS och dess daemon stoppas av systemets sekretesskydd, så crontab-jobb körs i praktiken aldrig. launchd (`~/Library/LaunchAgents`) är det scheduler som gäller.

## Felsökning

Utloggad från Azure (token har gått ut).

```bash
az login
```

Menyn klagar på att den inte hittar repot. Stå i `~/kod/001_AZ401/HRV/hrv-backend` (eller någon undermapp) när du startar.

Bygget hittar inte Java. Använd `JAVA_HOME=`-formen ovan, den pekar ut Temurin 21 exakt.
