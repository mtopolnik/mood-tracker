package org.mtopol.moodtracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.mtopol.moodtracker.HistoryRow
import org.mtopol.moodtracker.HistoryUiState
import org.mtopol.moodtracker.R
import org.mtopol.moodtracker.ui.components.ScorePill
import org.mtopol.moodtracker.ui.theme.anxietyColor
import org.mtopol.moodtracker.ui.theme.depressionColor
import org.mtopol.moodtracker.ui.theme.missedColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val WeekdayFmt = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
private val DayFmt = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

/** Last 30 days, newest first, with missed days clearly distinct and tappable. */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onOpen: (LocalDate) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 8.dp)) {
                Text(
                    stringResource(R.string.tab_history),
                    style = MaterialTheme.typography.headlineSmall,
                )
                val summary = if (state.missedCount == 0) {
                    stringResource(R.string.all_caught_up, state.totalDays)
                } else {
                    stringResource(R.string.missed_summary, state.missedCount)
                }
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(state.rows, key = { it.date.toEpochDay() }) { row ->
            if (row.present) PresentRow(row, onOpen) else MissedRow(row, onOpen)
        }
    }
}

@Composable
private fun PresentRow(row: HistoryRow, onOpen: (LocalDate) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(row.date) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DateLabel(row.date)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ScorePill(stringResource(R.string.anxiety), row.anxiety, anxietyColor())
                    ScorePill(stringResource(R.string.depression), row.depression, depressionColor())
                }
            }
            if (!row.note.isNullOrBlank()) {
                Text(
                    row.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MissedRow(row: HistoryRow, onOpen: (LocalDate) -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(row.date) },
        border = BorderStroke(1.dp, missedColor().copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DateLabel(row.date, muted = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.no_entry),
                    style = MaterialTheme.typography.bodyMedium,
                    color = missedColor(),
                )
                Spacer(Modifier.height(0.dp))
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                    tint = missedColor(),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun DateLabel(date: LocalDate, muted: Boolean = false) {
    val color = if (muted) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Column {
        Text(
            date.format(WeekdayFmt),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
        Text(
            date.format(DayFmt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
