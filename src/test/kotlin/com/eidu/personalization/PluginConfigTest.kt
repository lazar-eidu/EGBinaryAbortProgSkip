package com.eidu.personalization
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import org.junit.Test
import assertk.assertThat
import java.nio.file.Files
import java.nio.file.Paths

class PluginConfigTest {
    @Test
    fun `reads config from file`() {
        val config = PluginConfig.config
        val mappingPath = Paths.get(config.contentMappingPath)
        val mappingExists = Files.exists(mappingPath)
        assertThat(mappingExists).isTrue()
//        assertThat(config.contentMappingPath).isEqualTo("content_id_lookup_v1.0.3.csv")
//        assertThat(config.modelPath).isEqualTo("dkt230712_base_tail_v1.tflite")
    }
}