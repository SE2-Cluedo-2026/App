package at.aau.serg.websocketbrokerdemo.model

import at.aau.serg.websocketbrokerdemo.messaging.dtos.ExistingPlayerDTO

object ClientState {
    var gameStatus: String = "LOBBY"
    var availableCharacters: List<String> = emptyList()

    var playerId: String = ""
    var myCards: List<String> = emptyList()
    var myCharacter: String? = null

    var seenCards: MutableSet<String> = mutableSetOf()

    var players: List<ExistingPlayerDTO> = emptyList()

    var currentPlayerId: String = ""

    var remainingMoves: Int = 0
    var playerPositions: MutableMap<String, String> = mutableMapOf()
     var currentPhase: String = ""
    var currentPlayerIndex: Int = 0
    var isEliminated: Boolean = false
    var eliminatedPlayers: MutableSet<String> = mutableSetOf()
    var playerCharacterMap: MutableMap<String, String> = mutableMapOf()
    var cheatUsed: Boolean = false
}
