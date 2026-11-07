import java.io.File
import java.nio.file.Path

/** Azure constants + all az/docker invocations. Every command is echoed before it runs. */
object Azure {
    const val RG = "rg-hrv-dev"
    const val PG = "hrv-dev-pg"
    const val LOCATION = "swedencentral"
    const val ENVIRONMENT = "hrv-dev"
    const val IMAGE = "hrv-backend"

    /** Finds the repo root (dir containing infra/main.bicep), starting at the CWD. */
    fun findRepoRoot(from: Path = Path.of("").toAbsolutePath(), jarDir: File? = null): File? {
        var dir: File? = from.toFile()
        while (dir != null) {
            if (File(dir, "infra/main.bicep").isFile) return dir
            dir = dir.parentFile
        }
        if (jarDir != null) {
            var j: File? = jarDir
            repeat(4) {
                j = j?.parentFile
                if (j != null && File(j, "infra/main.bicep").isFile) return j
            }
        }
        return null
    }

    fun jarDir(): File? = try {
        val cp = System.getProperty("java.class.path") ?: return null
        File(cp.split(File.pathSeparator).first()).absoluteFile.parentFile
    } catch (_: Exception) {
        null
    }

    private fun az(vararg args: String, redact: Set<String> = emptySet()) =
        runEchoed(listOf("az") + args, redact)

    private fun docker(vararg args: String, workDir: File? = null) =
        runEchoed(listOf("docker") + args, workDir = workDir)

    fun opensslBase64(bytes: Int): String {
        val r = runEchoed(listOf("openssl", "rand", "-base64", bytes.toString()))
        if (r.code != 0) throw IllegalStateException("openssl failed")
        return r.out.trim()
    }

    fun ensureResourceGroup() {
        val show = az("group", "show", "-n", RG)
        if (show.code != 0) {
            Ui.term.println(Ui.yellow("Resource group $RG missing — creating it."))
            val create = az("group", "create", "-n", RG, "-l", LOCATION)
            if (create.code != 0) throw IllegalStateException("az group create failed")
        } else {
            Ui.term.println(Ui.green("Resource group $RG exists."))
        }
    }

    fun whatIf(repo: File) {
        Ui.term.println(Ui.title("What-if (no changes applied)"))
        val r = az(
            "deployment", "group", "what-if",
            "-g", RG, "--template-file", "infra/main.bicep",
            "--parameters", "environmentName=$ENVIRONMENT",
            "jwtSecret=what-if-dummy",
        )
        if (r.code != 0) Ui.term.println(Ui.red("what-if reported changes or errors — see above."))
    }

    /** Deploys infra. Secrets stay in memory only — never written to the repo. */
    fun deploy(repo: File) {
        ensureResourceGroup()
        Ui.term.println(Ui.title("Secrets (memory only, never written to disk)"))
        val envJwt = System.getenv("JWT_SECRET")?.takeIf { it.isNotBlank() }
        val jwtSecret: String
        if (envJwt != null) {
            Ui.term.println(Ui.green("Using JWT_SECRET from the environment."))
            jwtSecret = envJwt
        } else {
            jwtSecret = opensslBase64(32)
            Ui.term.println(Ui.dim("No JWT_SECRET in environment — generated fresh (${jwtSecret.length} chars, value hidden)."))
        }
        val secrets = setOf(jwtSecret)
        val r = runEchoed(
            listOf(
                "az", "deployment", "group", "create",
                "-g", RG, "--template-file", "infra/main.bicep",
                "--parameters", "environmentName=$ENVIRONMENT",
                "jwtSecret=$jwtSecret",
                "--query", "properties.outputs", "-o", "json",
            ),
            redact = secrets,
        )
        if (r.code != 0) throw IllegalStateException("deployment failed")
        Ui.term.println(Ui.green("Deploy finished. Outputs above."))
    }

    fun acrLoginServer(): String? {
        val r = az("acr", "list", "-g", RG, "--query", "[0].loginServer", "-o", "tsv")
        val server = r.out.trim().takeIf { r.code == 0 && it.isNotEmpty() && it != "null" }
        if (server == null) Ui.term.println(Ui.red("No container registry found in $RG. Deploy first."))
        return server
    }

    fun acrName(): String? {
        val r = az("acr", "list", "-g", RG, "--query", "[0].name", "-o", "tsv")
        return r.out.trim().takeIf { r.code == 0 && it.isNotEmpty() && it != "null" }
    }

    fun appName(): String? {
        val r = az("containerapp", "list", "-g", RG, "--query", "[0].name", "-o", "tsv")
        return r.out.trim().takeIf { r.code == 0 && it.isNotEmpty() && it != "null" }
    }

    /** Builds for linux/amd64, pushes to ACR, rolls the Container App to the new image. */
    fun buildAndPush(repo: File) {
        val server = acrLoginServer() ?: return
        val name = acrName() ?: return
        val tag = "$server/$IMAGE:latest"
        var r = docker("build", "--platform", "linux/amd64", "-t", tag, ".", workDir = repo)
        if (r.code != 0) throw IllegalStateException("docker build failed")
        r = az("acr", "login", "-n", name)
        if (r.code != 0) throw IllegalStateException("az acr login failed")
        r = docker("push", tag, workDir = repo)
        if (r.code != 0) throw IllegalStateException("docker push failed")
        val app = appName() ?: return
        r = az("containerapp", "update", "-n", app, "-g", RG, "-i", tag)
        if (r.code != 0) throw IllegalStateException("containerapp update failed")
        Ui.term.println(Ui.green("Container App $app now runs $tag."))
    }

    fun postgresState(): String? {
        val r = az("postgres", "flexible-server", "show", "-n", PG, "-g", RG,
            "--query", "state", "-o", "tsv")
        if (r.code != 0) return null
        return r.out.trim().takeIf { it.isNotEmpty() && it != "null" }
    }

    fun status() {
        Ui.term.println(Ui.title("Resources in $RG"))
        az("resource", "list", "-g", RG, "-o", "table")
        Ui.term.println()
        val state = postgresState()
        Ui.term.println(
            if (state == null) Ui.yellow("PostgreSQL $PG: not found (nothing to auto-delete).")
            else Ui.cyan("PostgreSQL $PG state: $state")
        )
        val paths = HrvPaths()
        val left = IdlePolicy.minutesLeft(
            Config.readLastActivity(paths),
            System.currentTimeMillis() / 1000,
            Config.readTimeoutMinutes(paths),
        )
        Ui.term.println(
            if (left == null) Ui.dim("No menu activity recorded yet.")
            else if (left <= 0) Ui.red("Idle timeout elapsed — watchdog will delete $PG.")
            else Ui.green("Auto-delete in ~$left min (timeout ${Config.readTimeoutMinutes(paths)} min).")
        )
    }

    fun deletePostgresNow() {
        Ui.term.println(Ui.red("This deletes ONLY the PostgreSQL server $PG."))
        Ui.term.println(Ui.red("The resource group and Key Vault are never touched."))
        if (!Menu.confirm("Delete $PG now?")) {
            Ui.term.println("Aborted.")
            return
        }
        val r = az("postgres", "flexible-server", "delete", "-n", PG, "-g", RG, "--yes", "--no-wait")
        if (r.code != 0) Ui.term.println(Ui.red("Delete failed — see above."))
        else Ui.term.println(Ui.green("Delete accepted for $PG."))
    }
}
