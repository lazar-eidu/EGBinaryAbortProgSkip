package com.eidu.personalization

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.eidu.personalization.api.PersonalizationInput
import com.eidu.personalization.api.UnitResult
import com.eidu.personalization.api.UnitResultType
import org.junit.Test

class ExpectedGainPersonalizationPluginTest {

    private val plugin = Implementation()
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

    @Test
    fun `returns empty list if all processed units have a probability of success of over threshold value`() {
        val pluginOutput = plugin.determineNextUnits(
            PersonalizationInput(
                listOf(),
                listOf(FIRST_UNIT, SECOND_UNIT, THIRD_UNIT, FOURTH_UNIT)
            )
        ) { _, _, output ->
            `given plugin probability values`(
                output,
                FIRST_UNIT to 0.98f,
                SECOND_UNIT to 0.98f,
                THIRD_UNIT to 0.98f,
                FOURTH_UNIT to 0.98f,
            )
        }

        assertThat(pluginOutput.nextUnits).isEmpty()
    }

    @Test
    fun `filters successful units`() {
        val pluginOutput = plugin.determineNextUnits(
            PersonalizationInput(
                listOf(UnitResult(FIRST_UNIT, UnitResultType.Success, 0L, null)),
                listOf(FIRST_UNIT)
            )
        ) { _, _, output ->
            `given plugin probability values`(
                output,
                FIRST_UNIT to 0.98f
            )
        }

        assertThat(pluginOutput.nextUnits).isEmpty()
    }

    private fun `given plugin probability values`(output: Any, vararg scores: Pair<String, Float>) {
        @Suppress("UNCHECKED_CAST") val outputArray = output as Array<Array<FloatArray>>
        scores.forEach { (contentId, score) ->
            val outputIndex = contentIdMapping[contentId]?.index ?: error("No Mapping")
            outputArray[0][0][outputIndex] = score
        }
    }

    companion object {
        private const val FIRST_UNIT =
            "Anton:/../c-mat-1-us/topic-01-numbers-and-counting/block-01-review-counting-1-to-10/level-02"
        private const val SECOND_UNIT =
            "Anton:/../c-mat-1-us/topic-01-numbers-and-counting/block-01-review-counting-1-to-10/level-03"
        private const val THIRD_UNIT =
            "Anton:/../c-mat-1-us/topic-01-numbers-and-counting/block-02-review-counting-1-to-20/level-01"
        private const val FOURTH_UNIT =
            "Anton:/../c-mat-1-us/topic-01-numbers-and-counting/block-02-review-counting-1-to-20/level-03"
    }
}
