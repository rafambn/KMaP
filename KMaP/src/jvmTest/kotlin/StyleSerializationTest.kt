import com.rafambn.kmap.utils.style.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.*
import java.io.File

class StyleSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        useArrayPolymorphism = false
    }

    @Test
    fun testStyleRoundTrip() {
        // Load the style.json file from resources
        val styleFile = File("src/jvmTest/resources/style/style.json")
        assertTrue(styleFile.exists(), "Style file should exist at src/jvmTest/resources/style/style.json")

        val originalJsonString = styleFile.readText()
        assertNotNull(originalJsonString)
        assertTrue(originalJsonString.isNotEmpty())

        // Deserialize the JSON to a Style object
        val styleObject = json.decodeFromString<Style>(originalJsonString)
        assertNotNull(styleObject)

        // Verify basic properties
        assertEquals(8, styleObject.version)
        assertEquals("Streets", styleObject.name)
        assertTrue(styleObject.sources.isNotEmpty())
        assertTrue(styleObject.layers.isNotEmpty())

        // Serialize back to JSON
        val serializedJsonString = json.encodeToString(styleObject)
        assertNotNull(serializedJsonString)
        assertTrue(serializedJsonString.isNotEmpty())

        // Deserialize again to verify round trip
        val roundTripStyleObject = json.decodeFromString<Style>(serializedJsonString)
        assertNotNull(roundTripStyleObject)

        // Verify the round trip preserved the essential structure
        assertEquals(styleObject.version, roundTripStyleObject.version)
        assertEquals(styleObject.name, roundTripStyleObject.name)
        assertEquals(styleObject.sources.size, roundTripStyleObject.sources.size)
        assertEquals(styleObject.layers.size, roundTripStyleObject.layers.size)

        println("[DEBUG_LOG] Successfully completed style round trip test")
        println("[DEBUG_LOG] Original style has ${styleObject.sources.size} sources and ${styleObject.layers.size} layers")
    }
}
