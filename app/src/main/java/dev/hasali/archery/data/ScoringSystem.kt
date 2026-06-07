package dev.hasali.archery.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

data class Score(
    val id: Int,
    val label: String,
    val value: Int,
    val color: Color,
) {
    val foregroundColor: Color
        get() = if (color.luminance() > 0.5f) Color.Black else Color.White
}

data class ScoringSystem(
    val id: String,
    val displayName: String,
    val scores: List<Score>,
) {
    private val scoresById = scores.associateBy { it.id }

    fun get(id: Int): Score = scoresById.getValue(id)
}

private val yellow = Color(0xFFFFEB3B)
private val red = Color(0xFFF44336)
private val blue = Color(0xFF2196F3)
private val green = Color(0xFF4CAF50)
private val black = Color(0xFF080808)
private val white = Color.White

object ScoringSystems {
    val metric = ScoringSystem(
        id = "metric",
        displayName = "Metric (10 Zone)",
        scores = listOf(
            Score(id = 1, label = "X", value = 10, color = yellow),
            Score(id = 2, label = "10", value = 10, color = yellow),
            Score(id = 3, label = "9", value = 9, color = yellow),
            Score(id = 4, label = "8", value = 8, color = red),
            Score(id = 5, label = "7", value = 7, color = red),
            Score(id = 6, label = "6", value = 6, color = blue),
            Score(id = 7, label = "5", value = 5, color = blue),
            Score(id = 8, label = "4", value = 4, color = black),
            Score(id = 9, label = "3", value = 3, color = black),
            Score(id = 10, label = "2", value = 2, color = white),
            Score(id = 11, label = "1", value = 1, color = white),
            Score(id = 12, label = "M", value = 0, color = green),
        ),
    )

    val imperial = ScoringSystem(
        id = "imperial",
        displayName = "Imperial (5 Zone)",
        scores = listOf(
            Score(id = 1, label = "9", value = 9, color = yellow),
            Score(id = 2, label = "7", value = 7, color = red),
            Score(id = 3, label = "5", value = 5, color = blue),
            Score(id = 4, label = "3", value = 3, color = black),
            Score(id = 5, label = "1", value = 1, color = white),
            Score(id = 6, label = "M", value = 0, color = green),
        ),
    )

    val worcester = ScoringSystem(
        id = "worcester",
        displayName = "Worcester",
        scores = listOf(
            Score(id = 1, label = "5", value = 5, color = white),
            Score(id = 2, label = "4", value = 4, color = black),
            Score(id = 3, label = "3", value = 3, color = black),
            Score(id = 4, label = "2", value = 2, color = black),
            Score(id = 5, label = "1", value = 1, color = black),
            Score(id = 6, label = "M", value = 0, color = green),
        ),
    )

    val byId = mapOf(
        metric.id to metric,
        imperial.id to imperial,
        worcester.id to worcester,
    )
}
