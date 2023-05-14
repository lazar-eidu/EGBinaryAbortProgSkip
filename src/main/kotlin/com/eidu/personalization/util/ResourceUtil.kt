package com.eidu.personalization.util

import java.io.InputStream

/**
 * Reads comma-separated values from a resource, skipping the first line. Note this does not support any features
 * of more complete CSV specifications, such as quoting and escaping.
 */
fun Any.getSimpleCsvFromResource(path: String): Sequence<List<String>> = sequence {
    this@getSimpleCsvFromResource.getResourceAsStream(path).bufferedReader().use { reader ->
        reader.lineSequence()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { line -> line.split(',').map { it.trim() } }
            .forEach { yield(it) }
    }
}

fun Any.getResourceAsStream(path: String): InputStream =
    requireNotNull(javaClass.getResourceAsStream(path)) { "Resource $path not found." }
