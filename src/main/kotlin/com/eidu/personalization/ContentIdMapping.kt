package com.eidu.personalization

import com.eidu.personalization.util.getSimpleCsvFromResource
import java.lang.Exception

data class ContentIdMapping(val index: Int, val contentId: String, val threshold: Float) {
    companion object {
        fun readContentIdMapping(): List<ContentIdMapping> =
            getSimpleCsvFromResource("/content_id_lookup_v1.0.1.csv").map {
                require(it.size == 3) { "Invalid row: $it" }

                try {
                    ContentIdMapping(it[0].toInt(), it[2], it[1].toFloat())
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid row: $it", e)
                }
            }.toList()
    }
}
