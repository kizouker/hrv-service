import java.io.File
import java.util.concurrent.TimeUnit

/** A finished process. */
data class ProcResult(val code: Int, val out: String)

/**
 * Runs [cmd], printing it first (with [redact] values masked).
 * Returns stdout+stderr and the exit code. Never throws for nonzero exit.
 */
fun runEchoed(cmd: List<String>, redact: Set<String> = emptySet(), workDir: File? = null): ProcResult {
    val shown = cmd.joinToString(" ") { arg ->
        var s = arg
        for (secret in redact) if (secret.isNotEmpty()) s = s.replace(secret, "***")
        if (' ' in s && !s.startsWith("***")) "\"$s\"" else s
    }
    Ui.term.println(Ui.dim("$ $shown"))
    return try {
        val proc = ProcessBuilder(cmd).apply {
            workDir?.let { directory(it) }
            redirectErrorStream(true)
        }.start()
        val out = proc.inputStream.bufferedReader().readText()
        proc.waitFor(10, TimeUnit.MINUTES)
        if (out.isNotBlank()) println(out.trimEnd())
        ProcResult(proc.exitValue(), out)
    } catch (e: Exception) {
        Ui.term.println(Ui.red("failed to start: ${e.message}"))
        ProcResult(127, "")
    }
}
