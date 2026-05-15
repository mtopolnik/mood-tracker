package org.mtopol.moodtracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.mtopol.moodtracker.QuestionnaireUiState
import org.mtopol.moodtracker.R
import org.mtopol.moodtracker.domain.QUESTION_LABELS
import org.mtopol.moodtracker.domain.QUESTION_COUNT
import org.mtopol.moodtracker.domain.isFirstOfSecondGroup
import org.mtopol.moodtracker.ui.components.CompletionPill
import org.mtopol.moodtracker.ui.components.ScorePill
import org.mtopol.moodtracker.ui.components.ScoreSelector
import org.mtopol.moodtracker.ui.theme.anxietyColor
import org.mtopol.moodtracker.ui.theme.depressionColor
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TitleDate = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())
private val FullDate = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())

/**
 * The 12-item form for a given day. Used both as the Today tab ([showBack] =
 * false) and as the full-screen editor for a past day ([showBack] = true).
 *
 * Every choice is saved immediately (no Save button); the bottom bar shows the
 * two category gauges, always visible and updating live as items are answered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionnaireScreen(
    state: QuestionnaireUiState,
    onAnswer: (Int, Int) -> Unit,
    showBack: Boolean,
    onBack: (() -> Unit)?,
) {
    val title = if (state.isToday) stringResource(R.string.today) else state.date.format(TitleDate)

    Scaffold(
        topBar = {
            if (showBack && onBack != null) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                )
            }
        },
        bottomBar = {
            androidx.compose.material3.Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // As the past-day editor this is the root Scaffold and
                        // its bottom bar reaches the screen edge; consume the
                        // system navigation inset so it doesn't sit under the
                        // navigation pill. As the Today tab the bar sits above
                        // MoodApp's NavigationBar, which already handles it.
                        .then(if (showBack) Modifier.navigationBarsPadding() else Modifier)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    CompletionPill(
                        label = stringResource(R.string.progress),
                        answered = state.answers.count { it >= 0 },
                        total = QUESTION_COUNT,
                    )
                    ScorePill(stringResource(R.string.anxiety), state.anxiety, anxietyColor())
                    ScorePill(stringResource(R.string.depression), state.depression, depressionColor())
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                Column(Modifier.padding(bottom = 12.dp)) {
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        state.date.format(FullDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            itemsIndexed(state.answers) { index, answer ->
                // Larger gap (and nothing else) before the first item of the
                // second group — the grouping is never labelled.
                if (isFirstOfSecondGroup(index)) Spacer(Modifier.height(36.dp))
                Column(Modifier.padding(vertical = 10.dp)) {
                    Text(
                        stringResource(QUESTION_LABELS[index]),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    ScoreSelector(value = answer, onValue = { onAnswer(index, it) })
                }
            }
        }
    }
}
