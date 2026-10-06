@file:OptIn(kotlin.js.ExperimentalJsExport::class, kotlin.js.ExperimentalWasmJsInterop::class,
    kotlinx.serialization.ExperimentalSerializationApi::class)

import com.rafambn.kmap.source.preparation.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.js.JsAny
import kotlin.js.JsExport

/** Browser benchmark entry point. Import this file directly, without starting the worker application. */
@JsExport
fun createLocalPreparation(definition: String): (JsAny, Int, Int, Int) -> JsAny {
    val preparation = TilePreparation(Json.decodeFromString(PreparationStyle.serializer(), definition))
    return { input, zoom, row, col ->
        val view = viewBytes(input)
        val bytes = ByteArray(byteSize(view)) { getByte(view, it).toByte() }
        val prepared = preparation.prepare(bytes, zoom, row, col)
        val encoded = ProtoBuf.encodeToByteArray(PreparedTile.serializer(), prepared)
        val output = allocateBytes(encoded.size)
        encoded.forEachIndexed { index, byte -> setByte(output, index, byte.toInt()) }
        toBuffer(output)
    }
}

private fun viewBytes(input: JsAny): JsAny = js("new Uint8Array(input)")
private fun byteSize(input: JsAny): Int = js("input.length")
private fun getByte(input: JsAny, index: Int): Int = js("input[index]")
private fun allocateBytes(size: Int): JsAny = js("new Uint8Array(size)")
private fun setByte(output: JsAny, index: Int, value: Int): Unit = js("output[index]=value")
private fun toBuffer(output: JsAny): JsAny = js("output.buffer")
