package com.yaabelozerov.glowws.util

inline fun <reified T : Enum<T>> valueOfOrNull(name: String): Enum<T>? {
    return enumValues<T>().findLast { it.name == name }
}