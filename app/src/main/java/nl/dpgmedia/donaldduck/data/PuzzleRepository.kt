package nl.dpgmedia.donaldduck.data

import nl.dpgmedia.donaldduck.data.remote.model.EventItem
import nl.dpgmedia.donaldduck.data.remote.model.PuzzleItem

interface PuzzleRepository {
    suspend fun getPuzzle(): PuzzleItem
    suspend fun trackGameFinished(event: EventItem.GameFinished)
}