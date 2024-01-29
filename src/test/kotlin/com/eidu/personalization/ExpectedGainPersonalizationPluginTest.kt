package com.eidu.personalization

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.eidu.personalization.api.PersonalizationInput
import org.junit.Test

class ExpectedGainPersonalizationPluginTest {

    private val plugin = PluginImplementation()
    private val contentIdMapping = ContentIdMapping.readContentIdMapping().associateBy { it.contentId }

    @Test
    fun `returns unit with good score when available`() {
        val pluginOutput = plugin.determineNextUnits(PersonalizationInput(listOf(), listOf(FIRST_UNIT))) { _, _, output ->
            `given plugin probability values`(
                output,
                FIRST_UNIT to 0.8f
            )
        }

        assertThat(pluginOutput.nextUnits).isEqualTo(listOf(FIRST_UNIT))
    }

    private fun `given plugin probability values`(output: Any, vararg scores: Pair<String, Float>) {
        @Suppress("UNCHECKED_CAST") val outputArray = output as Array<FloatArray>
        outputArray.forEach {
            scores.forEach { (contentId, score) ->
                val outputIndex = contentIdMapping[contentId]?.index ?: error("No Mapping")
                it[outputIndex] = score
            }
        }
    }

    companion object {
        private const val FIRST_UNIT =
            "Eidu:052a4fcf-c1ea-4ce5-8c86-31f37a90d72d/CardBox4"
        private const val SECOND_UNIT =
            "Eidu:052a4fcf-c1ea-4ce5-8c86-31f37a90d72d/Cargo1"
        private const val THIRD_UNIT =
            "Eidu:052a4fcf-c1ea-4ce5-8c86-31f37a90d72d/CargoExercise02"
        private const val FOURTH_UNIT =
            "Eidu:052a4fcf-c1ea-4ce5-8c86-31f37a90d72d/CargoExercise03"
    }
}
