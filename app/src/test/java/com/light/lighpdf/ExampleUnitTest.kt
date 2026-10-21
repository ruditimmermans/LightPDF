package com.light.lighpdf

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun recentFile_creationAndFormat() {
        val now = System.currentTimeMillis()
        val recentFile = RecentFile(
            uriString = "content://com.android.providers.downloads.documents/document/123",
            name = "TestDocument.pdf",
            timestamp = now
        )

        assertEquals("TestDocument.pdf", recentFile.name)
        assertEquals("Just now", formatTimeAgo(now))
    }

    @Test
    fun timeAgo_formatting() {
        val now = System.currentTimeMillis()
        val tenMinutesAgo = now - (10 * 60 * 1000)
        val twoHoursAgo = now - (2 * 60 * 60 * 1000)

        assertEquals("10m ago", formatTimeAgo(tenMinutesAgo))
        assertEquals("2h ago", formatTimeAgo(twoHoursAgo))
    }
}
