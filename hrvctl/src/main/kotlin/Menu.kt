import com.github.ajalt.mordant.widgets.Panel
import com.github.ajalt.mordant.widgets.Text

/** Interactive menu loop. Every completed action refreshes the activity timestamp. */
object Menu {
    fun confirm(question: String): Boolean {
        Ui.term.print(Ui.yellow("$question [y/N] "))
        return readlnOrNull()?.trim()?.lowercase() == "y"
    }

    private fun prompt(text: String): String? {
        Ui.term.print(Ui.cyan("$text "))
        return readlnOrNull()?.trim()
    }

    fun run(repo: java.io.File) {
        var running = true
        while (running) {
            Ui.term.println()
            Ui.term.println(Panel(Text(Ui.yellow(dieselHeader())), title = Text(Ui.title("hrvctl — Diesel on duty"))))
            Ui.term.println(Ui.bold("Azure for Students · ${Azure.RG} · ${Azure.PG}"))
            Ui.term.println(
                """
                ${Ui.cyan("1")}  What-if (preview Bicep changes)
                ${Ui.cyan("2")}  Deploy (RG + secrets + Bicep)
                ${Ui.cyan("3")}  Build + push image, update Container App
                ${Ui.cyan("4")}  Status (resources, Postgres, auto-delete countdown)
                ${Ui.cyan("5")}  Delete PostgreSQL now (server only)
                ${Ui.cyan("6")}  Change idle timeout (current: ${Config.readTimeoutMinutes()} min)
                ${Ui.cyan("7")}  Install / uninstall watchdog (${if (Launchd.installed()) "installed" else "not installed"})
                ${Ui.cyan("8")}  Quit
                """.trimIndent()
            )
            when (prompt("Choose [1-8]:")) {
                "1" -> { runCatching { Azure.whatIf(repo) }; Config.touchActivity() }
                "2" -> { runCatching { Azure.deploy(repo) }; Config.touchActivity() }
                "3" -> { runCatching { Azure.buildAndPush(repo) }; Config.touchActivity() }
                "4" -> { runCatching { Azure.status() }; Config.touchActivity() }
                "5" -> { runCatching { Azure.deletePostgresNow() }; Config.touchActivity() }
                "6" -> { changeTimeout(); Config.touchActivity() }
                "7" -> { toggleWatchdog(); Config.touchActivity() }
                "8" -> { Ui.term.println(Ui.dim("Bye. Diesel keeps watch.")); running = false }
                else -> Ui.term.println(Ui.red("Pick 1-8."))
            }
        }
    }

    private fun changeTimeout() {
        val current = Config.readTimeoutMinutes()
        val raw = prompt("Idle timeout in minutes (current $current, default ${IdlePolicy.DEFAULT_TIMEOUT_MINUTES}):")
        if (raw.isNullOrBlank()) {
            Ui.term.println("Unchanged.")
            return
        }
        val parsed = raw.toLongOrNull()
        if (parsed == null || parsed <= 0) {
            Ui.term.println(Ui.red("Not a positive number — unchanged."))
            return
        }
        Config.writeTimeoutMinutes(parsed)
        Ui.term.println(Ui.green("Idle timeout set to $parsed min (saved in ~/.hrvctl/config)."))
    }

    private fun toggleWatchdog() {
        if (Launchd.installed()) {
            Ui.term.println("Watchdog launchd job is ${if (Launchd.running()) "loaded" else "not loaded"}.")
            if (confirm("Uninstall it?")) Launchd.uninstall()
        } else {
            Ui.term.println("Installs a launchd job running `hrvctl watchdog` every 60s.")
            if (confirm("Install it?")) Launchd.install()
        }
    }
}
