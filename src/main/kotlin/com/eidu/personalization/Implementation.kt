package com.eidu.personalization

import com.eidu.personalization.api.PersonalizationInput
import com.eidu.personalization.api.PersonalizationOutput
import com.eidu.personalization.api.PersonalizationPlugin
import com.eidu.personalization.api.TensorflowInferenceRunner
import com.eidu.personalization.api.UnitResult
import com.eidu.personalization.api.UnitResultType
import kotlin.math.min

class Implementation : PersonalizationPlugin {
    private val modelFile = "dkt_model_230504.tflite"
    private val contentIdMapping = ContentIdMapping.readContentIdMapping()
    private val inputProcessor = InputProcessor(contentIdMapping)
    private val outputProcessor = OutputProcessor(contentIdMapping)
    private val thresholdMapping = contentIdMapping.associateBy({ it.contentId }) { it.threshold }

    override fun determineNextUnits(
        input: PersonalizationInput,
        runTensorflowInference: TensorflowInferenceRunner
    ): PersonalizationOutput {
        val sanitizedInput = PersonalizationInput(
            input.learningHistory,
            filterAvailableUnits(input)
        )
        val initialInference = runInference(sanitizedInput, runTensorflowInference)

        val filteredInference = initialInference.filter {
            it.value < (thresholdMapping[it.key] ?: 1f)
        }

        if (filteredInference.isEmpty()) return PersonalizationOutput(
            listOf(),
            filteredInference
        )
        val simulatedInferences = initialInference.keys.map { contentId ->
            val simulatedHistory =
                sanitizedInput.learningHistory + UnitResult(contentId, UnitResultType.Success, 0L, null)
            val simulatedInput = PersonalizationInput(simulatedHistory, sanitizedInput.availableUnits - contentId)
            val simulatedOutput = runInference(simulatedInput, runTensorflowInference)
            val simulatedGain =
                (initialInference[contentId] ?: 0.0f) * simulatedOutput.values.sum()
            contentId to simulatedGain
        }

        val maxGain = simulatedInferences.maxByOrNull { it.second }

        return PersonalizationOutput(
            maxGain?.let { listOf(it.first) } ?: listOf(),
            simulatedInferences.toMap()
        )
    }

    private fun filterAvailableUnits(input: PersonalizationInput) =
        input.availableUnits.filter { unit ->
            !input.learningHistory.any {
                it.unitId == unit &&
                    it.resultType == UnitResultType.Success &&
                    (it.score ?: 0f) > (thresholdMapping[it.unitId] ?: 0f)
            }
        }

    private fun runInference(
        input: PersonalizationInput,
        runTensorflowInference: TensorflowInferenceRunner
    ): Map<String, Float> {
        val tensorflowInput = inputProcessor.toTensorflowInput(input)
        val tensorflowOutput =
            arrayOf(Array(InputProcessor.HISTORY_LENGTH) { FloatArray(contentIdMapping.size) })
        runTensorflowInference.infer(modelFile, tensorflowInput, tensorflowOutput)
        val outputIndex =
            if (input.learningHistory.size == 0) 0
            else min(input.learningHistory.size - 1, InputProcessor.HISTORY_LENGTH - 1)
        return outputProcessor.fromTensorflowOutput(input, tensorflowOutput[0][outputIndex])
    }
}
