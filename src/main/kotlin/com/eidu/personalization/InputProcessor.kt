package com.eidu.personalization

import com.eidu.personalization.api.PersonalizationInput
import com.eidu.personalization.api.UnitResultType

class InputProcessor(contentIdMappings: List<ContentIdMapping>) {
    private val mappings: Map<String, ContentIdMapping> = contentIdMappings.associateBy { it.contentId }
    private val inputArray = arrayOf(Array(HISTORY_LENGTH) { FloatArray(mappings.size + 1) { -1f } })

    fun toTensorflowInput(input: PersonalizationInput): Array<Array<FloatArray>> {
        val trimmedHistory = input.learningHistory.takeLast(HISTORY_LENGTH)

        trimmedHistory.forEachIndexed { historyIndex, result ->
            val inputRow = FloatArray(mappings.size + 1)
            mappings[result.unitId]?.let { mapping ->
                inputRow[mapping.index] = 1f
                inputRow[mappings.size] = if (result.resultType == UnitResultType.Success) 1f else 0f
            }
            inputArray[0][historyIndex] = inputRow
        }
        return inputArray
    }

    companion object {
        const val HISTORY_LENGTH = 50
    }
}
