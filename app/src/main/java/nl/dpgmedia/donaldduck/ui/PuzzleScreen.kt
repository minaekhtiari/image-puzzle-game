package nl.dpgmedia.donaldduck.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import nl.dpgmedia.donaldduck.R
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.dpgmedia.donaldduck.domain.PuzzleGameEngine
import kotlin.math.roundToInt


@Composable
fun PuzzleRoute(
    viewModel: PuzzleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val imageState by viewModel.imageState.collectAsStateWithLifecycle()

    PuzzleScreen(
        uiState = uiState,
        imageState = imageState,
        onMovePiece = viewModel::movePiece,
        onRetry = viewModel::loadPuzzle,
        onRestart = viewModel::restartGame,
        onRetryTrackingEvent = viewModel::retryTrackingEvent,
        getConnectedGroupRange = viewModel::getConnectedGroupRange
    )
}

@Composable
fun PuzzleScreen(
    uiState: PuzzleUiState,
    imageState: PuzzleImageUiState,
    onMovePiece: (fromIndex: Int, toIndex: Int) -> Unit,
    onRetry: () -> Unit,
    onRestart: () -> Unit,
    onRetryTrackingEvent: () -> Unit,
    getConnectedGroupRange: (index: Int) -> IntRange
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (uiState) {
            is PuzzleUiState.Loading -> LoadingContent()
            is PuzzleUiState.Error -> ErrorContent(
                message = uiState.message,
                onRetry = onRetry
            )
            is PuzzleUiState.Playing -> PuzzleContent(
                title = uiState.title,
                author = uiState.author,
                description = uiState.description,
                imageState = imageState,
                pieces = uiState.pieces,
                isSolved = false,
                elapsedTimeInMs = 0L,
                isTrackingEvent = false,
                eventTrackingFailed = false,
                onMovePiece = onMovePiece,
                onRestart = onRestart,
                onRetryTrackingEvent = onRetryTrackingEvent,
                getConnectedGroupRange = getConnectedGroupRange
            )
            is PuzzleUiState.Solved -> PuzzleContent(
                title = uiState.title,
                author = uiState.author,
                description = uiState.description,
                imageState = imageState,
                pieces = uiState.pieces,
                isSolved = true,
                elapsedTimeInMs = uiState.elapsedTimeInMs,
                isTrackingEvent = uiState.isTrackingEvent,
                eventTrackingFailed = uiState.eventTrackingFailed,
                onMovePiece = onMovePiece,
                onRestart = onRestart,
                onRetryTrackingEvent = onRetryTrackingEvent,
                getConnectedGroupRange = getConnectedGroupRange
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
        Text(
            modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_80)),
            text = stringResource(R.string.puzzle_loading),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.spacing_xxl)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )

        Button(
            modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_lg)),
            onClick = onRetry
        ) {
            Text(stringResource(R.string.puzzle_error_retry))
        }
    }
}

@Composable
private fun PuzzleContent(
    title: String,
    author: String,
    description: String,
    imageState: PuzzleImageUiState,
    pieces: List<PuzzlePieceUi>,
    isSolved: Boolean,
    elapsedTimeInMs: Long,
    isTrackingEvent: Boolean,
    eventTrackingFailed: Boolean,
    onMovePiece: (fromIndex: Int, toIndex: Int) -> Unit,
    onRestart: () -> Unit,
    onRetryTrackingEvent: () -> Unit,
    getConnectedGroupRange: (index: Int) -> IntRange
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(dimensionResource(R.dimen.spacing_lg))
    ) {
        Text(
            modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_xl)),
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )

        if (author.isNotBlank()) {
            Text(
                modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_xs)),
                text = stringResource(R.string.puzzle_author_prefix, author),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (description.isNotBlank()) {
            Text(
                modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_sm)),
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
        }

        PuzzleBoard(
            modifier = Modifier
                .padding(top = dimensionResource(R.dimen.spacing_xl))
                .fillMaxWidth(),
            imageState = imageState,
            pieces = pieces,
            enabled = !isSolved,
            isSolved = isSolved,
            getConnectedGroupRange = getConnectedGroupRange,
            onMovePiece = onMovePiece
        )

        if (isSolved) {
            Text(
                modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_xl)),
                text = stringResource(R.string.puzzle_solved_message,
                    formatDuration(elapsedTimeInMs)),
                style = MaterialTheme.typography.titleMedium
            )
            if (isTrackingEvent) {
                Text(
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_sm)),
                    text = stringResource(R.string.puzzle_saving_result),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (eventTrackingFailed) {
                Text(
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_sm)),
                    text = stringResource(R.string.puzzle_save_failed),
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_sm)),
                    onClick = onRetryTrackingEvent
                ) {
                    Text(stringResource(R.string.puzzle_retry_save))
                }
            }

            Button(
                modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_md)),
                onClick = onRestart
            ) {
                Text(stringResource(R.string.puzzle_play_again))
            }
        }
    }
}

