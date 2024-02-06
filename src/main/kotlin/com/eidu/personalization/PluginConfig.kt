package com.eidu.personalization
import com.eidu.personalization.util.getResourceAsFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class PluginConfig(val contentMappingPath: String, val modelPath: String) {
    companion object {
        val configFile: File = getResourceAsFile("/config.json")
        val config: PluginConfig = Json.decodeFromString<PluginConfig>(
            configFile.readText()
        )
    }
}
