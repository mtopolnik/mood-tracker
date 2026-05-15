package org.mtopol.moodtracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.mtopol.moodtracker.data.MoodDatabase
import org.mtopol.moodtracker.data.MoodRepository
import org.mtopol.moodtracker.domain.ChartRange
import org.mtopol.moodtracker.domain.DailyPoint
import org.mtopol.moodtracker.domain.QUESTION_COUNT
import org.mtopol.moodtracker.domain.UNANSWERED
import org.mtopol.moodtracker.domain.anxietyScore
import org.mtopol.moodtracker.domain.depressionScore
import org.mtopol.moodtracker.domain.interpolateDaily
import org.mtopol.moodtracker.domain.isComplete
import org.mtopol.moodtracker.domain.resolveRange
import org.mtopol.moodtracker.reminder.ReminderScheduler
import java.time.LocalDate

const val HISTORY_DAYS = 30

enum class Tab { TODAY, HISTORY, TRENDS }

data class QuestionnaireUiState(
    val date: LocalDate,
    val isToday: Boolean,
    val answers: List<Int>,
    val isComplete: Boolean,
    /** Running scores: grow as items are answered, final once [isComplete]. */
    val anxiety: Int,
    val depression: Int,
)

data class HistoryRow(
    val date: LocalDate,
    val present: Boolean,
    val anxiety: Int?,
    val depression: Int?,
)

data class HistoryUiState(
    val rows: List<HistoryRow> = emptyList(),
    val missedCount: Int = 0,
    val totalDays: Int = HISTORY_DAYS,
)

data class TrendsUiState(
    val range: ChartRange = ChartRange.MONTH,
    val startEpochDay: Long = 0L,
    val endEpochDay: Long = 0L,
    val anxiety: List<DailyPoint> = emptyList(),
    val depression: List<DailyPoint> = emptyList(),
    val hasData: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MoodViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MoodRepository(MoodDatabase.get(app).moodDao())
    private val reminders = ReminderScheduler(app)

    /** Captured once at creation; the process is short-lived and relaunched daily. */
    private val today: LocalDate = LocalDate.now()

    private val _tab = MutableStateFlow(Tab.TODAY)
    val tab: StateFlow<Tab> = _tab.asStateFlow()

    /** Non-null → show the full-screen past-day editor over the History tab. */
    private val _editorDate = MutableStateFlow<LocalDate?>(null)
    val editorDate: StateFlow<LocalDate?> = _editorDate.asStateFlow()

    private val _questionnaire = MutableStateFlow(emptyQuestionnaire(today))
    val questionnaire: StateFlow<QuestionnaireUiState> = _questionnaire.asStateFlow()

    val history: StateFlow<HistoryUiState> =
        repo.observeRange(
            today.minusDays((HISTORY_DAYS - 1).toLong()).toEpochDay(),
            today.toEpochDay(),
        ).map { days ->
            val byDate = days.associateBy { it.date }
            val rows = (0 until HISTORY_DAYS).map { i ->
                val date = today.minusDays(i.toLong()) // i = 0 → today (newest first)
                // A day "counts" only when fully answered; a partially saved
                // day still reads as missed (and reopens with its progress).
                val record = byDate[date]?.takeIf { it.isComplete }
                HistoryRow(date, record != null, record?.anxiety, record?.depression)
            }
            HistoryUiState(rows, rows.count { !it.present }, HISTORY_DAYS)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    private val _range = MutableStateFlow(ChartRange.MONTH)

    val trends: StateFlow<TrendsUiState> =
        _range.flatMapLatest { range ->
            repo.observeEarliestEpochDay().flatMapLatest { earliest ->
                val window = resolveRange(range, LocalDate.now(), earliest)
                repo.observeRange(window.startEpochDay, window.endEpochDay).map { days ->
                    // Only fully answered days are plotted; partial days never
                    // pollute the line or its interpolation.
                    val complete = days.filter { it.isComplete }
                    TrendsUiState(
                        range = range,
                        startEpochDay = window.startEpochDay,
                        endEpochDay = window.endEpochDay,
                        anxiety = interpolateDaily(
                            complete.map { it.date.toEpochDay() to it.anxiety },
                            window.startEpochDay,
                            window.endEpochDay,
                        ),
                        depression = interpolateDaily(
                            complete.map { it.date.toEpochDay() to it.depression },
                            window.startEpochDay,
                            window.endEpochDay,
                        ),
                        hasData = complete.isNotEmpty(),
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrendsUiState())

    init {
        loadQuestionnaire(today)
        reminders.ensureScheduled()
    }

    fun selectTab(tab: Tab) {
        _editorDate.value = null
        _tab.value = tab
        if (tab == Tab.TODAY) loadQuestionnaire(LocalDate.now())
    }

    fun openEditor(date: LocalDate) {
        _editorDate.value = date
        loadQuestionnaire(date)
    }

    fun closeEditor() {
        _editorDate.value = null
    }

    /** Brings the app to today's questionnaire (used by the reminder deep-link). */
    fun openToday() {
        _editorDate.value = null
        _tab.value = Tab.TODAY
        loadQuestionnaire(LocalDate.now())
    }

    /**
     * Records the choice in memory (live bars update at once) and persists the
     * whole day immediately — there is no Save step. Each write is the full
     * 12-item snapshot keyed by day, so it is a plain upsert; leaving the
     * past-day editor is just the back arrow ([closeEditor]).
     */
    fun setAnswer(index: Int, value: Int) {
        val state = _questionnaire.value
        val answers = state.answers.toMutableList().also { it[index] = value }
        _questionnaire.value = state.copy(
            answers = answers,
            isComplete = isComplete(answers),
            anxiety = anxietyScore(answers),
            depression = depressionScore(answers),
        )
        viewModelScope.launch { repo.upsert(state.date, answers) }
    }

    fun setRange(range: ChartRange) {
        _range.value = range
    }

    private fun emptyQuestionnaire(date: LocalDate) = QuestionnaireUiState(
        date = date,
        isToday = date == LocalDate.now(),
        answers = List(QUESTION_COUNT) { UNANSWERED },
        isComplete = false,
        anxiety = 0,
        depression = 0,
    )

    private fun loadQuestionnaire(date: LocalDate) {
        viewModelScope.launch {
            val record = repo.getDay(date)
            _questionnaire.value = if (record != null) {
                QuestionnaireUiState(
                    date = date,
                    isToday = date == LocalDate.now(),
                    answers = record.answers,
                    isComplete = record.isComplete,
                    anxiety = record.anxiety,
                    depression = record.depression,
                )
            } else {
                emptyQuestionnaire(date)
            }
        }
    }
}
