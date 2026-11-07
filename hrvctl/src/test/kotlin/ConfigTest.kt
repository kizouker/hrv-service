import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import java.nio.file.Files

/** Config read/write round-trip against an isolated temp dir (never the real ~/.hrvctl). */
class ConfigTest {

    private fun tempPaths(): HrvPaths {
        val dir: Path = Files.createTempDirectory("hrvctl-test")
        return HrvPaths(dir)
    }

    @Test
    fun timeout_defaultsTo15WhenMissing() {
        assertEquals(15L, Config.readTimeoutMinutes(tempPaths()))
    }

    @Test
    fun timeout_roundTrips() {
        val paths = tempPaths()
        Config.writeTimeoutMinutes(42L, paths)
        assertEquals(42L, Config.readTimeoutMinutes(paths))
    }

    @Test
    fun timeout_garbageFallsBackToDefault() {
        val paths = tempPaths()
        paths.dir.createDirectories()
        Files.writeString(paths.configFile, "idle_timeout_minutes=soon\n")
        assertEquals(15L, Config.readTimeoutMinutes(paths))
    }

    @Test
    fun activity_missingIsNullAndTouchRecordsNow() {
        val paths = tempPaths()
        assertNull(Config.readLastActivity(paths))
        val before = System.currentTimeMillis() / 1000
        Config.touchActivity(paths)
        val read = Config.readLastActivity(paths)
        val after = System.currentTimeMillis() / 1000
        assertTrue(read != null && read in before..after)
    }

    @Test
    fun activity_garbageIsNull() {
        val paths = tempPaths()
        paths.dir.createDirectories()
        Files.writeString(paths.activityFile, "yesterday-ish\n")
        assertNull(Config.readLastActivity(paths))
    }
}
