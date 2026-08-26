package com.rafambn.kmap.render

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GraphiteJvmIntegrationTest {
    @Test
    fun jvmSelectsGraphiteWhenContentIsCompatible() {
        assertNull(platformGraphiteIncompatibility())
        assertTrue(
            resolveGraphiteBackend(
                renderBackend = MapRenderBackend.Auto,
                contentIncompatibility = null,
                platformIncompatibility = platformGraphiteIncompatibility(),
                graphiteFailed = false,
            )
        )
    }

    @Test
    fun graphiteControllerStartsAndClosesOnJvm() {
        var fatalError: Throwable? = null
        val controller = GraphiteMapController { fatalError = it }

        controller.close()
        controller.close()

        assertNull(fatalError)
    }
}
