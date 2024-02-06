package com.eidu.personalization
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class PluginConfig(val contentMappingPath: String, val modelPath: String) {
    companion object {
        private val classLoader = PluginConfig::class.java.classLoader
        val configPath: String = classLoader.getResource("config.json")?.file ?: error("config.json not found")
        val config: PluginConfig = Json.decodeFromString<PluginConfig>(
            File(configPath).readText()
        )
    }
}
