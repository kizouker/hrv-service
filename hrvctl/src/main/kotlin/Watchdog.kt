import java.nio.file.Files
import java.time.Instant
import kotlin.io.path.createDirectories

/** Non-interactive watchdog: `hrvctl watchdog [--dry-run]`. Never deletes anything but hrv-dev-pg. */
object Watchdog {
    fun run(dryRun: Boolean = false, paths: HrvPaths = HrvPaths(), nowEpochSec: Long = System.currentTimeMillis() / 1000): Int {
        fun log(msg: String) {
            val line = "${Instant.now()} $msg"
            try {
                paths.dir.createDirectories()
                Files.writeString(paths.logFile, line + "\n",
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)
            } catch (_: Exception) { /* log file must never break the watchdog */ }
            Ui.term.println(if (dryRun) Ui.dim("[dry-run] $msg") else msg)
        }

        val timeout = Config.readTimeoutMinutes(paths)
        val last = Config.readLastActivity(paths)
        log("watchdog check: timeout=${timeout}m lastActivity=${last ?: "never"}")

        if (Azure.postgresState() == null) {
            log("${Azure.PG} not found — nothing to do.")
            return 0
        }
        if (!IdlePolicy.shouldDelete(last, nowEpochSec, timeout)) {
            val left = IdlePolicy.minutesLeft(last, nowEpochSec, timeout)
            log("${Azure.PG} kept (idle ${left ?: "?"} min left).")
            return 0
        }
        if (dryRun) {
            log("would delete ${Azure.PG} (idle timeout elapsed).")
            return 0
        }
        log("idle timeout elapsed — deleting ${Azure.PG} (resource group and Key Vault untouched).")
        val r = runEchoed(listOf("az", "postgres", "flexible-server", "delete",
            "-n", Azure.PG, "-g", Azure.RG, "--yes", "--no-wait"))
        log(if (r.code == 0) "delete accepted for ${Azure.PG}." else "delete FAILED (exit ${r.code}).")
        return r.code
    }
}
