package nl.dpgmedia.donaldduck.domain


import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object PuzzleImageSlicer {

    fun sliceHorizontally(
        bitmap: Bitmap,
        tileCount: Int = PuzzleGameEngine.TILE_COUNT
    ): List<ImageBitmap> {
        val tileHeight = bitmap.height / tileCount

        return List(tileCount) { index ->
            val y = index * tileHeight
            val height = if (index == tileCount - 1) {
                bitmap.height - y
            } else {
                tileHeight
            }

            Bitmap.createBitmap(
                bitmap,
                0,
                y,
                bitmap.width,
                height
            ).asImageBitmap()
        }
    }
}