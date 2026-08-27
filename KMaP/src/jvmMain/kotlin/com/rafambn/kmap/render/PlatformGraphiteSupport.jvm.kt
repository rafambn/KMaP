package com.rafambn.kmap.render

internal actual fun platformGraphiteIncompatibility(): String? {
    val operatingSystem = System.getProperty("os.name").orEmpty()
    return if (
        operatingSystem.equals("Mac OS X", ignoreCase = true) ||
        operatingSystem.equals("Linux", ignoreCase = true)
    ) {
        null
    } else {
        "Graphite requires Metal on macOS or Vulkan on Linux for the JVM target"
    }
}
