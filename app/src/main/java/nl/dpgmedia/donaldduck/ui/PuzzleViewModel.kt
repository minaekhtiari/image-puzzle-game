package nl.dpgmedia.donaldduck.ui

import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.dpgmedia.donaldduck.data.PuzzleRepository
import nl.dpgmedia.donaldduck.domain.PuzzleGameEngine
import nl.dpgmedia.donaldduck.domain.PuzzleImageLoader
import nl.dpgmedia.donaldduck.domain.PuzzleImageResult
import nl.dpgmedia.donaldduck.domain.PuzzleImageSlicer
import nl.dpgmedia.donaldduck.data.remote.model.EventItem
import javax.inject.Inject

@HiltViewModel
class PuzzleViewModel @Inject constructor(
    private val repository: PuzzleRepository,
    private val gameEngine: PuzzleGameEngine,
    private val imageLoader: PuzzleImageLoader
) : ViewModel() {

    private val _uiState = MutableStateFlow<PuzzleUiState>(PuzzleUiState.Loading)
    val uiState: StateFlow<PuzzleUiState> = _uiState.asStateFlow()

    private val _imageState = MutableStateFlow(PuzzleImageUiState())
    val imageState: StateFlow<PuzzleImageUiState> = _imageState.asStateFlow()

    private var startedAtMs: Long = 0L
    private var eventAlreadySent = false

    init {
        loadPuzzle()
    }

    fun loadPuzzle() {
        viewModelScope.launch {
            _uiState.value = PuzzleUiState.Loading

            runCatching {
                repository.getPuzzle()
            }.onSuccess { puzzle ->
                startedAtMs = System.currentTimeMillis()
                eventAlreadySent = false

                _uiState.value = PuzzleUiState.Playing(
                    puzzleId = puzzle.id,
                    title = puzzle.title,
                    description = puzzle.description,
                    author = puzzle.author,
                    imageUrl = puzzle.thumbnail,
                    pieces = gameEngine.createShuffledPieces()
                )

                loadImage(puzzle.thumbnail)
            }.onFailure { throwable ->
                _uiState.value = PuzzleUiState.Error(
                    message = throwable.message ?: "Something went wrong"
                )
            }
        }
    }

    private fun loadImage(url: String) {
        viewModelScope.launch {
            _imageState.value = PuzzleImageUiState(isLoading = true)
            _imageState.value = when (val result = imageLoader.load(url)) {
                is PuzzleImageResult.Success -> PuzzleImageUiState(
                    tileBitmaps = PuzzleImageSlicer.sliceHorizontally(result.bitmap),
                    fullImageBitmap = result.bitmap.asImageBitmap(),
                    aspectRatio = result.bitmap.height.toFloat() / result.bitmap.width.toFloat()
                )
                PuzzleImageResult.Failure -> PuzzleImageUiState(loadFailed = true)
            }
        }
    }

    fun movePiece(fromIndex: Int, toIndex: Int) {
        val currentState = _uiState.value as? PuzzleUiState.Playing ?: return
        if (fromIndex !in currentState.pieces.indices) return
        if (toIndex !in currentState.pieces.indices) return

        val updatedPieces = gameEngine.swapGroups(
            pieces = currentState.pieces,
            fromGroupStart = fromIndex,
            toGroupStart = toIndex
        )

        val solved = gameEngine.isSolved(updatedPieces)
        _uiState.value = if (solved) {
            PuzzleUiState.Solved(
                puzzleId = currentState.puzzleId,
                title = currentState.title,
                description = currentState.description,
                author = currentState.author,
                imageUrl = currentState.imageUrl,
                pieces = updatedPieces,
                elapsedTimeInMs = System.currentTimeMillis() - startedAtMs
            )
        } else {
            currentState.copy(pieces = updatedPieces)
        }

        if (solved) {
            trackGameFinished()
        }
    }

    fun retryTrackingEvent() {
        trackGameFinished()
    }

    fun restartGame() {
        val currentState = _uiState.value as? PuzzleUiState.Solved ?: return

        startedAtMs = System.currentTimeMillis()
        eventAlreadySent = false

        _uiState.value = PuzzleUiState.Playing(
            puzzleId = currentState.puzzleId,
            title = currentState.title,
            description = currentState.description,
            author = currentState.author,
            imageUrl = currentState.imageUrl,
            pieces = gameEngine.createShuffledPieces()
        )
    }

    private fun trackGameFinished() {
        val state = _uiState.value as? PuzzleUiState.Solved ?: return
        if (eventAlreadySent) return

        eventAlreadySent = true

        viewModelScope.launch {
            _uiState.update { current ->
                (current as? PuzzleUiState.Solved)?.copy(isTrackingEvent = true, eventTrackingFailed = false) ?: current
            }

            runCatching {
                repository.trackGameFinished(
                    EventItem.GameFinished(
                        puzzleId = state.puzzleId,
                        durationInMs = state.elapsedTimeInMs
                    )
                )
            }.onSuccess {
                _uiState.update { current ->
                    (current as? PuzzleUiState.Solved)?.copy(isTrackingEvent = false, eventTrackingFailed = false) ?: current
                }
            }.onFailure {
                eventAlreadySent = false
                _uiState.update { current ->
                    (current as? PuzzleUiState.Solved)?.copy(isTrackingEvent = false, eventTrackingFailed = true) ?: current
                }
            }
        }
    }

    fun getConnectedGroupRange(index: Int): IntRange {
        val pieces = (_uiState.value as? PuzzleUiState.Playing)?.pieces ?: return index..index
        return gameEngine.findConnectedGroupRange(pieces = pieces, index = index)
    }
}