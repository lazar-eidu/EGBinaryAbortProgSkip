package com.eidu.personalization

import com.eidu.personalization.api.PersonalizationInput
import com.eidu.personalization.api.PersonalizationOutput
import com.eidu.personalization.api.PersonalizationPlugin
import com.eidu.personalization.api.TensorflowInferenceRunner
import com.eidu.personalization.api.UnitResultType

class PluginImplementation : PersonalizationPlugin {
    private val modelFile = PluginConfig.config.modelPath
    private val contentIdMapping = ContentIdMapping.readContentIdMapping()
    private val inputProcessor = InputProcessor(contentIdMapping)
    private val outputProcessor = OutputProcessor(contentIdMapping)
    private val thresholdMapping = contentIdMapping.associateBy({ it.contentId }) { it.threshold }
    private val repeatCutoff: Int = 10

    override fun determineNextUnits(
        input: PersonalizationInput,
        runTensorflowInference: TensorflowInferenceRunner
    ): PersonalizationOutput {

        if (getProgress(input) > 0.95f) return PersonalizationOutput(listOf(), mapOf())

        val sanitizedInput = PersonalizationInput(
            input.learningHistory,
            filterAvailableUnits(input)
        )

        val initialTensorflowInput = inputProcessor.toInitialTensorflowInput(sanitizedInput)
        val initialInference = getInitialInference(runTensorflowInference, initialTensorflowInput, sanitizedInput)

        val filteredInference = initialInference.filter { it.value < (thresholdMapping[it.key] ?: 1f) }
        if (filteredInference.isEmpty()) return PersonalizationOutput(listOf(), filteredInference)

        val simulatedInferences = SimulatedInference(
            modelFile, contentIdMapping, inputProcessor, outputProcessor
        ).getSimulatedInferences(
            initialTensorflowInput, initialInference, sanitizedInput, runTensorflowInference
        )
        val maxGain = simulatedInferences.maxByOrNull { it.second }

        return PersonalizationOutput(
            maxGain?.let { listOf(it.first) } ?: listOf(),
            simulatedInferences.toMap()
        )
    }

    private fun filterAvailableUnits(input: PersonalizationInput) =
        input.availableUnits.filter { unit ->
            !input.learningHistory.takeLast(repeatCutoff).any {
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

    fun getProgress(input: PersonalizationInput): Float {
        val successfulUnits = input.learningHistory
            .filter { histUnit ->
                histUnit.resultType == UnitResultType.Success && (histUnit.score ?: 0f) > 0.05
            }
            .map { it.unitId }
            .toSet()

        val successCount = input.availableUnits.count { unitId ->
            unitId in successfulUnits
        }

        return successCount.toFloat() / input.availableUnits.size.toFloat()
    }
}
