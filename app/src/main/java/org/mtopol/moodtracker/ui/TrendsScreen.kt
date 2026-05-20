package org.mtopol.moodtracker.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
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
private val NoteDateFmt = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())

/** Upward caret painted just below the X axis to flag a day that has a note. */
private val PinShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

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

        // The pins themselves are the affordance; this caption only appears
        // when there is at least one note, so it never adds noise to an
        // all-numbers chart.
        if (state.notes.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.trends_note_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

/**
 * The note marker, drawn over the layers (the phase Vico reliably invokes for
 * persistent markers). For every note day it paints a small caret just below
 * the plot; for the one [selectedDay] it additionally paints a thin vertical
 * hairline spanning the plot in the same colour — a "playhead" tying the open
 * note to its exact column, which matters precisely when the pins are too
 * crowded to tell apart.
 *
 * Used both as the tap [CartesianMarker] (toggle-on-tap) and, via
 * `persistentMarkers`, at every note day; filtering by [noteDays] keeps a tap
 * on a note-less day from drawing a stray caret. The hairline lives here rather
 * than in a second marker because Vico's persistent-marker map is keyed by x —
 * a separate line marker would collide with the caret on the selected day's
 * column. Keying the remembered instance on [selectedDay] rebuilds the chart
 * (a `rememberCartesianChart` key) so the hairline repaints at the new column.
 */
private class NotePinMarker(
    private val pin: ShapeComponent,
    private val line: ShapeComponent,
    private val noteDays: Set<Long>,
    private val selectedDay: Long?,
    private val halfWidthPx: Float,
    private val heightPx: Float,
    private val gapPx: Float,
    private val lineHalfPx: Float,
) : CartesianMarker {
    override fun drawOverLayers(
        context: CartesianDrawingContext,
        targets: List<CartesianMarker.Target>,
    ) {
        val bounds = context.layerBounds
        val pinTop = bounds.bottom + gapPx
        targets.forEach { target ->
            val day = target.x.toLong()
            if (day !in noteDays) return@forEach
            val cx = target.canvasX
            if (day == selectedDay) {
                line.draw(context, cx - lineHalfPx, bounds.top, cx + lineHalfPx, bounds.bottom)
            }
            pin.draw(context, cx - halfWidthPx, pinTop, cx + halfWidthPx, pinTop + heightPx)
        }
    }
}

@Composable
private fun ColumnScope.MoodChart(state: TrendsUiState) {
    val anxColor = anxietyColor()
    val depColor = depressionColor()
    val pinColor = MaterialTheme.colorScheme.primary
    val modelProducer = remember { CartesianChartModelProducer() }

    // Y values are negated so the chart flips vertically: 0 (best) sits at the
    // top, MAX_GROUP_SCORE (worst) at the bottom. "Up = better" matches the
    // everyday intuition for graphs, even though the underlying scale is a
    // severity score where higher is worse. The Y-axis label formatter below
    // re-flips the sign so the gridlines still read 0..MAX_GROUP_SCORE.
    LaunchedEffect(state.startEpochDay, state.endEpochDay, state.anxiety, state.depression) {
        val anx = state.anxiety.filter { it.value != null }
        val dep = state.depression.filter { it.value != null }
        modelProducer.runTransaction {
            lineSeries {
                if (anx.isNotEmpty()) {
                    series(anx.map { it.epochDay }, anx.map { -it.value!! })
                }
                if (dep.isNotEmpty()) {
                    series(dep.map { it.epochDay }, dep.map { -it.value!! })
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
            minY = -MAX_GROUP_SCORE.toDouble(),
            maxY = 0.0,
        )
    }
    val xFormatter = remember {
        CartesianValueFormatter { _, value, _ ->
            LocalDate.ofEpochDay(value.toLong()).format(AxisDateFmt)
        }
    }
    val yFormatter = remember {
        CartesianValueFormatter { _, value, _ -> (-value.toInt()).toString() }
    }

    val notesByDay = remember(state.notes) { state.notes.associate { it.epochDay to it.text } }
    val density = LocalDensity.current

    // Only the selected day is held, never the resolved note: the displayed
    // note is derived from the *current* window below, so a range change that
    // drops the day naturally closes the reader (see `idx`). It is also a
    // `notePin` key, so a selection change rebuilds the chart and the hairline
    // repaints at the new column.
    var selectedDay by remember { mutableStateOf<Long?>(null) }

    val notePin = remember(state.notes, pinColor, density, selectedDay) {
        val halfW = with(density) { 5.dp.toPx() }
        val h = with(density) { 7.dp.toPx() }
        val gap = with(density) { 1.dp.toPx() }
        val lineHalf = with(density) { 0.75.dp.toPx() }
        NotePinMarker(
            pin = ShapeComponent(fill = Fill(pinColor), shape = PinShape),
            line = ShapeComponent(fill = Fill(pinColor), shape = RectangleShape),
            noteDays = notesByDay.keys,
            selectedDay = selectedDay,
            halfWidthPx = halfW,
            heightPx = h,
            gapPx = gap,
            lineHalfPx = lineHalf,
        )
    }
    val markerListener = remember(notesByDay) {
        object : CartesianMarkerVisibilityListener {
            // The inline area is empty unless a note day is the active target,
            // so tapping a note-less day (or toggling the pin off) clears it.
            private fun resolve(targets: List<CartesianMarker.Target>) {
                val day = targets.firstOrNull()?.x?.toLong()
                selectedDay = if (day != null && notesByDay.containsKey(day)) day else null
            }

            override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) =
                resolve(targets)

            override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) =
                resolve(targets)

            override fun onHidden(marker: CartesianMarker) {
                selectedDay = null
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = lineProvider,
                rangeProvider = rangeProvider,
            ),
            startAxis = VerticalAxis.rememberStart(valueFormatter = yFormatter),
            bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xFormatter),
            marker = notePin,
            markerVisibilityListener = markerListener,
            markerController = CartesianMarkerController.rememberToggleOnTap(),
            // Always-visible pins: place the marker at every note day so its
            // caret is painted there even before any tap. The same instance
            // also draws the hairline at `selectedDay` (it carries it), so the
            // playhead tracks taps *and* prev/next and clears on a range change.
            persistentMarkers = { _ -> state.notes.forEach { notePin.at(it.epochDay) } },
        ),
        modelProducer = modelProducer,
        // No enter/diff animation: the chart is rebuilt from a fresh
        // modelProducer every time the Trends tab is entered, so Vico's
        // default spec would replay a grow-in animation on each visit.
        // null applies the data instantly.
        animationSpec = null,
        // The chart must always show the whole selected range at once. Vico's
        // host defaults to a horizontally scrollable viewport at fixed (1x)
        // zoom, so a wide range (e.g. 1Y / All) overflows and is only reachable
        // by scrolling. Disabling scroll and pinning zoom to Zoom.Content
        // scales the X axis to fit every day in the range into the viewport.
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
    )

    // Notes arrive ascending (DAO orders by epochDay); sorted defensively so
    // prev/next is reliable regardless of upstream ordering.
    val ordered = remember(state.notes) { state.notes.sortedBy { it.epochDay } }
    // Resolve the open note against the *current* window. A range change swaps
    // `state.notes`; if the selected day is no longer plotted it isn't in
    // `ordered`, `idx` is -1, and the reader closes with its now-absent pin —
    // never a note stranded from a range that's no longer shown.
    val idx = selectedDay?.let { d -> ordered.indexOfFirst { it.epochDay == d } } ?: -1

    // The note reader: plain text below the chart — no card, border, or
    // background — so the space is simply absent until a note pin is tapped,
    // and gone again on toggle-off / tapping elsewhere / range change. weight(1f)
    // lets a long note scroll the leftover height instead of overflowing.
    if (idx >= 0) {
        val note = ordered[idx]
        Spacer(Modifier.height(12.dp))
        // Arrows sit side by side so you can step back and forth with the same
        // thumb, without reaching across the width; the counter trails them.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { if (idx > 0) selectedDay = ordered[idx - 1].epochDay },
                enabled = idx > 0,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_prev_note),
                )
            }
            IconButton(
                onClick = { if (idx < ordered.lastIndex) selectedDay = ordered[idx + 1].epochDay },
                enabled = idx < ordered.lastIndex,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.cd_next_note),
                )
            }
            Text(
                stringResource(R.string.note_position, idx + 1, ordered.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                LocalDate.ofEpochDay(note.epochDay).format(NoteDateFmt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                note.text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
