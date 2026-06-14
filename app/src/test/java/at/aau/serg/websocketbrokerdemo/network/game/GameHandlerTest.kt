package at.aau.serg.websocketbrokerdemo.network.game

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import android.util.Log
import org.mockito.Mockito

class GameHandlerTest {

    @BeforeEach
    fun setup() {
        GameHandler.onRollDice = null
        GameHandler.onMove = null
        GameHandler.onEndTurn = null
        GameHandler.onEnterRoom = null
        GameHandler.onHiddenWay = null
        GameHandler.onAccusation = null
        GameHandler.onSuggestionResult = null
        GameHandler.onSuggestionRequest = null
        GameHandler.onCheatResult = null
        GameHandler.onGameFinished = null
        GameHandler.onGameAborted = null
        GameHandler.onGamePaused = null
        GameHandler.onContinueGame = null
        GameHandler.onGameError = null
    }

    private fun handle(block: () -> Unit) {
        Mockito.mockStatic(Log::class.java).use { block() }
    }

    @Test
    fun `GameHandler can be instantiated`() = handle {
        val handler = GameHandler()
        Assertions.assertNotNull(handler)
    }

    @Test
    fun `invalid JSON does not crash`() = handle {
        GameHandler.handle("not a json")
        Assertions.assertTrue(true)
    }

    @Test
    fun `unknown type goes to catch block`() = handle {
        GameHandler.handle("""{ "type": "UNKNOWN_TYPE" }""")
        Assertions.assertTrue(true)
    }


    @Test
    fun `ROLL_DICE calls callback`() = handle {
        var result = 0
        GameHandler.onRollDice = { _, value, _ ->
            result = value
        }
        GameHandler.handle("""{ "type": "ROLL_DICE", "payload": { "value": 5 } }""")
        Assertions.assertEquals(5, result)
    }

    @Test
    fun `ROLL_DICE with newPosition updates state`() = handle {
        var pos: String? = null
        GameHandler.onRollDice = { _, _, newPosition ->
            pos = newPosition
        }
        GameHandler.handle("""{ "type": "ROLL_DICE", "payload": { "value": 3, "newPosition": "5,5", "playerId": "p1" } }""")
        Assertions.assertEquals("5,5", pos)
    }

    @Test
    fun `ROLL_DICE with currentPhase updates ClientState`() = handle {
        GameHandler.handle("""{ "type": "ROLL_DICE", "payload": { "value": 3, "currentPhase": "WAITING_FOR_ROLL" } }""")
        Assertions.assertEquals("WAITING_FOR_ROLL", at.aau.serg.websocketbrokerdemo.model.ClientState.currentPhase)
    }

    @Test
    fun `ROLL_DICE without currentPhase does not update ClientState`() = handle {
        at.aau.serg.websocketbrokerdemo.model.ClientState.currentPhase = "OLD_PHASE"
        GameHandler.handle("""{ "type": "ROLL_DICE", "payload": { "value": 3 } }""")
        Assertions.assertEquals("OLD_PHASE", at.aau.serg.websocketbrokerdemo.model.ClientState.currentPhase)
    }

    @Test
    fun `ROLL_DICE with null payload does not crash`() = handle {
        var called = false
        GameHandler.onRollDice = { _, _, _ ->
            called = true
        }
        GameHandler.handle("""{ "type": "ROLL_DICE" }""")
        Assertions.assertFalse(called)
    }

    @Test
    fun `ROLL_DICE no callback set does not crash`() = handle {
        GameHandler.handle("""{ "type": "ROLL_DICE", "payload": { "value": 5 } }""")
        Assertions.assertTrue(true)
    }

    @Test
    fun `MOVE calls callback`() = handle {
        var p = ""
        var pos = ""
        GameHandler.onMove = {
            player, position, _ -> p = player; pos = position
        }
        GameHandler.handle("""{ "type": "MOVE", "payload": { "playerId": "p1", "position": "A1", "movesLeft": 2 } }""")
        Assertions.assertEquals("p1", p)
        Assertions.assertEquals("A1", pos)
    }

    @Test
    fun `MOVE with missing playerId does not crash`() = handle {
        var called = false
        GameHandler.onMove = {
            _, _, _ -> called = true
        }
        GameHandler.handle("""{ "type": "MOVE", "payload": { } }""")
        Assertions.assertFalse(called)
    }

