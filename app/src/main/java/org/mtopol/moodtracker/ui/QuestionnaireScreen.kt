package org.mtopol.moodtracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

/** Soft cap on the day's note — it is meant to be a short remark, not a journal. */
private const val NOTE_MAX_LEN = 500

/**
 * The 12-item form for a given day. Used both as the Today tab ([showBack] =
 * false) and as the full-screen editor for a past day ([showBack] = true).
 *
 * Every choice is saved immediately (no Save button); the bottom bar shows the
 * two category gauges, always visible and updating live as items are answered.
 */
@Composable
fun QuestionnaireScreen(
    state: QuestionnaireUiState,
    onAnswer: (Int, Int) -> Unit,
    onNote: (String) -> Unit,
    showBack: Boolean,
    onBack: (() -> Unit)?,
) {
    val title = if (state.isToday) stringResource(R.string.today) else state.date.format(TitleDate)

    Scaffold(
        topBar = {
            // Compact custom header instead of Material TopAppBar: the latter
            // adds a fixed ~64dp content height on top of the consumed
            // status-bar inset, wasting space above a short two-line title.
            // Here we only pad for the status bar, then size to the content.
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.background,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showBack && onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    }
                    Column {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            state.date.format(FullDate),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
                        // CloudyApp's NavigationBar, which already handles it.
                        // The bar deliberately ignores the IME: when the
                        // keyboard is up it is simply covered (the score gauges
                        // are irrelevant while typing the note).
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
                // Reserve the bar/system insets, mark them consumed, then add
                // the IME. Because the bar space is already consumed, imePadding
                // contributes only max(0, ime - bar): the list's bottom inset
                // is max(bar, ime), never their sum. So the focused note field
                // scrolls clear of the keyboard with no empty gap, while the
                // bottom bar stays put and is simply covered by the IME.
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
        ) {
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

            item {
                Spacer(Modifier.height(36.dp))
                OutlinedTextField(
                    value = state.note,
                    // Drop edits past the soft cap rather than truncating
                    // silently mid-type: the field just stops accepting more.
                    onValueChange = { if (it.length <= NOTE_MAX_LEN) onNote(it) },
                    label = { Text(stringResource(R.string.note_label)) },
                    placeholder = { Text(stringResource(R.string.note_placeholder)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                // Breathing room so the field clears the bottom score bar.
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
