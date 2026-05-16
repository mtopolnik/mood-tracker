package org.mtopol.moodtracker.data

import org.junit.Test
import org.mtopol.moodtracker.domain.QUESTION_COUNT
import org.mtopol.moodtracker.domain.UNANSWERED
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The import path parses an untrusted, possibly hostile file, so the focus here
 * is round-trip fidelity plus rejection of every malformed shape with a clean
 * [BackupFormatException] (never a raw JSON/parse crash).
 */
class CloudyBackupTest {

    private fun day(date: String, vararg a: Int) =
        BackupDay(LocalDate.parse(date), a.toList())

    private val complete = (0 until QUESTION_COUNT).map { it % 4 }

    @Test
    fun roundTripPreservesDaysAndOrder() {
        val days = listOf(
            day("2026-05-14", *complete.toIntArray()),
            day("2026-05-16", *IntArray(QUESTION_COUNT) { UNANSWERED }),
        )
        val decoded = CloudyBackup.decode(CloudyBackup.encode(days, "2026-05-16T00:00:00Z"))
        assertEquals(days, decoded)
    }

    @Test
    fun unansweredSentinelSurvivesRoundTrip() {
        val partial = MutableList(QUESTION_COUNT) { UNANSWERED }.also { it[0] = 3; it[7] = 2 }
        val decoded = CloudyBackup.decode(
            CloudyBackup.encode(listOf(BackupDay(LocalDate.now(), partial)), "now"),
        )
        assertEquals(partial, decoded.single().answers)
    }

    @Test
    fun emptyExportDecodesToEmptyList() {
        assertTrue(CloudyBackup.decode(CloudyBackup.encode(emptyList(), "now")).isEmpty())
    }

    @Test
    fun toleratesUnknownAndMissingTopLevelKeys() {
        // No "exportedAt", plus an unknown key — still valid.
        val json = """{"format":"cloudy","extra":true,
            "days":[{"date":"2026-01-01","answers":${complete}}]}"""
        assertEquals(LocalDate.parse("2026-01-01"), CloudyBackup.decode(json).single().date)
    }

    @Test
    fun rejectsNonJson() {
        assertFailsWith<BackupFormatException> { CloudyBackup.decode("not json at all") }
    }

    @Test
    fun rejectsForeignJson() {
        assertFailsWith<BackupFormatException> {
            CloudyBackup.decode("""{"format":"something-else","days":[]}""")
        }
    }

    @Test
    fun rejectsNewerVersion() {
        assertFailsWith<BackupFormatException> {
            CloudyBackup.decode("""{"format":"cloudy","version":99,"days":[]}""")
        }
    }

    @Test
    fun rejectsWrongAnswerCount() {
        assertFailsWith<BackupFormatException> {
            CloudyBackup.decode(
                """{"format":"cloudy","days":[{"date":"2026-01-01","answers":[0,1,2]}]}""",
            )
        }
    }

    @Test
    fun rejectsOutOfRangeAnswer() {
        val bad = complete.toMutableList().also { it[3] = 7 }
        assertFailsWith<BackupFormatException> {
            CloudyBackup.decode(
                """{"format":"cloudy","days":[{"date":"2026-01-01","answers":${bad}}]}""",
            )
        }
    }

    @Test
    fun rejectsUnparseableDate() {
        assertFailsWith<BackupFormatException> {
            CloudyBackup.decode(
                """{"format":"cloudy","days":[{"date":"yesterday","answers":${complete}}]}""",
            )
        }
    }

    @Test
    fun rejectsMissingAnswers() {
        assertFailsWith<BackupFormatException> {
            CloudyBackup.decode("""{"format":"cloudy","days":[{"date":"2026-01-01"}]}""")
        }
    }
}