    @Test
    fun `MOVE with null payload does not crash`() = handle {
        var called = false
        GameHandler.onMove = {
                _, _, _ -> called = true
        }
        GameHandler.handle("""{ "type": "MOVE" }""")
        Assertions.assertFalse(called)
    }

    @Test
    fun `MOVE no callback set does not crash`() = handle {
        GameHandler.handle("""{ "type": "MOVE", "payload": { "playerId": "p1", "position": "A1", "movesLeft": 2 } }""")
        Assertions.assertTrue(true)
    }

    @Test
    fun `END_TURN calls callback`() = handle {
        var currentPlayerIndex = -1
        GameHandler.onEndTurn = {
            index -> currentPlayerIndex = index
        }
        GameHandler.handle("""{ "type": "END_TURN", "payload": { "currentPlayerIndex": 2 } }""")
        Assertions.assertEquals(2, currentPlayerIndex)
    }

    @Test
    fun `END_TURN with missing currentPlayerIndex does not crash`() = handle {
        var called = false
        GameHandler.onEndTurn = {
            called = true
        }
        GameHandler.handle("""{ "type": "END_TURN", "payload": { } }""")
        Assertions.assertFalse(called)
    }

    @Test
    fun `END_TURN with null payload does not crash`() = handle {
        var called = false
        GameHandler.onEndTurn = {
                _ -> called = true
        }
        GameHandler.handle("""{ "type": "END_TURN" }""")
        Assertions.assertFalse(called)
    }

    @Test
    fun `END_TURN no callback set does not crash`() = handle {
        GameHandler.handle("""{ "type": "END_TURN", "payload": { "currentPlayerIndex": 2 } }""")
        Assertions.assertTrue(true)
    }

    @Test
    fun `ENTER_ROOM calls callback`() = handle {
        var room = ""
        GameHandler.onEnterRoom = {
                _, roomId -> room = roomId
        }
        GameHandler.handle("""{ "type": "ENTER_ROOM", "payload": { "playerId": "p1", "roomId": "room1" } }""")
        Assertions.assertEquals("room1", room)
    }

    @Test
    fun `ENTER_ROOM with missing playerId does not crash`() = handle {
        var called = false
        GameHandler.onEnterRoom = {
            _, _ -> called = true
        }
        GameHandler.handle("""{ "type": "ENTER_ROOM", "payload": { } }""")
        Assertions.assertFalse(called)
    }

    @Test
    fun `ENTER_ROOM with null payload does not crash`() = handle {
        var called = false
        GameHandler.onEnterRoom = {
                _, _ -> called = true
        }
        GameHandler.handle("""{ "type": "ENTER_ROOM" }""")
        Assertions.assertFalse(called)
    }

    @Test
    fun `ENTER_ROOM no callback set does not crash`() = handle {
        GameHandler.handle("""{ "type": "ENTER_ROOM", "payload": { "playerId": "p1", "roomId": "room1" } }""")
        Assertions.assertTrue(true)
    }

    @Test
    fun `TAKE_HIDDEN_WAY calls callback`() = handle {
        var called = false
        GameHandler.onHiddenWay = {
            _, _ -> called = true
        }
        GameHandler.handle("""{ "type": "TAKE_HIDDEN_WAY", "payload": { "playerId": "p1", "targetRoom": "room1" } }""")
        Assertions.assertTrue(called)
    }

    @Test
    fun `TAKE_HIDDEN_WAY with missing playerId does not crash`() = handle {
        var called = false
        GameHandler.onHiddenWay = {
                _, _ -> called = true
        }
        GameHandler.handle("""{ "type": "TAKE_HIDDEN_WAY", "payload": { } }""")
        Assertions.assertFalse(called)
    }

    @Test
    fun `TAKE_HIDDEN_WAY with null payload does not crash`() = handle {
        var called = false
        GameHandler.onHiddenWay = {
                _, _ -> called = true
        }
        GameHandler.handle("""{ "type": "TAKE_HIDDEN_WAY" }""")
        Assertions.assertFalse(called)
    }

    @Test
    fun `TAKE_HIDDEN_WAY no callback set does not crash`() = handle {
        GameHandler.handle("""{ "type": "TAKE_HIDDEN_WAY", "payload": { "playerId": "p1", "targetRoom": "room1" } }""")
        Assertions.assertTrue(true)
    }

