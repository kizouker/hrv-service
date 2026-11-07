import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal

/** Shared terminal + small style helpers. */
object Ui {
    val term = Terminal()
    fun red(s: String) = TextColors.red(s)
    fun green(s: String) = TextColors.green(s)
    fun yellow(s: String) = TextColors.yellow(s)
    fun cyan(s: String) = TextColors.cyan(s)
    fun dim(s: String) = TextColors.gray(s)
    fun bold(s: String) = TextStyles.bold(s)
    fun title(s: String) = (TextStyles.bold + TextColors.cyan)(s)
}
