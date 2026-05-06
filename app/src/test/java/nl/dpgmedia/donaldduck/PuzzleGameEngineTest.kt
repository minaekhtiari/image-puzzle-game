package nl.dpgmedia.donaldduck

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import nl.dpgmedia.donaldduck.data.remote.model.PuzzlePiece
import nl.dpgmedia.donaldduck.domain.PuzzleGameEngine
import org.junit.Test

class PuzzleGameEngineTest {

    private val engine = PuzzleGameEngine()

    @Test
    fun createShuffledPieces_createsExpectedPieces() {
        val pieces = engine.createShuffledPieces(tileCount = 8)

        assertEquals(8, pieces.size)
        assertEquals((0..7).toList(), pieces.map { it.correctIndex }.sorted())
        assertEquals((0..7).toList(), pieces.map { it.currentIndex }.sorted())
    }

    @Test
    fun isSolved_returnsTrue_whenAllPiecesAreCorrect() {
        val pieces = List(8) { index ->
            PuzzlePiece(id = index, correctIndex = index, currentIndex = index)
        }

        assertTrue(engine.isSolved(pieces))
    }

    @Test
    fun isSolved_returnsFalse_whenPiecesAreNotCorrect() {
        val pieces = listOf(
            PuzzlePiece(id = 0, correctIndex = 0, currentIndex = 1),
            PuzzlePiece(id = 1, correctIndex = 1, currentIndex = 0)
        )

        assertFalse(engine.isSolved(pieces))
    }

    @Test
    fun findConnectedGroupRange_returnsConnectedGroupAroundIndex() {
        val pieces = listOf(
            PuzzlePiece(id = 5, correctIndex = 5, currentIndex = 0),
            PuzzlePiece(id = 2, correctIndex = 2, currentIndex = 1),
            PuzzlePiece(id = 3, correctIndex = 3, currentIndex = 2),
            PuzzlePiece(id = 4, correctIndex = 4, currentIndex = 3),
            PuzzlePiece(id = 0, correctIndex = 0, currentIndex = 4)
        )

        val result = engine.findConnectedGroupRange(pieces, index = 2)

        assertEquals(1..3, result)
    }

    @Test
    fun findConnectedGroupRange_returnsSingleIndex_whenTileIsNotConnected() {
        val pieces = listOf(
            PuzzlePiece(id = 0, correctIndex = 0, currentIndex = 0),
            PuzzlePiece(id = 3, correctIndex = 3, currentIndex = 1),
            PuzzlePiece(id = 1, correctIndex = 1, currentIndex = 2)
        )

        val result = engine.findConnectedGroupRange(pieces, index = 1)

        assertEquals(1..1, result)
    }

    @Test
    fun swapGroups_swapsConnectedGroupWithTargetGroup() {
        val pieces = listOf(
            PuzzlePiece(id = 0, correctIndex = 0, currentIndex = 0),
            PuzzlePiece(id = 2, correctIndex = 2, currentIndex = 1),
            PuzzlePiece(id = 3, correctIndex = 3, currentIndex = 2),
            PuzzlePiece(id = 6, correctIndex = 6, currentIndex = 3),
            PuzzlePiece(id = 7, correctIndex = 7, currentIndex = 4)
        )

        val result = engine.swapGroups(
            pieces = pieces,
            fromGroupStart = 1,
            toGroupStart = 3
        )

        val order = result.sortedBy { it.currentIndex }.map { it.correctIndex }

        assertEquals(listOf(0, 6, 7, 2, 3), order)
    }

    @Test
    fun swapGroups_doesNothing_whenTargetOverlapsSourceGroup() {
        val pieces = listOf(
            PuzzlePiece(id = 1, correctIndex = 1, currentIndex = 0),
            PuzzlePiece(id = 2, correctIndex = 2, currentIndex = 1),
            PuzzlePiece(id = 3, correctIndex = 3, currentIndex = 2)
        )

        val result = engine.swapGroups(
            pieces = pieces,
            fromGroupStart = 0,
            toGroupStart = 1
        )

        assertEquals(pieces, result)
    }
}