    @Test
    fun `MAKE_ACCUSATION calls callback`() = handle {
        var called = false
        GameHandler.onAccusation = {
            _, _, _, _, _, _ -> called = true
        }
        GameHandler.handle("""{ "type": "MAKE_ACCUSATION", "payload": { "accuserID": "p1", "suspect": "s", "room": "r", "weapon": "w", "correct": true } }""")
        Assertions.assertTrue(called)
    }

    @Test
    fun `MAKE_ACCUSATION with eliminated true sets isEliminated`() = handle {
        at.aau.serg.websocketbrokerdemo.model.ClientState.playerId = "p1"
        at.aau.serg.websocketbrokerdemo.model.ClientState.isEliminated = false
        GameHandler.handle("""{ "type": "MAKE_ACCUSATION", "payload": { "accuserID": "p1", "suspect": "s", "room": "r", "weapon": "w", "correct": false, "eliminated": true } }""")
        Assertions.assertTrue(at.aau.serg.websocketbrokerdemo.model.ClientState.isEliminated)
    }

    @Test
    fun `MAKE_ACCUSATION with null payload does not crash`() = handle {
        var called = false
        GameHandler.onAccusation = {
                _, _, _, _, _, _ -> called = true
        }
        GameHandler.handle("""{ "type": "MAKE_ACCUSATION" }""")
        Assertions.assertFalse(called)
    }

    @Test
    fun `MAKE_ACCUSATION no callback set does not crash`() = handle {
        GameHandler.handle("""{ "type": "MAKE_ACCUSATION", "payload": { "accuserID": "p1", "suspect": "s", "room": "r", "weapon": "w", "correct": true } }""")
        Assertions.assertTrue(true)
    }

    @Test
    fun `SUGGESTION_RESULT calls callback`() = handle {
        var called = false
        GameHandler.onSuggestionResult = {
            _, _, _, _, _ -> called = true
        }
        GameHandler.handle("""{ "type": "SUGGESTION_RESULT", "payload": { "suggesterID": "p1", "suspect": "s", "room": "r", "weapon": "w", "matchingCards": [] } }""")
        Assertions.assertTrue(called)
    }

    @Test
    fun `SUGGESTION_RESULT with matching cards calls callback`() = handle {
        var cards = listOf<String>()
        GameHandler.onSuggestionResult = {
                _, _, _, _, matchingCards -> cards = matchingCards
        }
        GameHandler.handle("""{ "type": "SUGGESTION_RESULT", "payload": { "suggesterID": "p1", "suspect": "s", "room": "r", "weapon": "w", "matchingCards": [{"name": "knife"}] } }""")
        Assertions.assertEquals(listOf("knife"), cards)
    }

    @Test
    fun `SUGGESTION_RESULT with suggesterID matching playerId adds to seenCards`() = handle {
        at.aau.serg.websocketbrokerdemo.model.ClientState.playerId = "p1"
        at.aau.serg.websocketbrokerdemo.model.ClientState.seenCards.clear()
        GameHandler.handle("""{ "type": "SUGGESTION_RESULT", "payload": { "suggesterID": "p1", "suspect": "s", "room": "r", "weapon": "w", "matchingCards": [{"name": "knife"}] } }""")
        Assertions.assertTrue(at.aau.serg.websocketbrokerdemo.model.ClientState.seenCards.contains("knife"))
    }

    @Test
    fun `SUGGESTION_RESULT with null matchingCards array does not crash`() = handle {
        var cards = listOf<String>()
        GameHandler.onSuggestionResult = {
                _, _, _, _, matchingCards -> cards = matchingCards
        }
        GameHandler.handle("""{ "type": "SUGGESTION_RESULT", "payload": { "suggesterID": "p1", "suspect": "s", "room": "r", "weapon": "w" } }""")
        Assertions.assertTrue(cards.isEmpty())
    }

    @Test
    fun `SUGGESTION_RESULT with null payload does not crash`() = handle {
        var called = false
        GameHandler.onSuggestionResult = { _, _, _, _, _ -> called = true }
        GameHandler.handle("""{ "type": "SUGGESTION_RESULT" }""")
        Assertions.assertFalse(called)
    }

