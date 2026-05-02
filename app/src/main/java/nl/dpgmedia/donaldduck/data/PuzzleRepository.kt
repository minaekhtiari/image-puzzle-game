package nl.dpgmedia.donaldduck.data

import nl.dpgmedia.donaldduck.remote.model.EventItem
import nl.dpgmedia.donaldduck.remote.model.PuzzleItem

interface PuzzleRepository {
    suspend fun getPuzzle(): PuzzleItem
    suspend fun trackGameFinished(event: EventItem.GameFinished)
}