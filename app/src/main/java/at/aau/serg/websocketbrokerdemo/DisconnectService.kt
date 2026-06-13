package at.aau.serg.websocketbrokerdemo

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class DisconnectService : Service() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_LOBBY = "LOBBY"
        const val MODE_GAME = "GAME"
    }

    private var mode = MODE_LOBBY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_LOBBY
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("DisconnectService", "App removed from recents (mode=$mode) — disconnecting")
        try {
            if (mode == MODE_LOBBY) {
                MyStomp.instance.leaveLobby()
            }
            MyStomp.instance.disconnect()
        } catch (e: Exception) {
            Log.e("DisconnectService", "Error during disconnect: ${e.message}")
        }
        stopSelf()
    }
}
