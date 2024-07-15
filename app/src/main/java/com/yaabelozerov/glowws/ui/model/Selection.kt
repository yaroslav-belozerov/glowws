package com.yaabelozerov.glowws.ui.model

data class Selection<T>(
    val inSelectionMode: Boolean = false,
    val entries: List<T> = emptyList(),
)

fun <T> Selection<T>.select(value: T): Selection<T> =
    if (entries.contains(value)) Selection((entries - value).isNotEmpty(), entries - value)
    else Selection(true, entries + value)

fun <T> Selection<T>.selectAll(list: List<T>): Selection<T> = Selection(true, this.entries + list)