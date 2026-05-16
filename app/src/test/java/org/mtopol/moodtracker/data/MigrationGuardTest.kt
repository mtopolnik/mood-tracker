package org.mtopol.moodtracker.data

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * CI tripwire that makes a data-destroying schema change impossible to land by
 * accident. With real users and no `fallbackToDestructiveMigration`, a version
 * bump that ships without a migration would crash on open for everyone.
 *
 * This does NOT prove a migration is *correct* — that needs an instrumented
 * `MigrationTestHelper` test on a device. It proves the *process* was followed:
 * version bumped ⇒ schema JSON exported & committed ⇒ a Migration registered
 * for every step.
 */
class MigrationGuardTest {

    // Unit tests run with the module dir as the working dir; tolerate the repo
    // root too so this passes regardless of how the build is invoked.
    private val schemaDir: File =
        listOf(
            File("schemas/org.mtopol.moodtracker.data.MoodDatabase"),
            File("app/schemas/org.mtopol.moodtracker.data.MoodDatabase"),
        ).firstOrNull { it.isDirectory }
            ?: fail("Exported Room schema dir not found — has exportSchema/KSP broken?")

    private fun exportedVersions(): List<Int> =
        schemaDir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { it.nameWithoutExtension.toIntOrNull() }
            ?.sorted()
            .orEmpty()

    @Test
    fun baselineSchemaStaysCommitted() {
        assertTrue(
            File(schemaDir, "1.json").isFile,
            "The v1 schema baseline must stay committed — every migration diffs against it.",
        )
    }

    @Test
    fun exportedSchemaMatchesDeclaredVersion() {
        // Bumping MoodDatabase.VERSION without committing the matching
        // schemas/<version>.json (or the reverse) fails here, before release.
        assertEquals(
            MoodDatabase.VERSION,
            exportedVersions().maxOrNull(),
            "Highest exported schema must equal MoodDatabase.VERSION. " +
                "Build the app and commit the regenerated schemas/<version>.json.",
        )
    }

    @Test
    fun everyVersionStepHasARegisteredMigration() {
        for (target in 2..MoodDatabase.VERSION) {
            val covered = MoodDatabase.MIGRATIONS.any {
                it.startVersion == target - 1 && it.endVersion == target
            }
            assertTrue(
                covered,
                "No Room Migration(${target - 1}, $target) registered in " +
                    "MoodDatabase.MIGRATIONS — existing users would lose data on upgrade.",
            )
        }
    }
}
