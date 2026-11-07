import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdlePolicyTest {

    @Test
    fun parseTimeout_blankAndGarbageFallBackToDefault() {
        assertEquals(15L, IdlePolicy.parseTimeout(null))
        assertEquals(15L, IdlePolicy.parseTimeout(""))
        assertEquals(15L, IdlePolicy.parseTimeout("   "))
        assertEquals(15L, IdlePolicy.parseTimeout("soon"))
    }

    @Test
    fun parseTimeout_nonPositiveFallsBackToDefault() {
        assertEquals(15L, IdlePolicy.parseTimeout("0"))
        assertEquals(15L, IdlePolicy.parseTimeout("-5"))
    }

    @Test
    fun parseTimeout_acceptsPositiveValues() {
        assertEquals(30L, IdlePolicy.parseTimeout("30"))
        assertEquals(5L, IdlePolicy.parseTimeout("  5  "))
    }

    @Test
    fun minutesLeft_countsDownFromTimeout() {
        // 15 min timeout, last activity 5 min ago -> 10 left
        assertEquals(10L, IdlePolicy.minutesLeft(1000L, 1300L, 15L))
    }

    @Test
    fun minutesLeft_neverActiveIsNull() {
        assertNull(IdlePolicy.minutesLeft(null, 1300L, 15L))
    }

    @Test
    fun minutesLeft_goesNegativeWhenOverdue() {
        assertEquals(-1L, IdlePolicy.minutesLeft(1000L, 1000L + 16 * 60, 15L))
    }

    @Test
    fun shouldDelete_requiresSeenActivityPastTimeout() {
        assertFalse(IdlePolicy.shouldDelete(null, 9999L, 15L), "never active -> never delete")
        assertFalse(IdlePolicy.shouldDelete(1000L, 1300L, 15L), "5 min idle of 15 -> keep")
        assertTrue(IdlePolicy.shouldDelete(1000L, 1000L + 15 * 60, 15L), "exactly at timeout -> delete")
        assertTrue(IdlePolicy.shouldDelete(1000L, 1000L + 60 * 60, 15L), "long overdue -> delete")
    }
}
