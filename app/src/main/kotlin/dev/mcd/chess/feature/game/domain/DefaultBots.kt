package dev.mcd.chess.feature.game.domain

import dev.mcd.chess.common.player.Bot
import dev.mcd.chess.common.player.PlayerImage
import dev.mcd.chess.engine.lc0.MaiaWeights

object DefaultBots {
    fun bots() = listOf(
        Bot(
            slug = MaiaWeights.ELO_1100.name,
            name = "Novato (1100)",
            image = PlayerImage.Bot,
        ),
        Bot(
            slug = MaiaWeights.ELO_1200.name,
            name = "Principiante (1300)",
            image = PlayerImage.Bot,
        ),
        Bot(
            slug = MaiaWeights.ELO_1300.name,
            name = "Intermedio (1300)",
            image = PlayerImage.Bot,
        ),
        Bot(
            slug = MaiaWeights.ELO_1400.name,
            name = "Avanzado (1400)",
            image = PlayerImage.Bot,
        ),
        Bot(
            slug = MaiaWeights.ELO_1900.name,
            name = "Maestro (1900)",
            image = PlayerImage.Bot,
        ),
    )

    fun fromSlug(slug: String) = bots().first { it.slug == slug }
}
