package org.mtopol.moodtracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.mtopol.moodtracker.CloudyViewModel
import org.mtopol.moodtracker.R
import org.mtopol.moodtracker.Tab

/**
 * Root UI. Screen switching is plain Compose state (no navigation library),
 * matching the project's convention. A non-null editor date shows the
 * full-screen past-day editor over everything else.
 */
@Composable
fun CloudyApp(viewModel: CloudyViewModel) {
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
                    icon = { Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_trends)) },
                )
                NavigationBarItem(
                    selected = tab == Tab.BACKUP,
                    onClick = { viewModel.selectTab(Tab.BACKUP) },
                    icon = { Icon(Icons.Filled.Backup, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_backup)) },
                )
            }
        },
    ) { padding ->
        when (tab) {
            Tab.TODAY -> {
                // Today owns the status bar itself (its header applies
                // statusBarsPadding), so drop the top inset here — otherwise
                // the Scaffold reserves it as empty space above the header.
                val layoutDirection = LocalLayoutDirection.current
                val todayPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    end = padding.calculateEndPadding(layoutDirection),
                    top = 0.dp,
                    bottom = padding.calculateBottomPadding(),
                )
                val questionnaire by viewModel.questionnaire.collectAsState()
                Box(Modifier.padding(todayPadding)) {
                    QuestionnaireScreen(
                        state = questionnaire,
                        onAnswer = viewModel::setAnswer,
                        showBack = false,
                        onBack = null,
                    )
                }
            }

            Tab.HISTORY -> {
                val history by viewModel.history.collectAsState()
                Box(Modifier.padding(padding)) {
                    HistoryScreen(state = history, onOpen = viewModel::openEditor)
                }
            }

            Tab.TRENDS -> {
                val trends by viewModel.trends.collectAsState()
                Box(Modifier.padding(padding)) {
                    TrendsScreen(state = trends, onRange = viewModel::setRange)
                }
            }

            Tab.BACKUP -> {
                Box(Modifier.padding(padding)) {
                    BackupScreen(
                        onExportJson = viewModel::exportJson,
                        onImportJson = viewModel::importJson,
                    )
                }
            }
        }
    }
}
