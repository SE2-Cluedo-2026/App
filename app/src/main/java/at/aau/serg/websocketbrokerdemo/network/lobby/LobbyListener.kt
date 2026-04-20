package at.aau.serg.websocketbrokerdemo.network.lobby

interface LobbyListener {
    fun onJoinSuccess(message: String)
    fun onPlayersReceived(players: List<String>)
}
