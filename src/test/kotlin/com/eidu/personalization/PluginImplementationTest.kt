package com.eidu.personalization

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.matchesPredicate
import com.eidu.personalization.api.PersonalizationInput
import com.eidu.personalization.api.TensorflowInferenceRunner
import com.eidu.personalization.api.UnitResult
import com.eidu.personalization.api.UnitResultType
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
                PluginConfig.config.modelPath, any(), any()
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

    @Test
    fun `test getProgress`() {
        val availableUnits = listOf("U1", "U2", "U3", "U4", "U5", "U6", "U7", "U8", "U9", "U10")

        val learningHistory = listOf(
            UnitResult("U1", UnitResultType.Success, 1000, 0.1f),
            UnitResult("U2", UnitResultType.Success, 1500, 0.06f),
            UnitResult("U3", UnitResultType.Success, 2000, 0.07f),
            UnitResult("U11", UnitResultType.Success, 1200, 0.08f), // Not in available units
            UnitResult("U12", UnitResultType.Abort, 800, 0.99f) // Not a success
        )

        val currentProgress = plugin.getProgress(PersonalizationInput(learningHistory, availableUnits))

        assertThat(currentProgress).isEqualTo(0.3f) // Expected result is 3 out of 10 units meet the criteria
    }
}
