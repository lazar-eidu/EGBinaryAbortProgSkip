package com.eidu.personalization

import com.eidu.personalization.api.PersonalizationInput

class OutputProcessor(contentIdMapping: List<ContentIdMapping>) {
    private val mapping: Map<String, Int> = contentIdMapping.associateBy({ it.contentId }) { it.index }

    fun fromTensorflowOutput(
        input: PersonalizationInput,
        tensorflowOutput: Any
    ): Map<String, Float> {
        val output: List<Float> =
            (tensorflowOutput as FloatArray).asList()

        return input.availableUnits.associateWith { mapping[it]?.let { output[it] } ?: 0f }
    }
}
