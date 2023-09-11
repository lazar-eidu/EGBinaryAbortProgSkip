package com.eidu.personalization
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class PluginConfig(val contentMappingPath: String, val modelPath: String) {
    companion object {
        val config: PluginConfig = Json.decodeFromString<PluginConfig>(
            File("src/main/resources/config.json").readText()
        )
    }
}