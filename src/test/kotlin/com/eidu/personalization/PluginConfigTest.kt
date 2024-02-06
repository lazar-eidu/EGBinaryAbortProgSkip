package com.eidu.personalization
import assertk.assertThat
import assertk.assertions.isTrue
import org.junit.Test
import java.nio.file.Files

class PluginConfigTest {
    @Test
    fun `reads config from file`() {
        val resourcesFolder = PluginConfig.configFile.parentFile.toPath()
        val config = PluginConfig.config

        val mappingPath = resourcesFolder.resolve(config.contentMappingPath.drop(1))
        val mappingExists = Files.exists(mappingPath)
        assertThat(mappingExists).isTrue()

        val modelPath = resourcesFolder.resolve(config.modelPath)
        val modelExists = Files.exists(modelPath)
        assertThat(modelExists).isTrue()
    }
}
