package com.eidu.personalization

import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.isEqualTo
import com.eidu.personalization.api.PersonalizationInput
import com.eidu.personalization.api.UnitResult
import com.eidu.personalization.api.UnitResultType
import org.junit.Test

class InputProcessorTest {
    private val contentIdMapping = ContentIdMapping.readContentIdMapping()
    private val processor = InputProcessor(contentIdMapping)

    @Test
    fun `returns default float matrix on empty input`() {
        val availableUnits = listOf<String>()
        val history = listOf<UnitResult>()
        val modelInput = processor.toTensorflowInput(PersonalizationInput(history, availableUnits))

        assertThat(
            modelInput[0].all {
                it.all {
                    it == -1f
                }
            }
        )
        assertThat(modelInput[0].size).isEqualTo(InputProcessor.HISTORY_LENGTH)
        assertThat(
            modelInput[0].all {
                it.size == contentIdMapping.size + 1
            }
        )
    }

    @Test
    fun `returns correct float matrix when history has two units`() {
        val availableUnits = listOf<String>()
        val history = listOf(
            UnitResult(
                "Anton:/../c-mat-1-us/topic-01-numbers-and-counting/block-01-review-counting-1-to-10/level-02",
                UnitResultType.Success,
                0,
                1.0F
            ),
            UnitResult(
                "Anton:/../c-mat-1-us/topic-01-numbers-and-counting/block-01-review-counting-1-to-10/level-02",
                UnitResultType.Abort,
                10,
                0F
            )
        )
        val modelInput = processor.toTensorflowInput(PersonalizationInput(history, availableUnits))

        val expectedFirstRow = FloatArray(contentIdMapping.size + 1)
        expectedFirstRow[1] = 1f
        expectedFirstRow[contentIdMapping.size] = 1f
        assertThat(modelInput[0][0]).isEqualTo(expectedFirstRow)

        val expectedSecondRow = FloatArray(contentIdMapping.size + 1)
        expectedSecondRow[1] = 1f
        expectedSecondRow[contentIdMapping.size] = 0f
        assertThat(modelInput[0][1]).isEqualTo(expectedSecondRow)

        val expectedDefaultRow = FloatArray(contentIdMapping.size + 1) { -1f }
        assertThat(modelInput[0].slice(2..InputProcessor.HISTORY_LENGTH - 1)).each {
            it.isEqualTo(expectedDefaultRow)
        }
    }

    @Test
    fun `returns correct float matrix when history is long`() {
        val availableUnits = listOf<String>()
        val subHistory = listOf(
            UnitResult(
                "Anton:/../c-mat-1-us/topic-01-numbers-and-counting/block-01-review-counting-1-to-10/level-02",
                UnitResultType.Success,
                0,
                1.0F
            ),
            UnitResult(
                "Anton:/../c-mat-1-us/topic-01-numbers-and-counting/block-01-review-counting-1-to-10/level-02",
                UnitResultType.Abort,
                10,
                0F
            )
        )
        val history = (1..100).flatMap { subHistory }
        val modelInput = processor.toTensorflowInput(PersonalizationInput(history, availableUnits))

        val expectedFirstRow = FloatArray(contentIdMapping.size + 1)
        expectedFirstRow[1] = 1f
        expectedFirstRow[contentIdMapping.size] = 1f
        assertThat(modelInput[0].slice(0..InputProcessor.HISTORY_LENGTH - 1 step 2)).each {
            it.isEqualTo(expectedFirstRow)
        }

        val expectedSecondRow = FloatArray(contentIdMapping.size + 1)
        expectedSecondRow[1] = 1f
        expectedSecondRow[contentIdMapping.size] = 0f
        assertThat(modelInput[0].slice(1..InputProcessor.HISTORY_LENGTH - 1 step 2)).each {
            it.isEqualTo(expectedSecondRow)
        }
    }
}
