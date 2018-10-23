package com.creations.livebox_common.util

/**
 * @author Sérgio Serra
 * Dummy model for testing serialization/deserialization
 */
data class Bag<T>(val id: String, val values: List<T>)
