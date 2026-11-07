package dev.podcast.nativeapp
import org.junit.Test
import org.junit.Assert.*
class PlayerGesturePolicyTest {
    @Test fun shortSlowPullReturnsToExpandedPlayer() { assertFalse(PlayerGesturePolicy.dismiss(140f,2000f,100f)) }
    @Test fun deliberatePullClosesEvenWithoutVelocity() { assertTrue(PlayerGesturePolicy.dismiss(500f,2000f,0f)) }
    @Test fun fastFlickCanCloseBeforeDistanceThreshold() { assertTrue(PlayerGesturePolicy.dismiss(150f,2000f,1800f)) }
    @Test fun UpwardOrInvalidInputDoesNotClose() {
        assertFalse(PlayerGesturePolicy.dismiss(-300f,2000f,-2000f))
        assertFalse(PlayerGesturePolicy.dismiss(Float.NaN,2000f,500f))
        assertFalse(PlayerGesturePolicy.dismiss(100f,0f,2000f))
    }
}
