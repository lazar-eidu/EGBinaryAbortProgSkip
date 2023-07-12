package com.eidu.personalization

import com.eidu.personalization.api.PersonalizationInput
import com.eidu.personalization.api.UnitResult
import com.eidu.personalization.api.UnitResultType

class InputProcessor(contentIdMappings: List<ContentIdMapping>) {
    private val mappings: Map<String, ContentIdMapping> = contentIdMappings.associateBy { it.contentId }

    fun toInitialTensorflowInput(input: PersonalizationInput): Array<FloatArray> {
        val inputArray = Array(INPUT_LENGTH) { FloatArray(mappings.size + 1) { 0f } }
        val trimmedHistory = input.learningHistory.takeLast(HISTORY_LENGTH)

        trimmedHistory.forEachIndexed { historyIndex, result ->
            val inputRow = getInputRow(result)
            inputArray[HISTORY_LENGTH - trimmedHistory.size + historyIndex] = inputRow
        }
        return inputArray
    }

    fun toSimulatedTensorflowInput(
        tensorflowInputBase: Array<FloatArray>,
        simulatedTails: List<UnitResult>,
    ): Array<FloatArray> {
        val inputArray = Array(INPUT_LENGTH) { FloatArray(mappings.size + 1) { 0f } }

        tensorflowInputBase.forEachIndexed { index, row ->
            inputArray[index] = row
        }

        simulatedTails.forEachIndexed { index, result ->
            val inputRow = getInputRow(result)
            inputArray[HISTORY_LENGTH - 1 + index] = inputRow
        }
        return inputArray
    }
    private fun getInputRow(result: UnitResult): FloatArray {
        val inputRow = FloatArray(mappings.size + 1)
        mappings[result.unitId]?.let { mapping ->
            inputRow[mapping.index] = 1f
            inputRow[mapping.index] = 1f
            inputRow[mappings.size] = if (result.resultType == UnitResultType.Success) 1f else 0f
        }
        return inputRow
    }
    companion object {
        const val HISTORY_LENGTH = 50
        const val INPUT_LENGTH = 1500
    }
}