@Composable
private fun PuzzleBoard(
    modifier: Modifier = Modifier,
    imageState: PuzzleImageUiState,
    pieces: List<PuzzlePieceUi>,
    enabled: Boolean,
    isSolved: Boolean,
    getConnectedGroupRange: (index: Int) -> IntRange,
    onMovePiece: (fromGroupStart: Int, toGroupStart: Int) -> Unit
) {
    var draggedGroupRange by remember { mutableStateOf<IntRange?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val boardMaxWidth = dimensionResource(R.dimen.puzzle_board_max_width)
    val minTileHeight = dimensionResource(R.dimen.puzzle_tile_min_height)
    val cardCornerRadius = dimensionResource(R.dimen.card_corner_radius)
    val cardElevation = dimensionResource(R.dimen.card_elevation)
    val dragShadowElevation = dimensionResource(R.dimen.drag_shadow_elevation).value
    val hoverShadowElevation = dimensionResource(R.dimen.hover_shadow_elevation).value

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = boardMaxWidth)
    ) {
        val boardWidth = maxWidth
        val minBoardHeight = minTileHeight * PuzzleGameEngine.TILE_COUNT
        val calculatedBoardHeight = boardWidth * imageState.aspectRatio
        val boardHeight = if (calculatedBoardHeight < minBoardHeight) minBoardHeight else calculatedBoardHeight
        val tileHeight = boardHeight / PuzzleGameEngine.TILE_COUNT
        val tileHeightPx = with(LocalDensity.current) { tileHeight.toPx() }

        val dragState: Pair<IntRange, IntRange>? = draggedGroupRange?.let { range ->
            val groupSize = range.last - range.first + 1
            val targetStart = (range.first + (dragOffsetY / tileHeightPx).roundToInt())
                .coerceIn(0, pieces.size - groupSize)
            val targetRange = targetStart..<targetStart + groupSize
            val overlaps = range.first <= targetRange.last && targetRange.first <= range.last
            range to if (overlaps) range else targetRange
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(cardCornerRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(boardHeight)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                when {
                    imageState.isLoading -> CircularProgressIndicator()
                    imageState.loadFailed -> Text(
                        text = stringResource(R.string.puzzle_image_load_failed),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    else -> {
                        if (isSolved && imageState.fullImageBitmap != null) {
                            Image(
                                bitmap = imageState.fullImageBitmap,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                            return@Box
                        }

                        val orderedPieces = pieces.sortedBy { it.currentIndex }

                        orderedPieces.forEachIndexed { index, piece ->
                            val bitmap = imageState.tileBitmaps.getOrNull(piece.correctIndex)
                                ?: return@forEachIndexed

                            val isInDraggedGroup = dragState?.first?.contains(index) == true

                            val isInHoveredGroup = dragState != null
                                && dragState.second != dragState.first
                                && dragState.second.contains(index)

                            val offsetY = when {
                                isInDraggedGroup ->
                                    (index * tileHeightPx + dragOffsetY).roundToInt()
                                dragState != null && isInHoveredGroup -> {
                                    val (draggedRange, hoveredRange) = dragState
                                    val posInGroup = index - hoveredRange.first
                                    ((draggedRange.first + posInGroup) * tileHeightPx).roundToInt()
                                }
                                else -> (index * tileHeightPx).roundToInt()
                            }

                            PuzzleTile(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .fillMaxWidth()
                                    .height(tileHeight)
                                    .offset { IntOffset(x = 0, y = offsetY) }
                                    .zIndex(when {
                                        isInDraggedGroup -> 10f
                                        isInHoveredGroup -> 5f
                                        else -> 0f
                                    })
                                    .graphicsLayer {
                                        shadowElevation = when {
                                            isInDraggedGroup -> dragShadowElevation
                                            isInHoveredGroup -> hoverShadowElevation
                                            else -> 0f
                                        }
                                    }
                                    .scale(if (isInDraggedGroup) 1.03f else 1f)
                                    .pointerInput(enabled, pieces) {
                                        if (!enabled) return@pointerInput

                                        detectDragGestures(
                                            onDragStart = {
                                                draggedGroupRange = getConnectedGroupRange(index)
                                                dragOffsetY = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                            },
                                            onDragCancel = {
                                                draggedGroupRange = null
                                                dragOffsetY = 0f
                                            },
                                            onDragEnd = {
                                                val range = draggedGroupRange
                                                if (range != null) {
                                                    val groupSize = range.last - range.first + 1
                                                    val targetStart = (range.first + (dragOffsetY / tileHeightPx).roundToInt())
                                                        .coerceIn(0, pieces.size - groupSize)
                                                 //   val targetRange = targetStart..(targetStart + groupSize - 1)
                                                    val targetRange = targetStart ..< targetStart + groupSize
                                                    val overlaps = range.first <= targetRange.last && targetRange.first <= range.last

                                                    if (!overlaps && targetStart != range.first) {
                                                        onMovePiece(range.first, targetStart)
                                                    }
                                                }
                                                draggedGroupRange = null
                                                dragOffsetY = 0f
                                            }
                                        )
                                    },
                                bitmap = bitmap,
                                isConnectedToPrevious = isConnectedToPrevious(orderedPieces, index),
                                isConnectedToNext = isConnectedToNext(orderedPieces, index)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PuzzleTile(
    modifier: Modifier = Modifier,
    bitmap: ImageBitmap,
    isConnectedToPrevious: Boolean,
    isConnectedToNext: Boolean
) {
    val cornerRadius = dimensionResource(R.dimen.puzzle_tile_corner_radius)
    val paddingHorizontal = dimensionResource(R.dimen.puzzle_tile_padding_horizontal)
    val paddingVertical = dimensionResource(R.dimen.puzzle_tile_padding_vertical)

    val shape = RoundedCornerShape(
        topStart = if (isConnectedToPrevious) 0.dp else cornerRadius,
        topEnd = if (isConnectedToPrevious) 0.dp else cornerRadius,
        bottomStart = if (isConnectedToNext) 0.dp else cornerRadius,
        bottomEnd = if (isConnectedToNext) 0.dp else cornerRadius
    )

    val verticalPadding = if (isConnectedToPrevious || isConnectedToNext) 0.dp else paddingVertical

    Box(
        modifier = modifier
            .padding(paddingHorizontal, vertical = verticalPadding)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
    }
}

private fun isConnectedToPrevious(
    orderedPieces: List<PuzzlePieceUi>,
    index: Int
): Boolean {
    if (index == 0) return false
    val previous = orderedPieces[index - 1]
    val current = orderedPieces[index]
    return previous.correctIndex + 1 == current.correctIndex
}

private fun isConnectedToNext(
    orderedPieces: List<PuzzlePieceUi>,
    index: Int
): Boolean {
    if (index == orderedPieces.lastIndex) return false
    val current = orderedPieces[index]
    val next = orderedPieces[index + 1]
    return current.correctIndex + 1 == next.correctIndex
}

private fun formatDuration(durationInMs: Long): String {
    val totalSeconds = durationInMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return if (minutes > 0) {
        "${minutes}m ${seconds}s"
    } else {
        "${seconds}s"
    }
}
