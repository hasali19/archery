/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package dev.hasali.archery.presentation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.LocalContentColor
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.touchTargetAwareSize
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.google.android.horologist.compose.ambient.AmbientAware
import com.google.android.horologist.compose.ambient.AmbientState
import dev.hasali.archery.R
import dev.hasali.archery.presentation.theme.AndroidTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

private const val CHANNEL_ID = "activity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureNotificationChannel()
        setContent {
            WearApp()
        }
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Active Session",
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }
}

@Composable
fun WearApp() {
    val context = LocalContext.current
    val client = remember { ActiveSessionClient(context) }
    val sessionState by client
        .observeSession()
        .collectAsState(initial = ArcherySessionState.Loading)

    val isSessionActive = sessionState is ArcherySessionState.Active

    LaunchedEffect(isSessionActive) {
        if (isSessionActive) {
            val touchIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val builder = NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setContentTitle("Archery session active")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(touchIntent)
                .setOngoing(true)
                .setSilent(true)

            val ongoingActivity = OngoingActivity
                .Builder(context, 1, builder)
                .setTouchIntent(touchIntent)
                .setStatus(Status.forPart(Status.TextPart("Session active")))
                .build()

            ongoingActivity.apply(context)

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@LaunchedEffect
            }

            NotificationManagerCompat.from(context).notify(1, builder.build())
        } else {
            NotificationManagerCompat.from(context).cancel(1)
        }
    }

    AmbientAware { ambientState ->
        AndroidTheme {
            AppScaffold {
                if (ambientState.isAmbient && sessionState is ArcherySessionState.Active) {
                    val session = (sessionState as ArcherySessionState.Active).session
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${session.totalScore}",
                                color = Color.White,
                                fontSize = 48.sp,
                            )
                            session.name?.let {
                                Text(
                                    text = it,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                )
                            }
                            session.currentDistance?.let { dist ->
                                val suffix = if (dist.unit == DistanceUnit.Metres) "m" else "yd"
                                Text(
                                    text = "${dist.value}$suffix",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                } else {
                    when (val state = sessionState) {
                        ArcherySessionState.Loading -> {}

                        ArcherySessionState.Inactive -> {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Text("No session active")
                            }
                        }

                        is ArcherySessionState.Active -> {
                            ScoringScreen(
                                session = state.session,
                                onScoreTapped = { score -> client.addScore(state.session.sessionId, score.id) },
                                onBackspaceTapped = { client.deleteLastScore(state.session.sessionId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoringScreen(
    session: ArcherySession,
    onScoreTapped: (ArrowScore) -> Unit,
    onBackspaceTapped: () -> Unit,
) {
    val screenWidthAtOffset =
        @Composable { offset: Dp ->
            val screenHeight = LocalConfiguration.current.screenHeightDp / 2
            val radius = LocalConfiguration.current.smallestScreenWidthDp / 2
            (sqrt((radius * radius) - (screenHeight - offset.value) * (screenHeight - offset.value)) * 2).dp
        }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            val offset = 32.dp
            val width = screenWidthAtOffset(offset) - 8.dp

            Spacer(Modifier.height(offset))

            val scoreSize = width / 6 - 2.dp

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier =
                    Modifier
                        .width(width)
                        .height(scoreSize)
                        .align(Alignment.CenterHorizontally),
            ) {
                for (score in session.endScores) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .padding(horizontal = 1.dp)
                                .size(scoreSize)
                                .clip(CircleShape)
                                .background(score.color),
                    ) {
                        BasicText(
                            text = score.label,
                            color = { score.foregroundColor },
                            autoSize = TextAutoSize.StepBased(maxFontSize = 14.sp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            val bottomHeight = 48.dp
            val minOffset = min(offset.value + scoreSize.value + 4, bottomHeight.value)
            val mainWidth = screenWidthAtOffset(minOffset.dp)

            BoxWithConstraints(
                modifier =
                    Modifier
                        .weight(1f)
                        .width(mainWidth),
            ) {
                val width = constraints.maxWidth
                val buttonWidth = with(LocalDensity.current) { (width / 4).toDp() }
                val buttonHeight = with(LocalDensity.current) { (constraints.maxHeight / 3).toDp() }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    for (scoresRow in session.keyboardScores.chunked(4)) {
                        Row {
                            for (score in scoresRow) {
                                CompositionLocalProvider(LocalContentColor provides score.foregroundColor) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier =
                                            Modifier
                                                .size(buttonWidth, buttonHeight)
                                                .padding(2.dp)
                                                .clip(SquircleShape())
                                                .background(score.color)
                                                .clickable { onScoreTapped(score) },
                                    ) {
                                        val contentColor = LocalContentColor.current
                                        BasicText(
                                            text = score.label,
                                            color = { contentColor },
                                            autoSize = TextAutoSize.StepBased(maxFontSize = 18.sp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.height(bottomHeight),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    modifier = Modifier.touchTargetAwareSize(IconButtonDefaults.SmallButtonSize),
                    onClick = onBackspaceTapped,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Backspace,
                        contentDescription = null,
                        modifier = Modifier.size(IconButtonDefaults.SmallIconSize),
                    )
                }
            }
        }
    }
}

class SquircleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val n = 3f

        val steps = 100
        val a = size.width / 2
        val b = size.height / 2

        for (i in 0..steps) {
            val t = (2 * PI * i / steps).toFloat()
            val cosT = cos(t)
            val sinT = sin(t)

            val x = a + a * sign(cosT) * abs(cosT).pow(2 / n)
            val y = b + b * sign(sinT) * abs(sinT).pow(2 / n)

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        path.close()
        return Outline.Generic(path)
    }
}

@WearPreviewDevices
@Composable
private fun DefaultPreview() {
    WearApp()
}
