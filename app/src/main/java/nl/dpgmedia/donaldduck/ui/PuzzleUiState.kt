package nl.dpgmedia.donaldduck.ui

import nl.dpgmedia.donaldduck.data.remote.model.PuzzlePiece

sealed class PuzzleUiState {

    data object Loading : PuzzleUiState()

    data class Error(val message: String) : PuzzleUiState()

    data class Playing(
        val puzzleId: Long,
        val title: String,
        val description: String,
        val author: String,
        val imageUrl: String,
        val pieces: List<PuzzlePiece>
    ) : PuzzleUiState()

    data class Solved(
        val puzzleId: Long,
        val title: String,
        val description: String,
        val author: String,
        val imageUrl: String,
        val pieces: List<PuzzlePiece>,
        val elapsedTimeInMs: Long,
        val isTrackingEvent: Boolean = false,
        val eventTrackingFailed: Boolean = false
    ) : PuzzleUiState()
}