import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

/**
 * Pure idle-timeout policy. All time in epoch seconds; kept side-effect free
 * so it is unit-testable without touching disks, clocks or Azure.
 */
object IdlePolicy {
    const val DEFAULT_TIMEOUT_MINUTES = 15L

    /** Parses the configured timeout; blank/garbage/non-positive falls back to the default. */
    fun parseTimeout(raw: String?): Long {
        val v = raw?.trim()?.toLongOrNull()
        return if (v == null || v <= 0) DEFAULT_TIMEOUT_MINUTES else v
    }

    /**
     * Whole minutes of inactivity remaining before the watchdog acts.
     * Null when no activity was ever recorded (unknown — caller decides).
     */
    fun minutesLeft(lastActivityEpochSec: Long?, nowEpochSec: Long, timeoutMinutes: Long): Long? {
        if (lastActivityEpochSec == null) return null
        return timeoutMinutes - (nowEpochSec - lastActivityEpochSec) / 60
    }

    /** True only when we have seen activity AND the timeout has fully elapsed. */
    fun shouldDelete(lastActivityEpochSec: Long?, nowEpochSec: Long, timeoutMinutes: Long): Boolean {
        val left = minutesLeft(lastActivityEpochSec, nowEpochSec, timeoutMinutes) ?: return false
        return left <= 0
    }
}

/** On-disk state under ~/.hrvctl (injectable base for tests). */
class HrvPaths(base: Path = Path.of(System.getProperty("user.home"), ".hrvctl")) {
    val dir: Path = base
    val configFile: Path = base.resolve("config")
    val activityFile: Path = base.resolve("last-activity")
    val logFile: Path = base.resolve("watchdog.log")
}

object Config {
    private const val TIMEOUT_KEY = "idle_timeout_minutes"

    fun readTimeoutMinutes(paths: HrvPaths = HrvPaths()): Long {
        if (!paths.configFile.exists()) return IdlePolicy.DEFAULT_TIMEOUT_MINUTES
        return try {
            val props = Properties()
            paths.configFile.inputStream().use { props.load(it) }
            IdlePolicy.parseTimeout(props.getProperty(TIMEOUT_KEY))
        } catch (_: Exception) {
            IdlePolicy.DEFAULT_TIMEOUT_MINUTES
        }
    }

    fun writeTimeoutMinutes(minutes: Long, paths: HrvPaths = HrvPaths()) {
        paths.dir.createDirectories()
        val props = Properties()
        if (paths.configFile.exists()) {
            try { paths.configFile.inputStream().use { props.load(it) } } catch (_: Exception) { /* overwrite */ }
        }
        props.setProperty(TIMEOUT_KEY, minutes.toString())
        paths.configFile.outputStream().use { props.store(it, "hrvctl config") }
    }

    fun touchActivity(paths: HrvPaths = HrvPaths()) {
        paths.dir.createDirectories()
        Files.writeString(paths.activityFile, "${System.currentTimeMillis() / 1000}\n")
    }

    fun readLastActivity(paths: HrvPaths = HrvPaths()): Long? {
        return try {
            if (!paths.activityFile.exists()) null
            else Files.readString(paths.activityFile).trim().toLongOrNull()
        } catch (_: Exception) {
            null
        }
    }
}
