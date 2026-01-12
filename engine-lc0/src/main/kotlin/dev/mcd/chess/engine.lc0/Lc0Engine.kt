package dev.mcd.chess.engine.lc0

import android.content.Context
import androidx.annotation.Keep
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mcd.chess.common.engine.ChessEngine
import dev.mcd.chess.common.engine.EngineCommand
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import java.util.zip.GZIPInputStream


internal class Lc0Engine @Inject constructor(
    private val bridge: Lc0Jni,
    @ApplicationContext
    private val context: Context,
    private val engineContext: CoroutineContext,
) : ChessEngine<MaiaWeights, FenParam> {

    private val stateFlow = MutableStateFlow<State>(State.Uninitialized)
    private lateinit var weightsFile: File

    override fun init(params: MaiaWeights) {
        val assetPbPath = params.asset
        Timber.tag("Lc0").d("Requested weights asset: $assetPbPath")

        val outName = assetPbPath.substringAfterLast('/').removeSuffix(".gz")
        val outFileName = if (outName.endsWith(".pb")) outName else "$outName.pb"

        val outDir = File(context.filesDir, "weights")
        weightsFile = File(outDir, outFileName)

        Timber.tag("Lc0").d("Weights output file: ${weightsFile.absolutePath}")

        if (!weightsFile.exists() || weightsFile.length() == 0L) {
            outDir.mkdirs()

            val pbExists = assetExists(assetPbPath)
            val gzPath = if (assetPbPath.endsWith(".gz")) assetPbPath else "$assetPbPath.gz"
            val gzExists = assetExists(gzPath)

            Timber.tag("Lc0").d("Asset exists? pb=$pbExists gz=$gzExists (gzPath=$gzPath)")

            try {
                if (pbExists) {
                    context.assets.open(assetPbPath).use { input ->
                        weightsFile.outputStream().use { output -> input.copyTo(output) }
                    }
                } else {
                    context.assets.open(gzPath).use { raw ->
                        GZIPInputStream(raw).use { gzIn ->
                            weightsFile.outputStream().use { output -> gzIn.copyTo(output) }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.tag("Lc0").e(e, "Failed copying weights from assets")
                throw e
            }

            Timber.tag("Lc0").d("Weights copied. exists=${weightsFile.exists()} size=${weightsFile.length()}")
        } else {
            Timber.tag("Lc0").d("Weights already present. size=${weightsFile.length()}")
        }

        bridge.init()
    }
    private fun dumpWeightsAssets() {
        try {
            val list = context.assets.list("weights")?.toList().orEmpty()
            Timber.tag("Lc0").d("Assets/weights contains: $list")
        } catch (e: Exception) {
            Timber.tag("Lc0").e(e, "Failed listing assets/weights")
        }
    }


    private fun assetExists(path: String): Boolean =
        try {
            context.assets.open(path).close()
            true
        } catch (_: Exception) {
            false
        }


    override suspend fun awaitReady() {
        awaitState<State.Ready>()
    }

    override suspend fun startAndWait() {
        awaitState<State.Uninitialized>()
        CoroutineScope(coroutineContext).launch {
            launch(engineContext) {
                bridge.main(weightsFile.absolutePath)
            }

            launch(engineContext) {
                while (isActive) {
                    val output = bridge.readLine() ?: continue
                    if (output.startsWith(BEST_MOVE_TOKEN)) {
                        // 0:bestmove 1:[e2e4] 2:ponder 3:a6a7
                        val move = output.split(" ")[1].trim()
                        assertStateOrNull<State.Moving>()?.completable?.complete(move)
                    }
                }
            }
            moveToState(State.Ready)
        }.let { job ->
            try {
                awaitCancellation()
            } finally {
                job.cancel()
                moveToState(State.Uninitialized)
            }
        }
    }

    override suspend fun getMove(params: FenParam): String {
        return withContext(engineContext) {
            awaitState<State.Ready>()
            val moveCompletable = CompletableDeferred<String>()
            moveToState(State.Moving(moveCompletable))

            bridge.writeLine(EngineCommand.SetPosition(params.fen).toString())
            bridge.writeLine(EngineCommand.GoNodes.toString())

            moveCompletable.await().also {
                moveToState(State.Ready)
            }
        }
    }

    private inline fun <reified T : State> assertStateOrNull(): T? {
        return stateFlow.value as? T
    }

    private suspend inline fun <reified T : State> awaitState() {
        Timber.tag("Lc0").d("Awaiting ${T::class.simpleName}")
        stateFlow.takeWhile { it !is T }.collect()
    }

    private suspend fun moveToState(state: State) {
        stateFlow.emit(state)
        Timber.tag("Lc0").d("Moved to ${state::class.simpleName}")
    }

    private sealed interface State {
        @Keep
        object Uninitialized : State

        @Keep
        class Moving(
            val completable: CompletableDeferred<String>,
        ) : State

        @Keep
        object Ready : State
    }

    companion object {
        internal const val BEST_MOVE_TOKEN = "bestmove"
    }
}
