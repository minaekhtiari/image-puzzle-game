package nl.dpgmedia.donaldduck.domain

interface PuzzleImageLoader {
    suspend fun load(url: String): PuzzleImageResult
}
