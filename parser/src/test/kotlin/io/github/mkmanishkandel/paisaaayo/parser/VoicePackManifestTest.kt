package io.github.mkmanishkandel.paisaaayo.parser

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** Keeps tools/voice/clips.csv (the recording script for voice artists) in sync with the code. */
class VoicePackManifestTest {
    @Test fun clipsCsvMatchesCode() {
        val csv = File("../tools/voice/clips.csv").readLines().drop(1).filter { it.isNotBlank() }
            .associate { line -> line.substringBefore(',') to line.substringAfter(',') }
        assertEquals(Speech.ALL_CLIPS, csv)
    }
}
