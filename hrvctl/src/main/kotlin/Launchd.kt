import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

/**
 * macOS scheduling via launchd (not cron).
 *
 * Why not cron: cron has been deprecated on macOS since Tiger, and on modern
 * macOS the cron daemon is blocked by Transparency Consent and Control —
 * user crontabs typically never run unless Full Disk Access is granted to
 * `/usr/sbin/cron`, failing silently. launchd (`~/Library/LaunchAgents`) is
 * the supported per-user scheduler. Note the Mac must be awake: sleep
 * suspends launchd timers, so the watchdog only fires while awake.
 */
object Launchd {
    const val LABEL = "com.hrvctl.watchdog"
    val plist: Path = Path.of(System.getProperty("user.home"), "Library", "LaunchAgents", "$LABEL.plist")

    private fun javaBin(): String =
        Path.of(System.getProperty("java.home"), "bin", "java").toString()

    fun jarPath(): String? = try {
        val cp = System.getProperty("java.class.path") ?: return null
        cp.split(java.io.File.pathSeparator).first()
    } catch (_: Exception) {
        null
    }

    private fun plistXml(java: String, jar: String, log: String): String = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key><string>$LABEL</string>
    <key>ProgramArguments</key>
    <array><string>$java</string><string>-jar</string><string>$jar</string><string>watchdog</string></array>
    <key>StartInterval</key><integer>60</integer>
    <key>StandardOutPath</key><string>$log.launchd.log</string>
    <key>StandardErrorPath</key><string>$log.launchd.log</string>
</dict>
</plist>
"""

    fun installed(): Boolean = plist.exists()

    fun running(): Boolean {
        val uid = runEchoed(listOf("id", "-u")).out.trim()
        val r = runEchoed(listOf("launchctl", "print", "gui/$uid/$LABEL"))
        return r.code == 0
    }

    fun install(): Boolean {
        val jar = jarPath()
        if (jar == null || !java.io.File(jar).isFile) {
            Ui.term.println(Ui.red("Cannot locate the running hrvctl jar — install from a built jar."))
            return false
        }
        val log = HrvPaths().logFile.toString()
        plist.parent.createDirectories()
        Files.writeString(plist, plistXml(javaBin(), jar, log))
        Ui.term.println(Ui.dim("wrote $plist"))
        val uid = runEchoed(listOf("id", "-u")).out.trim()
        runEchoed(listOf("launchctl", "bootout", "gui/$uid/$LABEL"))
        val r = runEchoed(listOf("launchctl", "bootstrap", "gui/$uid", plist.toString()))
        return if (r.code == 0) {
            Ui.term.println(Ui.green("Watchdog scheduled every 60s via launchd."))
            true
        } else {
            Ui.term.println(Ui.red("launchctl bootstrap failed — see above."))
            false
        }
    }

    fun uninstall(): Boolean {
        val uid = runEchoed(listOf("id", "-u")).out.trim()
        runEchoed(listOf("launchctl", "bootout", "gui/$uid/$LABEL"))
        return if (plist.deleteIfExists()) {
            Ui.term.println(Ui.green("Watchdog unscheduled and plist removed."))
            true
        } else {
            Ui.term.println(Ui.yellow("No plist found — nothing to remove."))
            false
        }
    }
}
