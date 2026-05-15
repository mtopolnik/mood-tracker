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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import org.mtopol.moodtracker.R

private val TrackShape = RoundedCornerShape(percent = 50)

/**
 * A deliberately subdued sibling of [ScorePill] showing how many of [total]
 * items are still unanswered. The fill represents what's *remaining*: it spans
 * the full track when nothing is answered and shrinks towards the right edge
 * as items are answered. Once the form is complete there is nothing remaining,
 * so the whole pill disappears. Sized to match the mood gauges but kept
 * lower-contrast so it reads as secondary to them.
 */
@Composable
fun CompletionPill(
    label: String,
    answered: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val remaining = (total - answered).coerceAtLeast(0)
    // Complete = nothing remaining: the whole pill goes away.
    if (remaining == 0) return

    val target = if (total > 0) (remaining.toFloat() / total).coerceIn(0f, 1f) else 0f
    val fraction by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 300),
        label = "completionFill",
    )

    val fillColor = MaterialTheme.colorScheme.onSurfaceVariant
    val description = stringResource(R.string.progress_content_desc, label, remaining, total)

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
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f)),
        ) {
            if (fraction > 0f) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(fillColor.copy(alpha = 0.40f)),
                )
            }
        }
    }
}
