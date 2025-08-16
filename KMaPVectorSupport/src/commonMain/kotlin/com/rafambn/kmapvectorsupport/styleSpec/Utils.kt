package com.rafambn.kmapvectorsupport.styleSpec

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

private val module = SerializersModule {
    polymorphic(Source::class) {
        subclass(VectorSource::class)
        subclass(RasterSource::class)
        subclass(GeoJSONSource::class)
    }

    // Register Layer subclasses
    polymorphic(Layer::class) {
        subclass(BackgroundLayer::class)
        subclass(FillLayer::class)
        subclass(LineLayer::class)
        subclass(SymbolLayer::class)
        subclass(RasterLayer::class)
        subclass(CircleLayer::class)
        subclass(FillExtrusionLayer::class)
        subclass(HeatmapLayer::class)
        subclass(HillshadeLayer::class)
    }
}

private val jsonFormat = Json {
    serializersModule = module
    ignoreUnknownKeys = true
    isLenient = true
}

fun deserialize(styleJson: String): Style {
    return jsonFormat.decodeFromString(Style.serializer(), styleJson)
}

fun serialize(style: Style): String {
    return jsonFormat.encodeToString(Style.serializer(), style)
}
