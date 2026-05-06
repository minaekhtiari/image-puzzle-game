package nl.dpgmedia.donaldduck

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.dpgmedia.donaldduck.data.PuzzleRepository
import nl.dpgmedia.donaldduck.data.remote.model.PuzzleItem
import nl.dpgmedia.donaldduck.data.remote.model.PuzzlePiece
import nl.dpgmedia.donaldduck.domain.PuzzleGameEngine
import nl.dpgmedia.donaldduck.domain.PuzzleImageLoader
import nl.dpgmedia.donaldduck.domain.PuzzleImageResult
import nl.dpgmedia.donaldduck.domain.PuzzleImageSlicer
import nl.dpgmedia.donaldduck.ui.PuzzleImageUiState
import nl.dpgmedia.donaldduck.ui.PuzzleUiState
import nl.dpgmedia.donaldduck.ui.PuzzleViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PuzzleViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val repository: PuzzleRepository = mockk()
    private val gameEngine: PuzzleGameEngine = mockk()
    private val imageLoader: PuzzleImageLoader = mockk()
    private val imageSlicer: PuzzleImageSlicer = mockk()

    // A solved piece list where every piece sits in its correct slot.
    private val solvedPieces = List(8) { i ->
        PuzzlePiece(id = i, correctIndex = i, currentIndex = i)
    }

    // Two pieces swapped so the puzzle is not yet solved.
    private val shuffledPieces = solvedPieces.toMutableList().also {
        it[0] = solvedPieces[0].copy(currentIndex = 1)
        it[1] = solvedPieces[1].copy(currentIndex = 0)
    }

    private val puzzleItem = PuzzleItem(
        id = 42L,
        title = "Test Puzzle",
        description = "A description",
        author = "Author",
        thumbnail = "https://example.com/image.jpg"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { imageLoader.load(any()) } returns PuzzleImageResult.Failure
        every { imageSlicer.sliceHorizontally(any(), any()) } returns emptyList()
        every { gameEngine.createShuffledPieces(any(), any()) } returns shuffledPieces
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun buildViewModel() = PuzzleViewModel(
        repository = repository,
        gameEngine = gameEngine,
        imageLoader = imageLoader,
        imageSlicer = imageSlicer
    )

    private fun TestScope.buildPlayingViewModel(): PuzzleViewModel {
        coEvery { repository.getPuzzle() } returns puzzleItem
        val vm = buildViewModel()
        advanceUntilIdle()
        return vm
    }

    private fun TestScope.buildSolvedViewModel(): PuzzleViewModel {
        every { gameEngine.swapGroups(any(), any(), any()) } returns solvedPieces
        every { gameEngine.isSolved(solvedPieces) } returns true
        every { gameEngine.isSolved(shuffledPieces) } returns false
        coEvery { repository.trackGameFinished(any()) } returns Unit

        val vm = buildPlayingViewModel()
        vm.movePiece(fromIndex = 0, toIndex = 1)
        advanceUntilIdle()
        return vm
    }

    // ---------------------------------------------------------------------------
    // init / loadPuzzle — state transitions
    // ---------------------------------------------------------------------------

    @Test
    fun `init emits Loading then Playing on success`() = runTest {
        coEvery { repository.getPuzzle() } returns puzzleItem
        val vm = buildViewModel()
        val testScope = this

        vm.uiState.test {
            assertEquals(PuzzleUiState.Loading, awaitItem())
            testScope.advanceUntilIdle()
            val playing = awaitItem() as PuzzleUiState.Playing
            assertEquals(puzzleItem.id, playing.puzzleId)
            assertEquals(puzzleItem.title, playing.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init emits Loading then Error on repository failure`() = runTest {
        coEvery { repository.getPuzzle() } throws RuntimeException("network error")
        val vm = buildViewModel()
        val testScope = this

        vm.uiState.test {
            assertEquals(PuzzleUiState.Loading, awaitItem())
            testScope.advanceUntilIdle()
            val error = awaitItem() as PuzzleUiState.Error
            assertEquals("network error", error.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error message falls back to default when exception has no message`() = runTest {
        coEvery { repository.getPuzzle() } throws RuntimeException()
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals("Something went wrong", (vm.uiState.value as PuzzleUiState.Error).message)
    }

    @Test
    fun `loadPuzzle resets imageState to empty at the start of each load`() = runTest {
        coEvery { repository.getPuzzle() } returnsMany listOf(puzzleItem, puzzleItem)
        coEvery { imageLoader.load(any()) } returns PuzzleImageResult.Failure
        val vm = buildViewModel()
        advanceUntilIdle() // first load — imageState ends with loadFailed = true

        vm.imageState.test {
            awaitItem() // stale loadFailed state

            vm.loadPuzzle()

            // imageState must be reset to clean before the new image starts loading
            val reset = awaitItem()
            assertFalse("stale loadFailed still visible after retry", reset.loadFailed)
            assertFalse(reset.isLoading)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---------------------------------------------------------------------------
    // loadImage — imageState transitions
    // ---------------------------------------------------------------------------

    @Test
    fun `imageState transitions isLoading true then loadFailed on image failure`() = runTest {
        coEvery { repository.getPuzzle() } returns puzzleItem
        coEvery { imageLoader.load(any()) } returns PuzzleImageResult.Failure
        val vm = buildViewModel()
        val testScope = this

        vm.imageState.test {
            assertEquals(PuzzleImageUiState(), awaitItem())
            testScope.advanceUntilIdle()
            assertEquals(PuzzleImageUiState(isLoading = true), awaitItem())
            val failed = awaitItem()
            assertTrue(failed.loadFailed)
            assertFalse(failed.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `imageState contains correct aspectRatio on image success`() = runTest {
        val bitmap = mockk<android.graphics.Bitmap>(relaxed = true) {
            every { width } returns 100
            every { height } returns 125
        }
        coEvery { repository.getPuzzle() } returns puzzleItem
        coEvery { imageLoader.load(any()) } returns PuzzleImageResult.Success(bitmap)
        val vm = buildViewModel()
        advanceUntilIdle()

        val state = vm.imageState.value
        assertFalse(state.isLoading)
        assertFalse(state.loadFailed)
        assertEquals(1.25f, state.aspectRatio)
    }

    // ---------------------------------------------------------------------------
    // movePiece
    // ---------------------------------------------------------------------------

    @Test
    fun `movePiece updates pieces in Playing state`() = runTest {
        every { gameEngine.swapGroups(any(), any(), any()) } returns solvedPieces
        every { gameEngine.isSolved(solvedPieces) } returns false
        val vm = buildPlayingViewModel()

        vm.movePiece(fromIndex = 0, toIndex = 1)

        assertEquals(solvedPieces, (vm.uiState.value as PuzzleUiState.Playing).pieces)
    }

    @Test
    fun `movePiece transitions to Solved when isSolved returns true`() = runTest {
        every { gameEngine.swapGroups(any(), any(), any()) } returns solvedPieces
        every { gameEngine.isSolved(solvedPieces) } returns true
        coEvery { repository.trackGameFinished(any()) } returns Unit
        val vm = buildPlayingViewModel()

        vm.movePiece(fromIndex = 0, toIndex = 1)
        advanceUntilIdle()

        assertTrue(vm.uiState.value is PuzzleUiState.Solved)
    }

    @Test
    fun `movePiece is ignored when state is not Playing`() = runTest {
        coEvery { repository.getPuzzle() } throws RuntimeException()
        val vm = buildViewModel()
        advanceUntilIdle()
        val before = vm.uiState.value

        vm.movePiece(fromIndex = 0, toIndex = 1)

        assertEquals(before, vm.uiState.value)
    }

    @Test
    fun `movePiece is ignored when fromIndex is out of bounds`() = runTest {
        val vm = buildPlayingViewModel()
        val before = vm.uiState.value

        vm.movePiece(fromIndex = 999, toIndex = 0)

        assertEquals(before, vm.uiState.value)
    }

    @Test
    fun `movePiece is ignored when toIndex is out of bounds`() = runTest {
        val vm = buildPlayingViewModel()
        val before = vm.uiState.value

        vm.movePiece(fromIndex = 0, toIndex = 999)

        assertEquals(before, vm.uiState.value)
    }

    // ---------------------------------------------------------------------------
    // trackGameFinished (triggered by movePiece solving the puzzle)
    // ---------------------------------------------------------------------------

    @Test
    fun `solving puzzle emits isTrackingEvent true then false on success`() = runTest {
        every { gameEngine.swapGroups(any(), any(), any()) } returns solvedPieces
        every { gameEngine.isSolved(solvedPieces) } returns true
        coEvery { repository.trackGameFinished(any()) } returns Unit
        val vm = buildPlayingViewModel()
        val testScope = this

        vm.uiState.test {
            awaitItem() // Playing

            vm.movePiece(0, 1)
            awaitItem() as PuzzleUiState.Solved // Solved before tracking coroutine runs

            testScope.advanceUntilIdle()
            val tracking = awaitItem() as PuzzleUiState.Solved
            assertTrue(tracking.isTrackingEvent)

            val done = awaitItem() as PuzzleUiState.Solved
            assertFalse(done.isTrackingEvent)
            assertFalse(done.eventTrackingFailed)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tracking failure sets eventTrackingFailed true`() = runTest {
        every { gameEngine.swapGroups(any(), any(), any()) } returns solvedPieces
        every { gameEngine.isSolved(solvedPieces) } returns true
        coEvery { repository.trackGameFinished(any()) } throws RuntimeException("timeout")
        val vm = buildPlayingViewModel()

        vm.movePiece(0, 1)
        advanceUntilIdle()

        val state = vm.uiState.value as PuzzleUiState.Solved
        assertTrue(state.eventTrackingFailed)
        assertFalse(state.isTrackingEvent)
    }

    @Test
    fun `tracking event carries correct puzzleId`() = runTest {
        every { gameEngine.swapGroups(any(), any(), any()) } returns solvedPieces
        every { gameEngine.isSolved(solvedPieces) } returns true
        coEvery { repository.trackGameFinished(any()) } returns Unit
        val vm = buildPlayingViewModel()

        vm.movePiece(0, 1)
        advanceUntilIdle()

        coVerify { repository.trackGameFinished(match { it.puzzleId == puzzleItem.id }) }
    }

    @Test
    fun `duplicate solve does not send tracking event twice`() = runTest {
        every { gameEngine.swapGroups(any(), any(), any()) } returns solvedPieces
        every { gameEngine.isSolved(solvedPieces) } returns true
        coEvery { repository.trackGameFinished(any()) } returns Unit
        val vm = buildPlayingViewModel()

        vm.movePiece(0, 1) // triggers trackGameFinished internally
        vm.retryTrackingEvent() // called while in-progress — should be blocked by guard
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.trackGameFinished(any()) }
    }

    @Test
    fun `retryTrackingEvent resets guard and sends again after failure`() = runTest {
        every { gameEngine.swapGroups(any(), any(), any()) } returns solvedPieces
        every { gameEngine.isSolved(solvedPieces) } returns true
        coEvery { repository.trackGameFinished(any()) } throws RuntimeException() andThen Unit
        val vm = buildPlayingViewModel()

        vm.movePiece(0, 1)
        advanceUntilIdle() // first attempt — fails

        vm.retryTrackingEvent()
        advanceUntilIdle() // retry — succeeds

        val state = vm.uiState.value as PuzzleUiState.Solved
        assertFalse(state.eventTrackingFailed)
        coVerify(exactly = 2) { repository.trackGameFinished(any()) }
    }

    @Test
    fun `retryTrackingEvent is ignored when state is not Solved`() = runTest {
        val vm = buildPlayingViewModel()

        vm.retryTrackingEvent()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.trackGameFinished(any()) }
    }

    // ---------------------------------------------------------------------------
    // restartGame
    // ---------------------------------------------------------------------------

    @Test
    fun `restartGame transitions from Solved back to Playing`() = runTest {
        val vm = buildSolvedViewModel()

        vm.restartGame()

        assertTrue(vm.uiState.value is PuzzleUiState.Playing)
    }

    @Test
    fun `restartGame preserves puzzle metadata`() = runTest {
        val vm = buildSolvedViewModel()
        vm.restartGame()

        val playing = vm.uiState.value as PuzzleUiState.Playing
        assertEquals(puzzleItem.id, playing.puzzleId)
        assertEquals(puzzleItem.title, playing.title)
        assertEquals(puzzleItem.author, playing.author)
        assertEquals(puzzleItem.description, playing.description)
        assertEquals(puzzleItem.thumbnail, playing.imageUrl)
    }

    @Test
    fun `restartGame creates fresh shuffled pieces`() = runTest {
        val vm = buildSolvedViewModel()
        vm.restartGame()

        assertEquals(shuffledPieces, (vm.uiState.value as PuzzleUiState.Playing).pieces)
    }

    @Test
    fun `restartGame is ignored when state is not Solved`() = runTest {
        val vm = buildPlayingViewModel()
        val before = vm.uiState.value

        vm.restartGame()

        assertEquals(before, vm.uiState.value)
    }

    // ---------------------------------------------------------------------------
    // getConnectedGroupRange
    // ---------------------------------------------------------------------------

    @Test
    fun `getConnectedGroupRange delegates to gameEngine when Playing`() = runTest {
        every { gameEngine.findConnectedGroupRange(any(), eq(3)) } returns 2..4
        val vm = buildPlayingViewModel()

        assertEquals(2..4, vm.getConnectedGroupRange(3))
    }

    @Test
    fun `getConnectedGroupRange returns single-element range when not Playing`() = runTest {
        coEvery { repository.getPuzzle() } throws RuntimeException()
        val vm = buildViewModel()
        advanceUntilIdle()

        assertEquals(5..5, vm.getConnectedGroupRange(5))
    }
}
