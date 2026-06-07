package at.aau.serg.websocketbrokerdemo

interface Callbacks {
    fun onResponse(res: String);
    fun onConnected();
    fun onConnectionFailed(reason: String)
    fun onConnectionLost(reason: String)
}