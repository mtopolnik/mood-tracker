package org.mtopol.moodtracker.ui

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import org.mtopol.moodtracker.R
import org.mtopol.moodtracker.TrendsUiState
import org.mtopol.moodtracker.domain.ChartRange
import org.mtopol.moodtracker.domain.MAX_GROUP_SCORE
import org.mtopol.moodtracker.ui.theme.anxietyColor
import org.mtopol.moodtracker.ui.theme.depressionColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AxisDateFmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

@Composable
fun TrendsScreen(
    state: TrendsUiState,
    onRange: (ChartRange) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChartRange.entries.forEach { range ->
                FilterChip(
                    selected = state.range == range,
                    onClick = { onRange(range) },
                    label = { Text(range.label) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            LegendSwatch(stringResource(R.string.anxiety), anxietyColor())
            LegendSwatch(stringResource(R.string.depression), depressionColor())
        }

        Spacer(Modifier.height(16.dp))

        if (!state.hasData) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.trends_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            MoodChart(state)
        }
    }
}

@Composable
private fun LegendSwatch(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = color, shape = CircleShape, modifier = Modifier.size(12.dp)) {}
        Spacer(Modifier.size(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun MoodChart(state: TrendsUiState) {
    val anxColor = anxietyColor()
    val depColor = depressionColor()
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(state.startEpochDay, state.endEpochDay, state.anxiety, state.depression) {
        val anx = state.anxiety.filter { it.value != null }
        val dep = state.depression.filter { it.value != null }
        modelProducer.runTransaction {
            lineSeries {
                if (anx.isNotEmpty()) {
                    series(anx.map { it.epochDay }, anx.map { it.value!! })
                }
                if (dep.isNotEmpty()) {
                    series(dep.map { it.epochDay }, dep.map { it.value!! })
                }
            }
        }
    }

    val lineProvider = LineCartesianLayer.LineProvider.series(
        LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(Fill(anxColor))),
        LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(Fill(depColor))),
    )
    val rangeProvider = remember(state.startEpochDay, state.endEpochDay) {
        CartesianLayerRangeProvider.fixed(
            minX = state.startEpochDay.toDouble(),
            maxX = state.endEpochDay.toDouble(),
            minY = 0.0,
            maxY = MAX_GROUP_SCORE.toDouble(),
        )
    }
    val xFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            LocalDate.ofEpochDay(value.toLong()).format(AxisDateFmt)
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = lineProvider,
                rangeProvider = rangeProvider,
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xFormatter),
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
    )
}
