@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, kotlinx.serialization.ExperimentalSerializationApi::class)

import com.rafambn.kmap.source.preparation.PreparationStyle
import com.rafambn.kmap.source.preparation.PreparedTile
import com.rafambn.kmap.source.preparation.TilePreparation
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.js.JsAny

/** Separate entry point: does not start Compose or touch window/document. */
fun main() {
    var revision: String? = null
    var preparation: TilePreparation? = null
    listen { kind, id, styleRevision, definition, zoom, row, col, buffer ->
        try {
            when (kind) {
                "install-style" -> {
                    preparation = TilePreparation(Json.decodeFromString(PreparationStyle.serializer(), definition))
                    revision = styleRevision
                    reply("style-ready", "", styleRevision, null, "")
                }
                "prepare" -> {
                    check(revision == styleRevision && preparation != null) { "Preparation style is not installed" }
                    val input = view(requireNotNull(buffer))
                    val bytes = ByteArray(size(input)) { byteAt(input, it).toByte() }
                    val prepared = preparation.prepare(bytes, zoom, row, col)
                    val encoded = ProtoBuf.encodeToByteArray(PreparedTile.serializer(), prepared)
                    val output = allocate(encoded.size)
                    encoded.forEachIndexed { index, byte -> put(output, index, byte.toInt()) }
                    reply("prepared", id, styleRevision, output, "")
                }
            }
        } catch (error: Exception) {
            reply(if (kind == "prepare") "failed" else "fatal", id, styleRevision, null, error.message ?: "Tile preparation failed")
        }
    }
    reply("ready", "", "", null, "1")
}

private fun listen(callback: (String, String, String, String, Int, Int, Int, JsAny?) -> Unit): Unit = js("""{
    self.onmessage = event => { const m=event.data; callback(m.kind || '',m.id || '',m.revision || '',m.definition || '',m.zoom || 0,m.row || 0,m.col || 0,m.payload || null); };
}""")
private fun reply(kind: String, id: String, revision: String, payload: JsAny?, message: String): Unit = js("""{
    self.postMessage({kind:kind,id:id,revision:revision,payload:payload ? payload.buffer : null,message:message,execution:'dedicated-worker'},payload ? [payload.buffer] : []);
}""")
private fun view(buffer: JsAny): JsAny = js("new Uint8Array(buffer)")
private fun size(buffer: JsAny): Int = js("buffer.length")
private fun byteAt(buffer: JsAny, index: Int): Int = js("buffer[index]")
private fun allocate(size: Int): JsAny = js("new Uint8Array(size)")
private fun put(buffer: JsAny, index: Int, value: Int): Unit = js("buffer[index]=value")
