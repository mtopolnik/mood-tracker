package org.mtopol.moodtracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import org.mtopol.moodtracker.R
import org.mtopol.moodtracker.domain.MAX_GROUP_SCORE

private val TrackShape = RoundedCornerShape(percent = 50)

/**
 * A small category gauge: the label above a fixed-width track whose
 * proportional fill encodes score / [MAX_GROUP_SCORE]. No number — the bar's
 * length is the magnitude and its [color] identifies the category (the faint
 * track keeps that hue visible even at a zero/low score). The full track is
 * always drawn so a low score reads as "near the bottom of the scale", and the
 * exact value is still exposed to accessibility via [clearAndSetSemantics].
 */
@Composable
fun ScorePill(
    label: String,
    score: Int?,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val target = if (score != null) {
        (score / MAX_GROUP_SCORE.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    // Animates the fill as the live Today score changes while answering.
    val fraction by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 300),
        label = "scoreFill",
    )

    val description = if (score != null) {
        stringResource(R.string.score_content_desc, label, score, MAX_GROUP_SCORE)
    } else {
        stringResource(R.string.score_content_desc_empty, label)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(14.dp)
                .clip(TrackShape)
                .background(color.copy(alpha = 0.16f)),
        ) {
            if (fraction > 0f) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(color),
                )
            }
        }
    }
}
