package com.rafambn.kmap.render

import kotlin.test.Test
import kotlin.test.assertEquals

class GraphiteRecordingOrderTest {
    @Test
    fun completionOrderDoesNotChangeVisualInsertionOrder() {
        val completed = listOf(3 to "last", 1 to "second", 0 to "first", 2 to "third")

        assertEquals(
            listOf("first", "second", "third", "last"),
            completed.inVisualOrder().map { it.second },
        )
    }
}
