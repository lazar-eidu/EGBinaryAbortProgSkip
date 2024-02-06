package com.eidu.personalization

import com.eidu.personalization.util.getResourceAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PluginConfig(val contentMappingPath: String, val modelPath: String) {
    companion object {
        val config: PluginConfig = Json.decodeFromString<PluginConfig>(
            getResourceAsText("/config.json")
        )
    }
}
