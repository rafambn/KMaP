package com.rafambn.kmap.style

import de.infix.testBalloon.framework.core.testSuite
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

val StyleSerializationTest by testSuite {
    testFixture {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            useArrayPolymorphism = false
        }
    } asParameterForEach {
        test("testStyleRoundTrip") { json ->
            val styleFile = File("testResources@jvm/style/style12.json")
            assertTrue(styleFile.exists(), "Style file should exist at testResources@jvm/style/style12.json")

            val originalJsonString = styleFile.readText()
            assertNotNull(originalJsonString)
            assertTrue(originalJsonString.isNotEmpty())

            val styleObject = json.decodeFromString<Style>(originalJsonString)
            assertNotNull(styleObject)

            assertEquals(8, styleObject.version)
            assertEquals("Streets v4", styleObject.name)
            assertTrue(styleObject.sources.isNotEmpty())
            assertTrue(styleObject.layers.isNotEmpty())

            val serializedJsonString = json.encodeToString(styleObject)
            assertNotNull(serializedJsonString)
            assertTrue(serializedJsonString.isNotEmpty())

            val roundTripStyleObject = json.decodeFromString<Style>(serializedJsonString)
            assertNotNull(roundTripStyleObject)

            assertEquals(styleObject.version, roundTripStyleObject.version)
            assertEquals(styleObject.name, roundTripStyleObject.name)
            assertEquals(styleObject.sources.size, roundTripStyleObject.sources.size)
            assertEquals(styleObject.layers.size, roundTripStyleObject.layers.size)
        }
    }
}
