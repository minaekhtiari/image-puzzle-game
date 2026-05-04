package nl.dpgmedia.donaldduck.domain

import android.content.Context
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Singleton
import javax.inject.Inject
@Singleton
class CoilPuzzleImageLoader @Inject constructor(
    private val imageLoader: ImageLoader,
    @param:ApplicationContext private val context: Context
) : PuzzleImageLoader {
//    private val imageLoader by lazy { ImageLoader(context) }
    override suspend fun load(url: String): PuzzleImageResult {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        val result = imageLoader.execute(request)
        return if (result is SuccessResult) {
            PuzzleImageResult.Success(result.image.toBitmap())
        } else {
            PuzzleImageResult.Failure
        }
    }
}
