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
                platformIncompatibility = null,
                graphiteFailed = false,
            )
        )
    }

    @Test
    fun autoRequiresContentPlatformAndRuntime() {
        assertTrue(resolveGraphiteBackend(MapRenderBackend.Auto, null, null, false))
        assertFalse(resolveGraphiteBackend(MapRenderBackend.Auto, "content", null, false))
        assertFalse(resolveGraphiteBackend(MapRenderBackend.Auto, null, "platform", false))
        assertFalse(resolveGraphiteBackend(MapRenderBackend.Auto, null, null, true))
    }

    @Test
    fun forcedGraphiteReportsTheFirstResolvedIncompatibility() {
        val contentFailure = assertFailsWith<IllegalStateException> {
            resolveGraphiteBackend(MapRenderBackend.Graphite, "content", "platform", false)
        }
        kotlin.test.assertEquals("content", contentFailure.message)

        val platformFailure = assertFailsWith<IllegalStateException> {
            resolveGraphiteBackend(MapRenderBackend.Graphite, null, "platform", false)
        }
        kotlin.test.assertEquals("platform", platformFailure.message)
    }
}
