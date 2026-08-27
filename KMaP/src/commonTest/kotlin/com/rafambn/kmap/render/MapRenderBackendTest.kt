package com.rafambn.kmap.render

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapRenderBackendTest {
    @Test
    fun composeNeverSelectsGraphite() {
        assertFalse(
            resolveGraphiteBackend(
                renderBackend = MapRenderBackend.Compose,
                contentIncompatibility = null,
                graphiteFailed = false,
            )
        )
    }

    @Test
    fun autoRequiresCompatibleContentAndRuntime() {
        assertTrue(resolveGraphiteBackend(MapRenderBackend.Auto, null, false))
        assertFalse(resolveGraphiteBackend(MapRenderBackend.Auto, "content", false))
        assertFalse(resolveGraphiteBackend(MapRenderBackend.Auto, null, true))
    }

    @Test
    fun forcedGraphiteReportsTheFirstResolvedIncompatibility() {
        val contentFailure = assertFailsWith<IllegalStateException> {
            resolveGraphiteBackend(MapRenderBackend.Graphite, "content", false)
        }
        kotlin.test.assertEquals("content", contentFailure.message)
    }
}
