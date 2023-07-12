package com.eidu.personalization

class OutputProcessor(contentIdMapping: List<ContentIdMapping>) {
    private val mapping: Map<String, Int> = contentIdMapping.associateBy({ it.contentId }) { it.index }

    fun fromTensorflowOutput(
        availableUnits: List<String>,
        tensorflowOutput: Any
    ): Map<String, Float> {

        val output: List<Float> =
            (tensorflowOutput as FloatArray).asList()

        return availableUnits.associateWith { mapping[it]?.let { output[it] } ?: 0f }
    }
}
