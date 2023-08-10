package com.eidu.personalization

import com.eidu.personalization.api.PersonalizationInput
import com.eidu.personalization.api.PersonalizationOutput
import com.eidu.personalization.api.PersonalizationPlugin
import com.eidu.personalization.api.TensorflowInferenceRunner
import com.eidu.personalization.api.UnitResult
import com.eidu.personalization.api.UnitResultType

class PluginImplementation : PersonalizationPlugin {
    private val modelFile = "dkt230712_base_tail_v1.tflite"
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

        val initialTensorflowInput = inputProcessor.toInitialTensorflowInput(sanitizedInput)
        val initialInference = getInitialInference(runTensorflowInference, initialTensorflowInput, sanitizedInput)

        val filteredInference = initialInference.filter { it.value < (thresholdMapping[it.key] ?: 1f) }
        if (filteredInference.isEmpty()) return PersonalizationOutput(listOf(), filteredInference)

        val simulatedInferences = getSimulatedInferences(
            initialTensorflowInput, initialInference, sanitizedInput, runTensorflowInference
        )
        val maxGain = simulatedInferences.maxByOrNull { it.second }

        return PersonalizationOutput(
            maxGain?.let { listOf(it.first) } ?: listOf(),
            simulatedInferences.toMap()
        )
    }

    private fun getSimulatedInferences(
        initialTensorflowInput: Array<FloatArray>,
        initialInference: Map<String, Float>,
        sanitizedInput: PersonalizationInput,
        runTensorflowInference: TensorflowInferenceRunner
    ): List<Pair<String, Float>> {

        val simulatedTails = initialInference.keys.map { contentId ->
            val simulatedTail = UnitResult(contentId, UnitResultType.Success, 0L, initialInference[contentId])
            simulatedTail
        }

        val simulatedTensorflowOutput: Array<FloatArray> =
            getSimulatedTensorflowOutput(
                initialTensorflowInput, simulatedTails, runTensorflowInference
            ).sliceArray(
                InputProcessor.HISTORY_LENGTH - 1 until InputProcessor.HISTORY_LENGTH - 1 + simulatedTails.size
            )

        val simulatedOutputs = simulatedTensorflowOutput.zip(simulatedTails).map { (output, tail) ->
            outputProcessor.fromTensorflowOutput(sanitizedInput.availableUnits - tail.unitId, output)
        }

        val simulatedInferences = initialInference.keys.zip(simulatedOutputs).map { (contentId, output) ->
            contentId to (initialInference[contentId] ?: 0.0f) * output.values.sum()
        }

        return simulatedInferences
    }
    private fun getSimulatedTensorflowOutput(
        initialTensorflowInput: Array<FloatArray>,
        simulatedTails: List<UnitResult>,
        runTensorflowInference: TensorflowInferenceRunner
    ): Array<FloatArray> {

        val tensorflowInputBase = initialTensorflowInput.sliceArray(1 until InputProcessor.HISTORY_LENGTH)
        val simulatedTensorflowInput = inputProcessor.toSimulatedTensorflowInput(tensorflowInputBase, simulatedTails)
        val simulatedTensorflowOutput = Array(InputProcessor.INPUT_LENGTH - InputProcessor.HISTORY_LENGTH + 1) {
            FloatArray(contentIdMapping.size)
        }

        runTensorflowInference.infer(modelFile, simulatedTensorflowInput, simulatedTensorflowOutput)

        return simulatedTensorflowOutput
    }

    private fun filterAvailableUnits(input: PersonalizationInput) =
        input.availableUnits.filter { unit ->
            !input.learningHistory.any {
                it.unitId == unit &&
                    it.resultType == UnitResultType.Success &&
                    (it.score ?: 0f) > (thresholdMapping[it.unitId] ?: 0f)
            }
        }

    private fun getInitialInference(
        runTensorflowInference: TensorflowInferenceRunner,
        initialTensorflowInput: Array<FloatArray>,
        sanitizedInput: PersonalizationInput
    ): Map<String, Float> {
        val initialTensorflowOutput = Array(InputProcessor.INPUT_LENGTH - InputProcessor.HISTORY_LENGTH + 1) {
            FloatArray(contentIdMapping.size)
        }
        runTensorflowInference.infer(modelFile, initialTensorflowInput, initialTensorflowOutput)

        val initialInference = outputProcessor.fromTensorflowOutput(
            sanitizedInput.availableUnits, initialTensorflowOutput[InputProcessor.HISTORY_LENGTH - 1]
        )
        return initialInference
    }
}
