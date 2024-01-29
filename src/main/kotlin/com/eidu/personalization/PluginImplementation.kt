package com.eidu.personalization

import com.eidu.personalization.api.PersonalizationInput
import com.eidu.personalization.api.PersonalizationOutput
import com.eidu.personalization.api.PersonalizationPlugin
import com.eidu.personalization.api.TensorflowInferenceRunner

class PluginImplementation : PersonalizationPlugin {
    override fun determineNextUnits(
        input: PersonalizationInput,
        runTensorflowInference: TensorflowInferenceRunner
    ): PersonalizationOutput =
        PersonalizationOutput(
            input.availableUnits,
            mapOf()
        )
}
