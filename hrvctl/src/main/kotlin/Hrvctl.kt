fun printHelp() {
    println(
        """
        hrvctl — control tool for the HRV backend on Azure.

        Usage:
          hrvctl                  interactive TUI menu
          hrvctl watchdog         one watchdog check (used by the launchd job)
          hrvctl watchdog --dry-run   same, but never deletes
          hrvctl --help           this text

        Config & state live in ~/.hrvctl/ (config, last-activity, watchdog.log).
        The watchdog deletes ONLY the PostgreSQL server (${Azure.PG}) after the
        idle timeout — never the resource group or Key Vault.
        """.trimIndent()
    )
}

fun main(args: Array<String>) {
    when {
        args.contains("--help") || args.contains("-h") -> printHelp()
        args.firstOrNull() == "watchdog" -> {
            val code = Watchdog.run(dryRun = args.contains("--dry-run"))
            kotlin.system.exitProcess(code)
        }
        args.isNotEmpty() -> {
            Ui.term.println(Ui.red("Unknown arguments: ${args.joinToString(" ")}"))
            printHelp()
            kotlin.system.exitProcess(2)
        }
        else -> {
            val repo = Azure.findRepoRoot(jarDir = Azure.jarDir())
            if (repo == null) {
                Ui.term.println(Ui.red("Cannot find the repo (looked for infra/main.bicep). Run from inside the checkout."))
                kotlin.system.exitProcess(1)
            }
            Menu.run(repo)
        }
    }
}
