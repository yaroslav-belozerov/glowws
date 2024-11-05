package com.yaabelozerov.glowws.ui.model

data class SelectionState<T>(
    val inSelectionMode: Boolean = false,
    val entries: List<T> = emptyList(),
)

fun <T> SelectionState<T>.select(value: T): SelectionState<T> =
    if (entries.contains(value)) {
      SelectionState((entries - value).isNotEmpty(), entries - value)
    } else {
      SelectionState(true, entries + value)
    }

fun <T> SelectionState<T>.selectAll(list: List<T>): SelectionState<T> =
    SelectionState(true, this.entries + list)
