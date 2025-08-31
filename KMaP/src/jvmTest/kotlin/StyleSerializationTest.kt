import com.rafambn.kmap.utils.style.*
import kotlinx.serialization.json.Json
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
        val styleFile = File("src/jvmTest/resources/style/style.json")
        assertTrue(styleFile.exists(), "Style file should exist at src/jvmTest/resources/style/style.json")

        val originalJsonString = styleFile.readText()
        assertNotNull(originalJsonString)
        assertTrue(originalJsonString.isNotEmpty())

        val styleObject = json.decodeFromString<Style>(originalJsonString)
        assertNotNull(styleObject)

        assertEquals(8, styleObject.version)
        assertEquals("Streets", styleObject.name)
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
