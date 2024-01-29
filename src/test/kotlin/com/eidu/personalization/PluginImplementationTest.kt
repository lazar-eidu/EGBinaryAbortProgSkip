package com.eidu.personalization

import assertk.assertThat
import assertk.assertions.matchesPredicate
import com.eidu.personalization.api.PersonalizationInput
import io.mockk.justRun
import io.mockk.mockk
import org.junit.Test

class PluginImplementationTest {
    private val plugin = PluginImplementation()

    @Test
    fun `instantiates and computes result for an existing unit`() {
        assertThat(
            plugin.determineNextUnits(
                PersonalizationInput(
                    emptyList(),
                    listOf(ACTUAL_UNIT_FROM_CSV)
                ),
                mockk() {
                    justRun { infer(any(), any(), any()) }
                }
            )
        ).matchesPredicate {
            it.nextUnits.contains(ACTUAL_UNIT_FROM_CSV)
        }
    }

    companion object {
        private const val ACTUAL_UNIT_FROM_CSV = "Eidu:51ec0f5e-78a2-4909-b65c-4e9aae441db5/Time21"
    }
}
