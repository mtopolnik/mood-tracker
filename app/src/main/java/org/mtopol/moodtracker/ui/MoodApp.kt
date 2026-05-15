package org.mtopol.moodtracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.mtopol.moodtracker.MoodViewModel
import org.mtopol.moodtracker.R
import org.mtopol.moodtracker.Tab

/**
 * Root UI. Screen switching is plain Compose state (no navigation library),
 * matching the project's convention. A non-null editor date shows the
 * full-screen past-day editor over everything else.
 */
@Composable
fun MoodApp(viewModel: MoodViewModel) {
    val tab by viewModel.tab.collectAsState()
    val editorDate by viewModel.editorDate.collectAsState()

    if (editorDate != null) {
        val questionnaire by viewModel.questionnaire.collectAsState()
        QuestionnaireScreen(
            state = questionnaire,
            onAnswer = viewModel::setAnswer,
            showBack = true,
            onBack = viewModel::closeEditor,
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.TODAY,
                    onClick = { viewModel.selectTab(Tab.TODAY) },
                    icon = { Icon(Icons.Filled.EditCalendar, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_today)) },
                )
                NavigationBarItem(
                    selected = tab == Tab.HISTORY,
                    onClick = { viewModel.selectTab(Tab.HISTORY) },
                    icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_history)) },
                )
                NavigationBarItem(
                    selected = tab == Tab.TRENDS,
                    onClick = { viewModel.selectTab(Tab.TRENDS) },
                    icon = { Icon(Icons.Filled.ShowChart, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_trends)) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                Tab.TODAY -> {
                    val questionnaire by viewModel.questionnaire.collectAsState()
                    QuestionnaireScreen(
                        state = questionnaire,
                        onAnswer = viewModel::setAnswer,
                        showBack = false,
                        onBack = null,
                    )
                }

                Tab.HISTORY -> {
                    val history by viewModel.history.collectAsState()
                    HistoryScreen(state = history, onOpen = viewModel::openEditor)
                }

                Tab.TRENDS -> {
                    val trends by viewModel.trends.collectAsState()
                    TrendsScreen(state = trends, onRange = viewModel::setRange)
                }
            }
        }
    }
}
