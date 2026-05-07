package nl.dpgmedia.donaldduck

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.dpgmedia.donaldduck.ui.PuzzleImageUiState
import nl.dpgmedia.donaldduck.ui.PuzzleScreen
import nl.dpgmedia.donaldduck.ui.PuzzleUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PuzzleScreenTest {
//when the state is Loading, the screen renders the loading text without crashing
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun loadingState_showsProgressIndicator() {
        lateinit var loadingText: String

        composeTestRule.setContent {
            MaterialTheme {
                loadingText = stringResource(R.string.puzzle_loading)
                PuzzleScreen(
                    uiState = PuzzleUiState.Loading,
                    imageState = PuzzleImageUiState(),
                    onMovePiece = { _, _ -> },
                    onRetry = {},
                    onRestart = {},
                    onRetryTrackingEvent = {},
                    getConnectedGroupRange = { IntRange.EMPTY }
                )
            }
        }

        composeTestRule
            .onNodeWithText(loadingText)
            .assertIsDisplayed()
    }
}