    @Test
    fun `SUGGESTION_RESULT no callback set does not crash`() = handle {
        GameHandler.handle("""{ "type": "SUGGESTION_RESULT", "payload": { "suggesterID": "p1", "suspect": "s", "room": "r", "weapon": "w", "matchingCards": [] } }""")
        Assertions.assertTrue(true)
    }

    @Test
    fun `SUGGESTION_REQUEST calls callback`() = handle {
        var called = false
        GameHandler.onSuggestionRequest = { _, _, _, _, _, _ -> called = true }
        GameHandler.handle("""{ "type": "SUGGESTION_REQUEST", "payload": { "suggesterID": "p1", "suspect": "s", "room": "r", "weapon": "w", "cheatWindowSeconds": 5 } }""")
        Assertions.assertTrue(called)
    }

    @Test
    fun `SUGGESTION_REQUEST with matching cards calls callback with cards`() = handle {
        var cards = listOf<String>()
        GameHandler.onSuggestionRequest = { _, _, _, _, _, matchingCards -> cards = matchingCards }
        GameHandler.handle("""{ "type": "SUGGESTION_REQUEST", "payload": { "suggesterID": "p1", "suspect": "s", "room": "r", "weapon": "w", "cheatWindowSeconds": 5, "matchingCards": [{"name": "knife"}] } }""")
        Assertions.assertEquals(listOf("knife"), cards)
    }

    @Test
    fun `SUGGESTION_REQUEST with null payload does not crash`() = handle {
        var called = false
        GameHandler.onSuggestionRequest = { _, _, _, _, _, _ -> called = true }
        GameHandler.handle("""{ "type": "SUGGESTION_REQUEST" }""")
        Assertions.assertFalse(called)
    }

    @Test
    fun `SUGGESTION_REQUEST no callback set does not crash`() = handle {
        GameHandler.handle("""{ "type": "SUGGESTION_REQUEST", "payload": { "suggesterID": "p1", "suspect": "s", "room": "r", "weapon": "w", "cheatWindowSeconds": 5 } }""")
        Assertions.assertTrue(true)
    }

    @Test
    fun `CHEAT_RESULT with cheatDetected true calls callback`() = handle {
        at.aau.serg.websocketbrokerdemo.model.ClientState.playerId = "p1"
        var detected = false
        GameHandler.onCheatResult = { cheatDetected, _, _, _ -> detected = cheatDetected }
        GameHandler.handle("""{ "type": "CHEAT_RESULT", "payload": { "cheatDetected": true, "cheatPressed": true, "targetPlayerId": "p1", "cheaters": [] } }""")
        Assertions.assertTrue(detected)
    }

    @Test
    fun `CHEAT_RESULT with cheaters and cards calls callback`() = handle {
        at.aau.serg.websocketbrokerdemo.model.ClientState.playerId = "p1"
        var cheaters = listOf<Pair<String, List<String>>>()
        GameHandler.onCheatResult = { _, c, _, _ -> cheaters = c }
        GameHandler.handle("""{ "type": "CHEAT_RESULT", "payload": { "cheatDetected": true, "cheatPressed": true, "targetPlayerId": "p1", "cheaters": [{"playerId": "p2", "cards": [{"name": "knife"}]}] } }""")
        Assertions.assertEquals("p2", cheaters[0].first)
        Assertions.assertEquals(listOf("knife"), cheaters[0].second)
    }

    @Test
    fun `CHEAT_RESULT with cheater without cards does not crash`() = handle {
        at.aau.serg.websocketbrokerdemo.model.ClientState.playerId = "p1"
        var called = false
        GameHandler.onCheatResult = {
                _, _, _, _ -> called = true }
        GameHandler.handle("""{ "type": "CHEAT_RESULT", "payload": { "cheatDetected": true, "cheatPressed": true, "targetPlayerId": "p1", "cheaters": [{"playerId": "p2"}] } }""")
        Assertions.assertTrue(called)
    }

    @Test
    fun `CHEAT_RESULT with cheatDetected false calls callback`() = handle {
        var detected = true
        GameHandler.onCheatResult = { cheatDetected, _, _, _ -> detected = cheatDetected }
        GameHandler.handle("""{ "type": "CHEAT_RESULT", "payload": { "cheatDetected": false, "cheatPressed": true, "revealedCard": "knife" } }""")
        Assertions.assertFalse(detected)
    }

