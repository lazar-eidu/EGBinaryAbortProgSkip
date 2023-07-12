package com.eidu.personalization

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.matchesPredicate
import com.eidu.personalization.api.PersonalizationInput
import com.eidu.personalization.api.TensorflowInferenceRunner
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class PluginImplementationTest {
    private val plugin = PluginImplementation()
    private val tensorflowInferenceRunnerMock: TensorflowInferenceRunner = mockk() {
        justRun { infer(any(), any(), any()) }
    }

    @Test
    fun `instantiates and computes result for an existing unit`() {
        assertThat(
            plugin.determineNextUnits(
                PersonalizationInput(
                    emptyList(),
                    listOf(ACTUAL_UNIT_FROM_CSV)
                ),
                tensorflowInferenceRunnerMock
            )
        ).matchesPredicate {
            it.nextUnits.contains(ACTUAL_UNIT_FROM_CSV)
        }
        verify() {
            tensorflowInferenceRunnerMock.infer(
                "dkt230712_base_tail.tflite", any(), any()
            )
        }
    }

    @Test
    fun `returns empty next units when plugin returns 0 scores`() {
        val pluginOutput = plugin.determineNextUnits(
            PersonalizationInput(listOf(), listOf())
        ) { _, _, _ -> }

        assertThat(pluginOutput.nextUnits).isEmpty()
    }

    companion object {
        private const val ACTUAL_UNIT_FROM_CSV = "Eidu:51ec0f5e-78a2-4909-b65c-4e9aae441db5/Time21"
    }
}
