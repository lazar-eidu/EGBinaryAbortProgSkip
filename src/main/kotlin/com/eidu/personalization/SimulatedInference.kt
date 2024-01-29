package com.eidu.personalization

import com.eidu.personalization.api.PersonalizationInput
import com.eidu.personalization.api.TensorflowInferenceRunner
import com.eidu.personalization.api.UnitResult
import com.eidu.personalization.api.UnitResultType

class SimulatedInference(
    private val modelFile: String,
    private val contentIdMapping: List<ContentIdMapping>,
    private val inputProcessor: InputProcessor,
    private val outputProcessor: OutputProcessor
) {
    private fun getGains(
        tensorflowOutput: Array<FloatArray>,
        simulatedTails: List<UnitResult>,
        initialInference: Map<String, Float>,
        sanitizedInput: PersonalizationInput,
        isAbort: Boolean = false
    ): List<Float> { // Changed return type to List<Float>
        val outputs = tensorflowOutput.zip(simulatedTails) { output, tail ->
            outputProcessor.fromTensorflowOutput(sanitizedInput.availableUnits - tail.unitId, output)
        }
        return initialInference.keys.zip(outputs) { content_id, output ->
            val sumOfOutputs = output.values.sum()
            val initialProbability = initialInference[content_id] ?: 0f
            if (isAbort) sumOfOutputs * (1f - initialProbability)
            else sumOfOutputs * initialProbability
        }
    }

    fun getSimulatedInferences(
        initialTensorflowInput: Array<FloatArray>, // The initial input for the TensorFlow model.
        initialInference: Map<String, Float>, // Map containing initial inference values by content ID.
        sanitizedInput: PersonalizationInput, // Cleaned input data for personalization.
        runTensorflowInference: TensorflowInferenceRunner // Interface to run TensorFlow inferences.
    ): List<Pair<String, Float>> {

        // Generates "tails" for each content ID to represent next-step learning outcomes,
        // with "Success" indicating a positive outcome and "Abort" indicating a non-successful outcome.
        val simulatedSuccessTails = initialInference.keys.map { contentId ->
            UnitResult(contentId, UnitResultType.Success, 0L, initialInference[contentId])
        }
        val simulatedAbortTails = initialInference.keys.map { contentId ->
            UnitResult(contentId, UnitResultType.Abort, 0L, initialInference[contentId])
        }

        // Combines success and abort tails to create a complete simulation set.
        val simulatedTails = simulatedSuccessTails + simulatedAbortTails

        // Determines the range of TensorFlow output to use, accounting for unused outputs,
        // because the first InputProcessor.HISTORY_LENGTH - 1 of outputs
        // are not used relevant to the simulation.
        val outputRange = InputProcessor.HISTORY_LENGTH - 1 until
            InputProcessor.HISTORY_LENGTH - 1 + simulatedTails.size

        // Slices the TensorFlow output to obtain only the relevant values.
        val simulatedTensorflowOutput = getSimulatedTensorflowOutput(
            initialTensorflowInput,
            simulatedTails,
            runTensorflowInference
        ).sliceArray(outputRange)

        // Given how we created simulatedTails we know that the first half of the outputs are
        // for success tails and the second half are for abort tails.
        val successRange = 0 until simulatedSuccessTails.size
        val abortRange = simulatedSuccessTails.size until simulatedTails.size

        // Processes the success and abort outcomes separately, using the respective ranges.
        val successGains = getGains(
            simulatedTensorflowOutput.sliceArray(successRange),
            simulatedSuccessTails,
            initialInference,
            sanitizedInput
        )
        val abortGains = getGains(
            simulatedTensorflowOutput.sliceArray(abortRange),
            simulatedAbortTails,
            initialInference,
            sanitizedInput,
            isAbort = true
        )

        // Aggregates the success and abort inferences for each content ID,
        // pairing them as <ContentID, CombinedInferenceScore>.
        // Combine the outputs of success and abort into pairs
        val gains = successGains.zip(abortGains) {
            success, abort ->
            success + abort
        }

        // Now, create the final list of inferences by combining content IDs with their corresponding combined output
        val simulatedInferences = initialInference.keys.zip(gains) { contentId, combinedOutput ->
            contentId to combinedOutput
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
}
