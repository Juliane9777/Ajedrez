package dev.mcd.chess.feature.common.domain

enum class AppColorScheme {
    Blue, Brown, Green, Gray;

    companion object {
        fun default(): AppColorScheme = Blue
    }
}
