package dev.mcd.chess.online.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mcd.chess.online.domain.entity.Puzzle
import dev.mcd.chess.online.domain.usecase.GetRandomPuzzle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

class OfflineGetRandomPuzzle @Inject constructor(
    @ApplicationContext private val context: Context,
) : GetRandomPuzzle {

    // Cache simple en memoria
    @Volatile private var cached: List<Puzzle>? = null

    override suspend fun invoke(ratingRange: IntRange): Puzzle = withContext(Dispatchers.IO) {
        val puzzles = cached ?: loadFromAssets().also { cached = it }

        val candidates = puzzles.filter { it.rating in ratingRange }
        if (candidates.isEmpty()) {
            error("No hay puzzles en el rango $ratingRange. Total cargados: ${puzzles.size}")
        }
        candidates[Random.nextInt(candidates.size)]
    }

    private fun loadFromAssets(): List<Puzzle> {
        // Pon aquí la ruta exacta dentro de assets
        // Ejemplo: app/src/main/assets/puzzles/puzzles.csv
        val assetPath = "puzzles/puzzles.csv"

        context.assets.open(assetPath).bufferedReader().useLines { lines ->
            return lines
                .drop(1) // header
                .mapNotNull { parsePuzzleLine(it) }
                .toList()
        }
    }

    private fun parsePuzzleLine(line: String): Puzzle? {
        // IMPORTANTE: ajusta esta parte al formato REAL de tu CSV “bueno”.
        // Asumo: PuzzleId,FEN,Moves,Rating,...,Themes,...
        // y que Moves viene separado por espacios, Themes separado por espacios.
        val cols = line.split(',')
        if (cols.size < 8) return null

        val puzzleId = cols[0].trim().trim('"')
        val fen = cols[1].trim().trim('"')
        val movesStr = cols[2].trim().trim('"')
        val rating = cols[3].trim().trim('"').toIntOrNull() ?: return null

        // Muchas veces Themes está más adelante; ajusta el índice si hace falta
        val themesStr = cols[7].trim().trim('"')

        val moves = movesStr.split(' ').filter { it.isNotBlank() }
        val themes = themesStr.split(' ').filter { it.isNotBlank() }

        return Puzzle(
            puzzleId = puzzleId,
            fen = fen,
            moves = moves,
            rating = rating,
            themes = themes,
        )
    }
}
