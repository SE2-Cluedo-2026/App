package at.aau.serg.websocketbrokerdemo

import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class DisconnectServiceTest {

    @Test
    fun `onBind returns null`() {
        val service = DisconnectService()

        val result: IBinder? = service.onBind(null)

        Assertions.assertNull(result)
    }

    @Test
    fun `onStartCommand with null intent uses lobby mode`() {
        val service = Mockito.spy(DisconnectService())

        val result = service.onStartCommand(null, 0, 1)

        Assertions.assertEquals(android.app.Service.START_NOT_STICKY, result)
    }

    @Test
    fun `onStartCommand with game mode returns not sticky`() {
        val service = Mockito.spy(DisconnectService())
        val intent = Mockito.mock(Intent::class.java)

        Mockito.`when`(intent.getStringExtra(DisconnectService.EXTRA_MODE))
            .thenReturn(DisconnectService.MODE_GAME)

        val result = service.onStartCommand(intent, 0, 1)

        Assertions.assertEquals(android.app.Service.START_NOT_STICKY, result)
    }

    @Test
    fun `onStartCommand with lobby mode returns not sticky`() {
        val service = Mockito.spy(DisconnectService())
        val intent = Mockito.mock(Intent::class.java)

        Mockito.`when`(intent.getStringExtra(DisconnectService.EXTRA_MODE))
            .thenReturn(DisconnectService.MODE_LOBBY)

        val result = service.onStartCommand(intent, 0, 1)

        Assertions.assertEquals(android.app.Service.START_NOT_STICKY, result)
    }

    @Test
    fun `onTaskRemoved in game mode does not crash`() {
        val service = Mockito.spy(DisconnectService())
        val intent = Mockito.mock(Intent::class.java)

        Mockito.`when`(intent.getStringExtra(DisconnectService.EXTRA_MODE))
            .thenReturn(DisconnectService.MODE_GAME)

        service.onStartCommand(intent, 0, 1)

        Mockito.mockStatic(Log::class.java).use {
            Assertions.assertDoesNotThrow {
                service.onTaskRemoved(null)
            }
        }
    }

    @Test
    fun `onTaskRemoved in lobby mode does not crash`() {
        val service = Mockito.spy(DisconnectService())
        val intent = Mockito.mock(Intent::class.java)

        Mockito.`when`(intent.getStringExtra(DisconnectService.EXTRA_MODE))
            .thenReturn(DisconnectService.MODE_LOBBY)

        service.onStartCommand(intent, 0, 1)

        Mockito.mockStatic(Log::class.java).use {
            Assertions.assertDoesNotThrow {
                service.onTaskRemoved(null)
            }
        }
    }
}