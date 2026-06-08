package at.aau.serg.websocketbrokerdemo.model

import androidx.core.graphics.toColorInt

object BoardColors {
    val CHARACTER_COLORS: Map<String, Int> by lazy {
        mapOf(
            "MRS_LAVENDER" to "#CEA8F0".toColorInt(),
            "MRS_PINK" to "#FF99D8".toColorInt(),
            "DR_RED" to "#FF5050".toColorInt(),
            "DR_BLUE" to "#99ACFF".toColorInt()
        )
    }
}