    @Test
    fun `CHEAT_RESULT with cheatPressed true and nobody cheated reports cheatPressed`() = handle {
        var pressed = false
        GameHandler.onCheatResult = { _, _, _, cheatPressed -> pressed = cheatPressed }
        GameHandler.handle("""{ "type": "CHEAT_RESULT", "payload": { "cheatDetected": false, "cheatPressed": true, "suggesterID": "p1", "revealedCard": {"name": "knife"} } }""")
        Assertions.assertTrue(pressed)
    }

    @Test
    fun `CHEAT_RESULT with cheatPressed false reports cheatPressed false so client shows no message`() = handle {
        var pressed = true
        var detected = true
        GameHandler.onCheatResult = { cheatDetected, _, _, cheatPressed ->
            detected = cheatDetected
            pressed = cheatPressed
        }
        GameHandler.handle("""{ "type": "CHEAT_RESULT", "payload": { "cheatDetected": false, "cheatPressed": false, "suggesterID": "p1" } }""")
        Assertions.assertFalse(detected)
        Assertions.assertFalse(pressed)
    }

    @Test
    fun `CHEAT_RESULT with null payload does not crash`() = handle {
        var called = false
        GameHandler.onCheatResult = { _, _, _, _ -> called = true }
        GameHandler.handle("""{ "type": "CHEAT_RESULT" }""")
        Assertions.assertFalse(called)
    }

    @Test
    fun `CHEAT_RESULT no callback set does not crash`() = handle {
        GameHandler.handle("""{ "type": "CHEAT_RESULT", "payload": { "cheatDetected": false } }""")
        Assertions.assertTrue(true)
    }

    @Test
    fun `GAME_FINISHED calls callback`() = handle {
        var winner = ""
        GameHandler.onGameFinished = {
            w -> winner = w
        }
        GameHandler.handle("""{ "type": "GAME_FINISHED", "payload": { "winner": "p1" } }""")
        Assertions.assertEquals("p1", winner)
    }

    @Test
    fun `GAME_FINISHED with null payload uses empty string`() = handle {
        var winner = "x"
        GameHandler.onGameFinished = { w -> winner = w }
        GameHandler.handle("""{ "type": "GAME_FINISHED" }""")
        Assertions.assertEquals("", winner)
    }

    @Test
    fun `GAME_FINISHED no callback set does not crash`() = handle {
        GameHandler.handle("""{ "type": "GAME_FINISHED", "payload": { "winner": "p1" } }""")
        Assertions.assertTrue(true)
    }

    @Test
    fun `GAME_PAUSED calls callback`() = handle {
        var called = false
        GameHandler.onGamePaused = { _, _ -> called = true }
        GameHandler.handle("""{ "type": "GAME_PAUSED", "payload": { "disconnectedPlayerId": "p1", "countdown": 30 } }""")
        Assertions.assertTrue(called)
    }

    @Test
    fun `GAME_PAUSED with null payload does not crash`() = handle {
        var called = false
        GameHandler.onGamePaused = { _, _ -> called = true }
        GameHandler.handle("""{ "type": "GAME_PAUSED" }""")
        Assertions.assertTrue(called)
    }

    @Test
    fun `CONTINUE_GAME calls callback`() = handle {
        var called = false
        GameHandler.onContinueGame = { rejoinedPlayerId, someBoolean ->
            called = true
        }
        GameHandler.handle("""{ "type": "CONTINUE_GAME", "payload": { "rejoinedPlayerId": "p1" } }""")
        Assertions.assertTrue(called)
    }

    @Test
    fun `GAME_ABORTED calls callback`() = handle {
        var reason = ""
        GameHandler.onGameAborted = {
            r -> reason = r
        }
        GameHandler.handle("""{ "type": "GAME_ABORTED", "payload": { "reason": "player left" } }""")
        Assertions.assertEquals("player left", reason)
    }

    @Test
    fun `GAME_ABORTED with availableCharacters and existingPlayers updates ClientState`() = handle {
        GameHandler.handle("""{ "type": "GAME_ABORTED", "payload": { "reason": "test", "availableCharacters": ["DR_RED"], "existingPlayers": [{"playerId": "p1", "ready": true}] } }""")
        Assertions.assertTrue(at.aau.serg.websocketbrokerdemo.model.ClientState.availableCharacters.contains("DR_RED"))
        Assertions.assertEquals("p1", at.aau.serg.websocketbrokerdemo.model.ClientState.players[0].playerId)
    }

