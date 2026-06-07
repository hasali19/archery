package dev.hasali.archery.ui.sessions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.hasali.archery.data.DistanceUnit
import dev.hasali.archery.data.ScoringSystems
import dev.hasali.archery.data.Session
import dev.hasali.archery.data.standardRounds
import dev.hasali.archery.data.standardRoundsById
import dev.hasali.archery.repository.NewSessionParams
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

private val dateFormat = LocalDate.Format {
    day()
    char(' ')
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    year()
}

@Composable
private fun ChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
) {
    val leadingIcon = if (selected) {
        @Composable {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        }
    } else {
        null
    }

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        leadingIcon = leadingIcon,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    viewModel: SessionsViewModel,
    onNavigateToSession: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showNewSessionDialog by rememberSaveable { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }
    val bottomSheetState = rememberModalBottomSheetState()

    if (showNewSessionDialog) {
        NewSessionDialog(
            onDismiss = { showNewSessionDialog = false },
            onConfirm = { params ->
                showNewSessionDialog = false
                scope.launch {
                    val id = viewModel.createSession(params)
                    onNavigateToSession(id)
                }
            },
        )
    }

    if (sessionToDelete != null) {
        ModalBottomSheet(
            onDismissRequest = { sessionToDelete = null },
            sheetState = bottomSheetState,
        ) {
            ListItem(
                headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingContent = {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .combinedClickable(onClick = {
                        val id = sessionToDelete!!.id
                        sessionToDelete = null
                        viewModel.deleteSession(id)
                    }),
            )
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sessions") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewSessionDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New Session")
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
            )
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                items(uiState.sessions, key = { it.id }) { session ->
                    SessionListItem(
                        session = session,
                        total = uiState.totals[session.id] ?: 0,
                        isPersonalBest = session.id in uiState.pbSessionIds,
                        onClick = { onNavigateToSession(session.id) },
                        onLongPress = { sessionToDelete = session },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionListItem(
    session: Session,
    total: Int,
    isPersonalBest: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(session.roundDetails.displayName) },
            supportingContent = {
                val localDateTime =
                    session.startTime.toLocalDateTime(TimeZone.currentSystemDefault())
                Text(localDateTime.date.format(dateFormat))
            },
            trailingContent = {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = total.toString(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    if (isPersonalBest) {
                        Text(
                            text = "PB",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            },
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: (NewSessionParams) -> Unit,
) {
    var selectedRoundId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedScoringSystem by rememberSaveable { mutableStateOf("metric") }
    var distanceText by rememberSaveable { mutableStateOf("20") }
    var selectedDistanceUnit by rememberSaveable { mutableStateOf(DistanceUnit.Yards) }
    var selectedArrowsPerEnd by rememberSaveable { mutableIntStateOf(3) }
    var isCompetition by rememberSaveable { mutableStateOf(false) }
    var distanceError by rememberSaveable { mutableStateOf(false) }

    var roundDropdownExpanded by remember { mutableStateOf(false) }
    var scoringDropdownExpanded by remember { mutableStateOf(false) }

    val standardRound = standardRoundsById[selectedRoundId]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Round selector
                ExposedDropdownMenuBox(
                    expanded = roundDropdownExpanded,
                    onExpandedChange = { roundDropdownExpanded = it },
                ) {
                    OutlinedTextField(
                        value = standardRound?.displayName ?: "Free Practice",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Round") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = roundDropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = roundDropdownExpanded,
                        onDismissRequest = { roundDropdownExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Free Practice") },
                            onClick = {
                                selectedRoundId = null
                                selectedArrowsPerEnd = 3
                                roundDropdownExpanded = false
                            },
                        )
                        standardRounds.forEach { round ->
                            DropdownMenuItem(
                                text = { Text(round.displayName) },
                                onClick = {
                                    selectedRoundId = round.id
                                    selectedArrowsPerEnd = round.distances[0].defaultArrowsPerEnd
                                    roundDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                if (selectedRoundId == null) {
                    // Scoring system selector
                    ExposedDropdownMenuBox(
                        expanded = scoringDropdownExpanded,
                        onExpandedChange = { scoringDropdownExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = ScoringSystems.byId[selectedScoringSystem]?.displayName ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Scoring") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = scoringDropdownExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = scoringDropdownExpanded,
                            onDismissRequest = { scoringDropdownExpanded = false },
                        ) {
                            ScoringSystems.byId.values.forEach { system ->
                                DropdownMenuItem(
                                    text = { Text(system.displayName) },
                                    onClick = {
                                        selectedScoringSystem = system.id
                                        scoringDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    // Distance text field
                    OutlinedTextField(
                        value = distanceText,
                        onValueChange = {
                            distanceText = it
                            distanceError = it.toIntOrNull() == null
                        },
                        label = { Text("Distance") },
                        isError = distanceError,
                        supportingText = if (distanceError) ({ Text("Enter a valid number") }) else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Distance unit chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DistanceUnit.entries.forEach { unit ->
                            ChoiceChip(
                                selected = selectedDistanceUnit == unit,
                                onClick = { selectedDistanceUnit = unit },
                                label = { Text(unit.name) },
                            )
                        }
                    }
                }

                // Arrows per end chips
                val possibleArrowsPerEnd = standardRound?.distances?.get(0)?.possibleArrowsPerEnd
                    ?: listOf(3, 6)
                if (selectedRoundId == null || possibleArrowsPerEnd.size > 1) {
                    Column {
                        Text(
                            text = "Arrows per end",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            possibleArrowsPerEnd.forEach { value ->
                                ChoiceChip(
                                    selected = selectedArrowsPerEnd == value,
                                    onClick = { selectedArrowsPerEnd = value },
                                    label = { Text(value.toString()) },
                                )
                            }
                        }
                    }
                }

                // Practice / Competition toggle (standard rounds only)
                if (selectedRoundId != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceChip(
                            selected = !isCompetition,
                            onClick = { isCompetition = false },
                            label = { Text("Practice") },
                        )
                        ChoiceChip(
                            selected = isCompetition,
                            onClick = { isCompetition = true },
                            label = { Text("Competition") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val distance = distanceText.toIntOrNull()
                    if (selectedRoundId == null && distance == null) {
                        distanceError = true
                        return@TextButton
                    }
                    val params: NewSessionParams = if (selectedRoundId != null) {
                        NewSessionParams.Round(
                            roundId = selectedRoundId!!,
                            arrowsPerEnd = selectedArrowsPerEnd,
                            isCompetition = isCompetition,
                        )
                    } else {
                        NewSessionParams.FreePractice(
                            arrowsPerEnd = selectedArrowsPerEnd,
                            distance = distance!!,
                            distanceUnit = selectedDistanceUnit,
                            scoringSystem = selectedScoringSystem,
                        )
                    }
                    onConfirm(params)
                },
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
