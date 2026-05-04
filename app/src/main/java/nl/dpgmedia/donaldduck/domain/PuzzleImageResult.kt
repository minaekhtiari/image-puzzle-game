package nl.dpgmedia.donaldduck.domain

import coil3.Bitmap

sealed interface PuzzleImageResult {
    data class Success(val bitmap: Bitmap) : PuzzleImageResult
    data object Failure : PuzzleImageResult
}