    @Test
    fun `GAME_ABORTED with null payload uses default reason`() = handle {
        var reason = ""
        GameHandler.onGameAborted = {
                r -> reason = r
        }
        GameHandler.handle("""{ "type": "GAME_ABORTED" }""")
        Assertions.assertEquals("Game aborted", reason)
    }

    @Test
    fun `GAME_ABORTED no callback set does not crash`() = handle {
        GameHandler.handle("""{ "type": "GAME_ABORTED", "payload": { "reason": "player left" } }""")
        Assertions.assertTrue(true)
    }

    @Test
    fun `error type calls onGameError callback`() = handle {
        var reason = ""
        GameHandler.onGameError = { r -> reason = r }
        GameHandler.handle("""{ "type": "ROLL_DICE_ERROR", "payload": { "reason": "some error" } }""")
        Assertions.assertEquals("some error", reason)
    }

    @Test
    fun `error type with null payload uses default reason`() = handle {
        var reason = ""
        GameHandler.onGameError = { r -> reason = r }
        GameHandler.handle("""{ "type": "ROLL_DICE_ERROR" }""")
        Assertions.assertEquals("An error occurred", reason)
    }
    @Test
    fun `ROLL_DICE passes playerId to callback`() = handle {
        var playerId = ""
        GameHandler.onRollDice = { id, _, _ ->
            playerId = id
        }
        GameHandler.handle("""{ "type": "ROLL_DICE", "payload": { "playerId": "p1", "value":5 } }""")
        Assertions.assertEquals("p1", playerId)
    }
    @Test
    fun `GAME_ABORTED replaces player ids with existing character names in reason`() = handle {
        var reason = ""

        at.aau.serg.websocketbrokerdemo.model.ClientState.playerCharacterMap.clear()
        at.aau.serg.websocketbrokerdemo.model.ClientState.playerCharacterMap["p1"] = "MRS_LAVENDER"

        at.aau.serg.websocketbrokerdemo.model.ClientState.players = listOf(
            at.aau.serg.websocketbrokerdemo.messaging.dtos.ExistingPlayerDTO(
                playerId = "p2",
                ready = true,
                character = "DR_RED",
                position = null
            )
        )

        GameHandler.onGameAborted = { r ->
            reason = r
        }

        GameHandler.handle(
            """{
            "type": "GAME_ABORTED",
            "payload": {
                "reason": "p1 and p2 left the game"
            }
        }"""
        )

        Assertions.assertEquals("MRS_LAVENDER and DR_RED left the game", reason)
    }
    @Test
    fun `CHEAT_RESULT with existing phase current player index and current player cheater updates state`() = handle {
        var endTurnIndex = -1

        at.aau.serg.websocketbrokerdemo.model.ClientState.playerId = "p2"
        at.aau.serg.websocketbrokerdemo.model.ClientState.cheatUsed = false
        at.aau.serg.websocketbrokerdemo.model.ClientState.currentPhase = "WAITING_FOR_ROLL"
        at.aau.serg.websocketbrokerdemo.model.ClientState.currentPlayerIndex = 0
        at.aau.serg.websocketbrokerdemo.model.ClientState.remainingMoves = 5

        GameHandler.onEndTurn = { index ->
            endTurnIndex = index
        }

        GameHandler.handle(
            """{
            "type": "CHEAT_RESULT",
            "payload": {
                "currentPhase": "WAITING_FOR_MOVE",
                "currentPlayerIndex": 3,
                "cheatDetected": true,
                "targetPlayerId": "other",
                "cheaters": [
                    {
                        "playerId": "p2",
                        "cards": []
                    }
                ]
            }
        }"""
        )

        Assertions.assertEquals(
            "WAITING_FOR_MOVE",
            at.aau.serg.websocketbrokerdemo.model.ClientState.currentPhase
        )
        Assertions.assertEquals(3, at.aau.serg.websocketbrokerdemo.model.ClientState.currentPlayerIndex)
        Assertions.assertEquals(0, at.aau.serg.websocketbrokerdemo.model.ClientState.remainingMoves)
        Assertions.assertEquals(3, endTurnIndex)
        Assertions.assertTrue(at.aau.serg.websocketbrokerdemo.model.ClientState.cheatUsed)
    }
}