package nl.dpgmedia.donaldduck.ui

import androidx.compose.ui.graphics.ImageBitmap

data class PuzzleImageUiState(
    val tileBitmaps: List<ImageBitmap> = emptyList(),
    val fullImageBitmap: ImageBitmap? = null,
    val aspectRatio: Float = 1.25f,
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false
)