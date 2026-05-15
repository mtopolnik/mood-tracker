package org.mtopol.moodtracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.mtopol.moodtracker.domain.MAX_ANSWER

/**
 * 0–3 segmented selector. No descriptive text for the scores by design.
 * [value] = -1 (unanswered) leaves every segment unselected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreSelector(
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = MAX_ANSWER + 1
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        for (i in 0..MAX_ANSWER) {
            SegmentedButton(
                selected = value == i,
                onClick = { onValue(i) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = count),
                icon = {}, // no check icon — keeps the 0/1/2/3 cells compact
                label = { Text("$i") },
            )
        }
    }
}
