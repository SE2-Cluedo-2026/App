package at.aau.serg.websocketbrokerdemo

import android.hardware.SensorEvent
import android.hardware.SensorManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
class ShakeDetectorTest {

    private lateinit var shakeDetector: ShakeDetector
    private var shakeCount = 0

    @Before
    fun setUp() {
        shakeCount = 0
        shakeDetector = ShakeDetector { shakeCount++ }
    }

    private fun createSensorEvent(x: Float, y: Float, z: Float): SensorEvent {
        val constructor = SensorEvent::class.java.getDeclaredConstructor(Int::class.java)
        constructor.isAccessible = true
        val event = constructor.newInstance(3)
        val valuesField: Field = SensorEvent::class.java.getDeclaredField("values")
        valuesField.isAccessible = true
        valuesField.set(event, floatArrayOf(x, y, z))
        return event
    }

    private fun setLastShakeTime(timeMs: Long) {
        val field = ShakeDetector::class.java.getDeclaredField("lastShakeTime")
        field.isAccessible = true
        field.set(shakeDetector, timeMs)
    }

    @Test
    fun `onSensorChanged with null event does nothing`() {
        shakeDetector.onSensorChanged(null)
        assertEquals(0, shakeCount)
    }

    @Test
    fun `onSensorChanged below threshold does not trigger onShake`() {
        val event = createSensorEvent(0f, 0f, SensorManager.GRAVITY_EARTH)
        shakeDetector.onSensorChanged(event)
        assertEquals(0, shakeCount)
    }

    @Test
    fun `onSensorChanged at exact threshold does not trigger onShake`() {
        val total = SensorManager.GRAVITY_EARTH + 12f
        val event = createSensorEvent(total, 0f, 0f)
        shakeDetector.onSensorChanged(event)
        assertEquals(0, shakeCount)
    }

    @Test
    fun `onSensorChanged above threshold triggers onShake`() {
        val event = createSensorEvent(30f, 30f, 30f)
        shakeDetector.onSensorChanged(event)
        assertEquals(1, shakeCount)
    }

    @Test
    fun `onSensorChanged second shake within cooldown is ignored`() {
        val event = createSensorEvent(30f, 30f, 30f)
        shakeDetector.onSensorChanged(event)
        shakeDetector.onSensorChanged(event)
        assertEquals(1, shakeCount)
    }

    @Test
    fun `onSensorChanged second shake after cooldown triggers onShake again`() {
        val event = createSensorEvent(30f, 30f, 30f)
        shakeDetector.onSensorChanged(event)
        setLastShakeTime(System.currentTimeMillis() - 1500L)
        shakeDetector.onSensorChanged(event)
        assertEquals(2, shakeCount)
    }

    @Test
    fun `onAccuracyChanged does not throw`() {
        shakeDetector.onAccuracyChanged(null, 0)
    }

    @Test
    fun `onSensorChanged with negative axis values triggers onShake`() {
        val event = createSensorEvent(-30f, -30f, -30f)
        shakeDetector.onSensorChanged(event)
        assertEquals(1, shakeCount)
    }
}