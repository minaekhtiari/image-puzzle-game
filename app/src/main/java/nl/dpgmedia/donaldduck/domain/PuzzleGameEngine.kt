package nl.dpgmedia.donaldduck.domain

import nl.dpgmedia.donaldduck.data.remote.model.PuzzlePiece
import javax.inject.Inject
import kotlin.random.Random

private fun IntRange.overlaps(other: IntRange) = first <= other.last && other.first <= last

class PuzzleGameEngine @Inject constructor() {

    // Two-phase: build a solved list first, then shuffle and re-assign currentIndex.
    // Keeping correctIndex stable means isSolved() is always a simple equality check.
    fun createShuffledPieces(
        tileCount: Int = TILE_COUNT,
        random: Random = Random.Default
    ): List<PuzzlePiece> {
        val solvedPieces = List(tileCount) { index ->
            PuzzlePiece(
                id = index,
                correctIndex = index,
                currentIndex = index
            )
        }

        return solvedPieces
            .shuffled(random)
            .mapIndexed { currentIndex, piece ->
                piece.copy(currentIndex = currentIndex)
            }
    }

    fun swapGroups(
        pieces: List<PuzzlePiece>,
        fromGroupStart: Int,
        toGroupStart: Int
    ): List<PuzzlePiece> {
        if (fromGroupStart == toGroupStart) return pieces

        val orderedPieces = pieces.sortedBy { it.currentIndex }

        if (fromGroupStart !in orderedPieces.indices) return pieces
        if (toGroupStart !in orderedPieces.indices) return pieces

        val groupRange = findConnectedGroupRange(orderedPieces, fromGroupStart)
        val groupSize = groupRange.last - groupRange.first + 1

        if (toGroupStart + groupSize > orderedPieces.size) return pieces

        val targetRange = toGroupStart..<toGroupStart + groupSize

        if (groupRange.overlaps(targetRange)) return pieces

        val result = orderedPieces.toMutableList()

        for (i in 0 until groupSize) {
            val srcSlot = groupRange.first + i
            val dstSlot = toGroupStart + i
            result[srcSlot] = orderedPieces[srcSlot].copy(currentIndex = dstSlot)
            result[dstSlot] = orderedPieces[dstSlot].copy(currentIndex = srcSlot)
        }

        return result
    }

    // Walks outward from [index] in both directions, expanding the range as long as
    // adjacent pieces have consecutive correctIndex values (i.e. they belong together
    // in the solved image). Returns the widest contiguous run that includes [index].
    fun findConnectedGroupRange(
        pieces: List<PuzzlePiece>,
        index: Int
    ): IntRange {
        val orderedPieces = pieces.sortedBy { it.currentIndex }

        if (index !in orderedPieces.indices) return index..index

        var start = index
        var end = index

        while (start > 0) {
            val previous = orderedPieces[start - 1]
            val current = orderedPieces[start]

            val isConnectedToPrevious =
                previous.correctIndex + 1 == current.correctIndex

            if (isConnectedToPrevious) {
                start--
            } else {
                break
            }
        }

        while (end < orderedPieces.lastIndex) {
            val current = orderedPieces[end]
            val next = orderedPieces[end + 1]

            val isConnectedToNext =
                current.correctIndex + 1 == next.correctIndex

            if (isConnectedToNext) {
                end++
            } else {
                break
            }
        }

        return start..end
    }

    fun isSolved(pieces: List<PuzzlePiece>): Boolean {
        return pieces.all { piece ->
            piece.currentIndex == piece.correctIndex
        }
    }

    companion object {
        const val TILE_COUNT = 8
    }
}