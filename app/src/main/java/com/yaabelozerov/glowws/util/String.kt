package com.yaabelozerov.glowws.util

fun String.toReadableKey() =
    replace("_", " ").lowercase().replaceFirstChar { char -> char.uppercase() }

const val JSON_DELIMITER = ", "