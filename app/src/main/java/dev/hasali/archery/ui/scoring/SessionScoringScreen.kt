package dev.hasali.archery.ui.scoring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hasali.archery.data.RoundDistance
import dev.hasali.archery.data.Score
import dev.hasali.archery.ui.components.ScoreKeyboard
import kotlin.time.Instant

// Sealed items for the flat scoresheet list
private sealed interface ScoreSheetItem {
    data class DistanceHeader(
        val label: String,
        val total: Int,
    ) : ScoreSheetItem

    data class EndRow(
        val endNumber: Int,
        val scores: List<Score>,
    ) : ScoreSheetItem

    data object Divider : ScoreSheetItem
}

private fun buildScoreSheetItems(
    distances: List<RoundDistance>,
    scores: List<Score>,
): List<ScoreSheetItem> {
    val items = mutableListOf<ScoreSheetItem>()
    for (distance in distances) {
        val distanceScores = if (distance.firstArrowIndex < scores.size) {
            scores.subList(
                distance.firstArrowIndex,
                minOf(distance.firstArrowIndex + distance.arrows, scores.size),
            )
        } else {
            emptyList()
        }
        val distanceTotal = distanceScores.sumOf { it.value }
        items += ScoreSheetItem.DistanceHeader(
            label = "${distance.distanceValue.value} ${distance.distanceValue.unit.name}",
            total = distanceTotal,
        )
        items += ScoreSheetItem.Divider
        for (end in 0 until distance.ends) {
            val startIdx = end * distance.arrowsPerEnd
            val endScores = if (startIdx >= distanceScores.size) {
                emptyList()
            } else {
                distanceScores.subList(
                    startIdx,
                    minOf(startIdx + distance.arrowsPerEnd, distanceScores.size),
                )
            }
            items += ScoreSheetItem.EndRow(
                endNumber = distance.firstEndIndex + end + 1,
                scores = endScores,
            )
            items += ScoreSheetItem.Divider
        }
    }
    return items
}

// Compute which flat list item index corresponds to a given arrow index
private fun targetItemIndexForArrow(
    arrowIndex: Int,
    distances: List<RoundDistance>,
    scoreSheetItems: List<ScoreSheetItem>,
): Int {
    // Find which distance+end the arrow belongs to
    for (distanceIdx in distances.indices) {
        val distance = distances[distanceIdx]
        if (arrowIndex in distance.firstArrowIndex..distance.lastArrowIndex) {
            val localArrow = arrowIndex - distance.firstArrowIndex
            val endIdx = localArrow / distance.arrowsPerEnd
            // Each distance in the flat list starts with: header + divider = 2 items
            // Then alternates: EndRow + Divider (2 items per end)
            // Previous distances each consume: 2 + (ends * 2) items
            var offset = 0
            for (d in 0 until distanceIdx) {
                offset += 2 + distances[d].ends * 2
            }
            // +2 to skip header and first divider, then endIdx * 2 for the end rows
            return offset + 2 + endIdx * 2
        }
    }
    return scoreSheetItems.size - 1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScoringScreen(
    viewModel: SessionScoringViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val session = uiState.session
    val scores = uiState.scores
    val listState = rememberLazyListState()
    var showDatePicker by remember { mutableStateOf(false) }

    val distances = session?.roundDetails?.distances ?: emptyList()
    val scoreSheetItems = remember(scores, distances) {
        buildScoreSheetItems(distances, scores)
    }

    // Auto-scroll to the current arrow position when scores change
    LaunchedEffect(scores.size) {
        if (scores.isNotEmpty() && distances.isNotEmpty()) {
            val targetIdx = targetItemIndexForArrow(
                arrowIndex = (scores.size - 1).coerceAtLeast(0),
                distances = distances,
                scoreSheetItems = scoreSheetItems,
            )
            val layoutInfo = listState.layoutInfo
            val targetItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIdx }
            val isFullyVisible = targetItem != null &&
                targetItem.offset >= layoutInfo.viewportStartOffset &&
                targetItem.offset + targetItem.size <= layoutInfo.viewportEndOffset
            if (!isFullyVisible) {
                listState.animateScrollToItem(targetIdx)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = session?.startTime?.toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.updateStartTime(Instant.fromEpochMilliseconds(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(session?.roundDetails?.displayName ?: "") },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Change date")
                    }
                    IconButton(onClick = {
                        viewModel.deleteSession { onNavigateBack() }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete session")
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Score sheet
            ScoreSheet(
                listState = listState,
                items = scoreSheetItems,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            )

            // Stats bar
            val total = scores.sumOf { it.value }
            val average = if (scores.isEmpty()) 0.0 else total.toDouble() / scores.size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Text("Total: $total")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Average: ${"%.2f".format(average)}")
            }

            // Score keyboard
            if (session != null) {
                ScoreKeyboard(
                    scoringSystem = session.roundDetails.scoringSystem,
                    onScorePress = viewModel::addScore,
                    onBackspacePress = viewModel::removeLastScore,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun ScoreSheet(
    listState: LazyListState,
    items: List<ScoreSheetItem>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(state = listState, modifier = modifier) {
        items(
            count = items.size,
            key = { index -> index },
        ) { index ->
            when (val item = items[index]) {
                is ScoreSheetItem.DistanceHeader -> DistanceHeaderRow(item)
                is ScoreSheetItem.EndRow -> EndRow(item)
                is ScoreSheetItem.Divider -> HorizontalDivider()
            }
        }
    }
}

@Composable
private fun DistanceHeaderRow(item: ScoreSheetItem.DistanceHeader) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(item.label, style = MaterialTheme.typography.titleSmall)
        Text("Total: ${item.total}", style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun EndRow(item: ScoreSheetItem.EndRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // End number
        Text(
            text = "${item.endNumber}",
            modifier = Modifier
                .width(42.dp)
                .padding(start = 8.dp, end = 16.dp)
                .alpha(0.6f),
            style = MaterialTheme.typography.labelMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )

        // Score circles
        item.scores.forEach { score ->
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(36.dp)
                    .background(color = score.color, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = score.label,
                    color = score.foregroundColor,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // End total
        Text(
            text = item.scores.sumOf { it.value }.toString(),
            modifier = Modifier.padding(end = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
