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
    fun `returns default initial matrix on empty input`() {
        val availableUnits = listOf<String>()
        val history = listOf<UnitResult>()
        val modelInput = processor.toInitialTensorflowInput(PersonalizationInput(history, availableUnits))

        assertThat(
            modelInput.all {
                it.all {
                    it == 0f
                }
            }
        )
        assertThat(modelInput.size).isEqualTo(InputProcessor.INPUT_LENGTH)
        assertThat(
            modelInput.all {
                it.size == contentIdMapping.size + 1
            }
        )
    }

    @Test
    fun `returns correct initial matrix when history has two units`() {
        val availableUnits = listOf<String>()
        val history = listOf(
            UnitResult(
                "Eidu:052a4fcf-c1ea-4ce5-8c86-31f37a90d72d/CardBox4",
                UnitResultType.Success,
                0,
                1.0F
            ),
            UnitResult(
                "Eidu:052a4fcf-c1ea-4ce5-8c86-31f37a90d72d/CardBox4",
                UnitResultType.Abort,
                10,
                0.5F
            )
        )
        val modelInput = processor.toInitialTensorflowInput(PersonalizationInput(history, availableUnits))

        val expectedFirstRow = FloatArray(contentIdMapping.size + 1)
        expectedFirstRow[1] = 1f
        expectedFirstRow[contentIdMapping.size] = 1f

        assertThat(modelInput[InputProcessor.HISTORY_LENGTH - 2]).isEqualTo(expectedFirstRow)

        val expectedSecondRow = FloatArray(contentIdMapping.size + 1)
        expectedSecondRow[1] = 1f
        expectedSecondRow[contentIdMapping.size] = 0f

        assertThat(modelInput[InputProcessor.HISTORY_LENGTH - 1]).isEqualTo(expectedSecondRow)

        val expectedDefaultRow = FloatArray(contentIdMapping.size + 1) { 0f }

        assertThat(
            modelInput.slice(0..InputProcessor.HISTORY_LENGTH - 3)
        ).each {
            it.isEqualTo(expectedDefaultRow)
        }

        assertThat(
            modelInput.slice(
                InputProcessor.HISTORY_LENGTH..InputProcessor.INPUT_LENGTH - 1
            )
        ).each {
            it.isEqualTo(expectedDefaultRow)
        }
    }

    @Test
    fun `returns correct initial matrix when history is long`() {
        val availableUnits = listOf<String>()
        val subHistory = listOf(
            UnitResult(
                "Eidu:052a4fcf-c1ea-4ce5-8c86-31f37a90d72d/CardBox4",
                UnitResultType.Success,
                0,
                1.0F
            ),
            UnitResult(
                "Eidu:052a4fcf-c1ea-4ce5-8c86-31f37a90d72d/CardBox4",
                UnitResultType.Abort,
                10,
                0.5F
            )
        )
        val history = (1..100).flatMap { subHistory }
        val modelInput = processor.toInitialTensorflowInput(PersonalizationInput(history, availableUnits))
        assertThat(
            modelInput.all {
                it.all {
                    it != -1f
                }
            }
        )
        val expectedFirstRow = FloatArray(contentIdMapping.size + 1)
        expectedFirstRow[1] = 1f
        expectedFirstRow[contentIdMapping.size] = 1f
        assertThat(modelInput.slice(0..InputProcessor.HISTORY_LENGTH - 1 step 2)).each {
            it.isEqualTo(expectedFirstRow)
        }

        val expectedSecondRow = FloatArray(contentIdMapping.size + 1)
        expectedSecondRow[1] = 1f
        expectedSecondRow[contentIdMapping.size] = 0f
        assertThat(modelInput.slice(1..InputProcessor.HISTORY_LENGTH - 1 step 2)).each {
            it.isEqualTo(expectedSecondRow)
        }

        val expectedDefaultRow = FloatArray(contentIdMapping.size + 1) { 0f }
        assertThat(
            modelInput.slice(
                InputProcessor.HISTORY_LENGTH..InputProcessor.INPUT_LENGTH - 1
            )
        ).each {
            it.isEqualTo(expectedDefaultRow)
        }
    }
}
