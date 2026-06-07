package dev.hasali.archery.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.hasali.archery.data.Score
import dev.hasali.archery.data.ScoringSystem

@Composable
fun ScoreKeyboard(
    scoringSystem: ScoringSystem,
    onScorePress: (Score) -> Unit,
    onBackspacePress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val minButtonWidthDp = 70.dp
        val columns = (maxWidth / minButtonWidthDp).toInt().coerceAtLeast(2)
        val buttonWidth = maxWidth / columns

        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                scoringSystem.scores.chunked(columns - 1).forEach { rowScores ->
                    Row {
                        rowScores.forEach { score ->
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                                    onScorePress(score)
                                },
                                modifier = Modifier.width(buttonWidth).padding(horizontal = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = score.color,
                                    contentColor = score.foregroundColor,
                                ),
                            ) {
                                androidx.compose.material3.Text(score.label)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackspacePress()
                },
                modifier = Modifier.width(buttonWidth).padding(horizontal = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                )
            }
        }
    }
}
