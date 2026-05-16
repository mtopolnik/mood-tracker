package org.mtopol.moodtracker.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.mtopol.moodtracker.domain.MAX_ANSWER
import org.mtopol.moodtracker.domain.QUESTION_COUNT
import org.mtopol.moodtracker.domain.UNANSWERED
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** One day's answers in a portable, schema-independent shape (ISO date + 12 ints). */
data class BackupDay(val date: LocalDate, val answers: List<Int>)

/** Thrown when an imported file is not a readable Mood Tracker export. */
class BackupFormatException(message: String) : Exception(message)

/**
 * The shareable backup format — deliberately decoupled from the Room schema so a
 * file exported by one app version still imports after the (pre-1.0, destructive)
 * DB schema changes. This same JSON is what the user shares out for safekeeping,
 * so [decode] treats its input as untrusted: every field is validated, a hostile
 * size is rejected, and unknown/missing keys are tolerated for forward-compat.
 *
 * Uses the platform `org.json` so it adds no production dependency.
 */
object MoodBackup {

    /** Identifies the file as ours; a foreign JSON is rejected, not half-read. */
    const val FORMAT = "moodtracker"

    /** Bump when the shape changes; [decode] refuses a newer version than it knows. */
    const val VERSION = 1

    /** Defensive ceiling (~270 years of daily entries) so a hostile file can't OOM us. */
    private const val MAX_DAYS = 100_000

    fun encode(days: List<BackupDay>, exportedAt: String): String {
        val daysJson = JSONArray()
        for (day in days) {
            daysJson.put(
                JSONObject()
                    .put("date", day.date.toString()) // ISO-8601, e.g. 2026-05-16
                    .put("answers", JSONArray(day.answers)),
            )
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", exportedAt)
            .put("days", daysJson)
            .toString(2)
    }

    fun decode(text: String): List<BackupDay> {
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            throw BackupFormatException("Not a JSON document")
        }
        if (root.optString("format") != FORMAT) {
            throw BackupFormatException("Not a Mood Tracker export")
        }
        if (root.optInt("version", 1) > VERSION) {
            throw BackupFormatException("This file was made by a newer app version")
        }
        val daysJson = root.optJSONArray("days") ?: JSONArray()
        if (daysJson.length() > MAX_DAYS) {
            throw BackupFormatException("File has too many entries")
        }
        val out = ArrayList<BackupDay>(daysJson.length())
        for (i in 0 until daysJson.length()) {
            val obj = daysJson.optJSONObject(i)
                ?: throw BackupFormatException("Malformed entry at position $i")
            out += parseDay(obj, i)
        }
        return out
    }

    private fun parseDay(obj: JSONObject, position: Int): BackupDay {
        val date = try {
            LocalDate.parse(obj.optString("date"))
        } catch (e: DateTimeParseException) {
            throw BackupFormatException("Invalid date in entry at position $position")
        }
        val answersJson = obj.optJSONArray("answers")
            ?: throw BackupFormatException("Missing answers in entry for $date")
        if (answersJson.length() != QUESTION_COUNT) {
            throw BackupFormatException("Entry for $date must have $QUESTION_COUNT answers")
        }
        val answers = (0 until QUESTION_COUNT).map { idx ->
            // UNANSWERED is a legal stored value (an in-progress day); 0..MAX is a
            // real answer. Anything else means the file is corrupt or not ours.
            answersJson.optInt(idx, Int.MIN_VALUE).also { v ->
                if (v != UNANSWERED && v !in 0..MAX_ANSWER) {
                    throw BackupFormatException("Out-of-range answer in entry for $date")
                }
            }
        }
        return BackupDay(date, answers)
    }
}
