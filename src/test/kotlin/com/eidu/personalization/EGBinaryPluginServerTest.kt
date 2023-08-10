package com.eidu.personalization

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import com.eidu.personalization.api.PersonalizationInput
import com.eidu.personalization.api.TensorflowInferenceRunner
import com.eidu.personalization.api.UnitResult
import com.eidu.personalization.api.UnitResultType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Ignore
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@Ignore
class EGBinaryPluginServerTest {

    private val plugin = PluginImplementation()
    private val serverTensorflowInferenceRunner = ServerTensorflowInferenceRunner()

    @Test
    fun `returns a unit with highest simulated average score`() {
        val testInput = PersonalizationInput(
            listOf(),
            listOf(FIRST_UNIT, SECOND_UNIT, THIRD_UNIT, FOURTH_UNIT)
        )
        val pluginOutput = plugin.determineNextUnits(
            testInput, serverTensorflowInferenceRunner
        )

        assertThat(listOf(FIRST_UNIT, SECOND_UNIT, THIRD_UNIT, FOURTH_UNIT)).contains(pluginOutput.nextUnits[0])
    }

    @Test
    fun `filters successful units`() {
        val testInput = PersonalizationInput(
            listOf(UnitResult(FIRST_UNIT, UnitResultType.Success, 0L, 0.96f)),
            listOf(FIRST_UNIT)
        )
        val pluginOutput = plugin.determineNextUnits(testInput, serverTensorflowInferenceRunner)
        assertThat(pluginOutput.nextUnits).isEmpty()
    }

    companion object {
        private const val FIRST_UNIT =
            "Anton:/../c-mat-1-us/topic-01-numbers-and-counting/block-01-review-counting-1-to-10/level-02"
        private const val SECOND_UNIT =
            "Anton:/../c-mat-1-us/topic-01-numbers-and-counting/block-01-review-counting-1-to-10/level-03"
        private const val THIRD_UNIT =
            "Anton:/../c-mat-1-us/topic-01-numbers-and-counting/block-02-review-counting-1-to-20/level-01"
        private const val FOURTH_UNIT =
            "Anton:/../c-mat-1-us/topic-01-numbers-and-counting/block-02-review-counting-1-to-20/level-03"
    }

    class ServerTensorflowInferenceRunner : TensorflowInferenceRunner {
        override fun infer(modelResourcePath: String, input: Any, output: Any) {
            serverInfer(input, output)
        }
        fun getServerArray(inputArray: Array<FloatArray>): Array<FloatArray> {
            val url = URL("http://localhost:8000")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val requestBody = Json.encodeToString(inputArray)

            connection.outputStream.use { outputStream ->
                outputStream.write(requestBody.toByteArray())
                outputStream.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                reader.close()
                connection.disconnect()

                val outputData = Json.decodeFromString<Array<FloatArray>>(response.toString())
                return outputData
            } else {
                connection.disconnect()
                throw Exception("Error: $responseCode")
            }
        }

        fun serverInfer(input: Any, output: Any) {
            @Suppress("UNCHECKED_CAST") val inputArray = input as Array<FloatArray>
            @Suppress("UNCHECKED_CAST") val outputArray = output as Array<FloatArray>
            val tensorflowOutput = getServerArray(inputArray)
            outputArray.forEachIndexed { row_index, it_row ->
                it_row.forEachIndexed { col_index, it_col ->
                    it_row[col_index] = tensorflowOutput[row_index][col_index]
                }
            }
        }
    }
}
