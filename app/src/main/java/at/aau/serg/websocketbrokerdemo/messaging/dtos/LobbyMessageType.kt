package at.aau.serg.websocketbrokerdemo.messaging.dtos

enum class LobbyMessageType {
    NEW_PLAYER_JOINED,
    PLAYER_REJOINED,
    GAME_FULL,
    PLAYER_REMOVED
}