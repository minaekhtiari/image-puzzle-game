package nl.dpgmedia.donaldduck.data

import nl.dpgmedia.donaldduck.remote.PuzzleService
import nl.dpgmedia.donaldduck.remote.model.EventItem
import nl.dpgmedia.donaldduck.remote.model.PuzzleItem
import javax.inject.Inject

 class PuzzleRepositoryImpl @Inject constructor(
    private val puzzleService: PuzzleService
) : PuzzleRepository {

     override suspend fun getPuzzle(): PuzzleItem {
         val response = puzzleService.getPuzzle()
         if (!response.isSuccessful) {
             throw Exception("Failed to load puzzle")
         }
         return response.body() ?: throw Exception("Empty response")
     }

     override suspend fun trackGameFinished(event: EventItem.GameFinished) {
        puzzleService.putEvent(event)
    }
}