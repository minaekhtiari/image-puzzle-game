package nl.dpgmedia.donaldduck.domain

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap

interface PuzzleImageSlicer {
    fun sliceHorizontally(bitmap: Bitmap, tileCount: Int = PuzzleGameEngine.TILE_COUNT): List<ImageBitmap>
}
