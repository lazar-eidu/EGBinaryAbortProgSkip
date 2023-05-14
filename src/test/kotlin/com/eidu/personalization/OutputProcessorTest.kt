package com.eidu.personalization

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.eidu.personalization.api.PersonalizationInput
import com.eidu.personalization.api.UnitResult
import org.junit.Test

class OutputProcessorTest {
    private val contentIdMapping = ContentIdMapping.readContentIdMapping()
    private val processor = OutputProcessor(contentIdMapping)

    @Test
    fun `returns empty list if no units available`() {
        val modelOutput = `given tensorflow output`()
        val availableUnits = listOf<String>()
        val history = listOf<UnitResult>()

        val pluginOutput =
            processor.fromTensorflowOutput(PersonalizationInput(history, availableUnits), modelOutput)

        assertThat(pluginOutput).isEmpty()
    }

    @Test
    fun `returns single available unit`() {
        val modelOutput = `given tensorflow output`()
        val availableUnits =
            listOf("Anton:/../c-mat-1-us/topic-01-numbers-and-counting/block-01-review-counting-1-to-10/level-02")
        val history = listOf<UnitResult>()

        val pluginOutput =
            processor.fromTensorflowOutput(PersonalizationInput(history, availableUnits), modelOutput)

        assertThat(pluginOutput.keys).isEqualTo(availableUnits.toSet())
    }

    private fun `given tensorflow output`(
        vararg probabilities: Pair<String, Float>
    ): FloatArray {
        val output = FloatArray(contentIdMapping.size)
        probabilities.forEach { (contentId, probability) ->
            output[contentIdMapping.indexOfFirst { it.contentId == contentId }] = probability
        }
        return output
    }
}
