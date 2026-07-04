package dev.hasali.archery.data

data class StandardRound(
    val id: String,
    val displayName: String,
    val scoringSystem: ScoringSystem,
    val distances: List<StandardRoundDistance>,
)

data class StandardRoundDistance(
    val distanceValue: DistanceValue,
    val arrows: Int,
    val defaultArrowsPerEnd: Int,
    val possibleArrowsPerEnd: List<Int>,
)

val standardRounds = listOf(
    StandardRound(
        id = "portsmouth",
        displayName = "Portsmouth",
        scoringSystem = ScoringSystems.metric,
        distances = listOf(
            StandardRoundDistance(
                distanceValue = DistanceValue(20, DistanceUnit.Yards),
                arrows = 60,
                defaultArrowsPerEnd = 3,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
        ),
    ),
    StandardRound(
        id = "frostbite",
        displayName = "Frostbite",
        scoringSystem = ScoringSystems.metric,
        distances = listOf(
            StandardRoundDistance(
                distanceValue = DistanceValue(30, DistanceUnit.Metres),
                arrows = 36,
                defaultArrowsPerEnd = 3,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
        ),
    ),
    StandardRound(
        id = "short_metric_1",
        displayName = "Short Metric I",
        scoringSystem = ScoringSystems.metric,
        distances = listOf(
            StandardRoundDistance(
                distanceValue = DistanceValue(50, DistanceUnit.Metres),
                arrows = 36,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
            StandardRoundDistance(
                distanceValue = DistanceValue(30, DistanceUnit.Metres),
                arrows = 36,
                defaultArrowsPerEnd = 3,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
        ),
    ),
    StandardRound(
        id = "worcester",
        displayName = "Worcester",
        scoringSystem = ScoringSystems.worcester,
        distances = listOf(
            StandardRoundDistance(
                distanceValue = DistanceValue(20, DistanceUnit.Yards),
                arrows = 30,
                defaultArrowsPerEnd = 5,
                possibleArrowsPerEnd = listOf(5),
            ),
            StandardRoundDistance(
                distanceValue = DistanceValue(20, DistanceUnit.Yards),
                arrows = 30,
                defaultArrowsPerEnd = 5,
                possibleArrowsPerEnd = listOf(5),
            ),
        ),
    ),
    StandardRound(
        id = "windsor_60",
        displayName = "Windsor",
        scoringSystem = ScoringSystems.imperial,
        distances = listOf(
            StandardRoundDistance(
                distanceValue = DistanceValue(60, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
            StandardRoundDistance(
                distanceValue = DistanceValue(50, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
            StandardRoundDistance(
                distanceValue = DistanceValue(40, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
        ),
    ),
    StandardRound(
        id = "windsor_50",
        displayName = "Windsor 50",
        scoringSystem = ScoringSystems.imperial,
        distances = listOf(
            StandardRoundDistance(
                distanceValue = DistanceValue(50, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
            StandardRoundDistance(
                distanceValue = DistanceValue(40, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
            StandardRoundDistance(
                distanceValue = DistanceValue(30, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
        ),
    ),
    StandardRound(
        id = "windsor_40",
        displayName = "Windsor 40",
        scoringSystem = ScoringSystems.imperial,
        distances = listOf(
            StandardRoundDistance(
                distanceValue = DistanceValue(40, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
            StandardRoundDistance(
                distanceValue = DistanceValue(30, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
            StandardRoundDistance(
                distanceValue = DistanceValue(20, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
        ),
    ),
    StandardRound(
        id = "windsor_30",
        displayName = "Windsor 30",
        scoringSystem = ScoringSystems.imperial,
        distances = listOf(
            StandardRoundDistance(
                distanceValue = DistanceValue(30, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
            StandardRoundDistance(
                distanceValue = DistanceValue(20, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
            StandardRoundDistance(
                distanceValue = DistanceValue(10, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
        ),
    ),
    StandardRound(
        id = "american",
        displayName = "American",
        scoringSystem = ScoringSystems.imperial,
        distances = listOf(
            StandardRoundDistance(
                distanceValue = DistanceValue(60, DistanceUnit.Yards),
                arrows = 30,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
            StandardRoundDistance(
                distanceValue = DistanceValue(50, DistanceUnit.Yards),
                arrows = 30,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
            StandardRoundDistance(
                distanceValue = DistanceValue(40, DistanceUnit.Yards),
                arrows = 30,
                defaultArrowsPerEnd = 6,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
        ),
    ),
    StandardRound(
        id = "252_60",
        displayName = "252 - 60 yds",
        scoringSystem = ScoringSystems.imperial,
        distances = listOf(
            StandardRoundDistance(
                distanceValue = DistanceValue(60, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 3,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
        ),
    ),
    StandardRound(
        id = "252_50",
        displayName = "252 - 50 yds",
        scoringSystem = ScoringSystems.imperial,
        distances = listOf(
            StandardRoundDistance(
                distanceValue = DistanceValue(50, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 3,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
        ),
    ),
    StandardRound(
        id = "252_40",
        displayName = "252 - 40 yds",
        scoringSystem = ScoringSystems.imperial,
        distances = listOf(
            StandardRoundDistance(
                distanceValue = DistanceValue(40, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 3,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
        ),
    ),
    StandardRound(
        id = "252_30",
        displayName = "252 - 30 yds",
        scoringSystem = ScoringSystems.imperial,
        distances = listOf(
            StandardRoundDistance(
                distanceValue = DistanceValue(30, DistanceUnit.Yards),
                arrows = 36,
                defaultArrowsPerEnd = 3,
                possibleArrowsPerEnd = listOf(3, 6),
            ),
        ),
    ),
)

val standardRoundsById = standardRounds.associateBy { it.id }
