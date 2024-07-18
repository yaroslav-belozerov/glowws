package com.yaabelozerov.glowws.ui.model

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.TransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.ui.screen.archive.ArchiveScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.MainScreenViewModel
import com.yaabelozerov.glowws.ui.screen.main.ScreenSelectedDialog

@Composable
fun MainScreenFloatingButtons(mvm: MainScreenViewModel, addNewIdeaCallback: (Long) -> Unit) {
    val isConfirmationOpen = remember {
        mutableStateOf(false)
    }
    if (isConfirmationOpen.value) {
        ScreenSelectedDialog(title = stringResource(id = R.string.dialog_archive_all),
            entries = listOf(
                DialogEntry(
                    Icons.Default.CheckCircle, stringResource(id = R.string.label_confirm), {
                        mvm.archiveSelected()
                    }, needsConfirmation = false
                ), DialogEntry(
                    null,
                    stringResource(id = R.string.label_cancel),
                    onClick = { isConfirmationOpen.value = false },
                    needsConfirmation = false
                )
            ),
            onDismiss = { isConfirmationOpen.value = false })
    }
    if (mvm.selection.collectAsState().value.inSelectionMode) {
        FloatingActionButton(onClick = {
            isConfirmationOpen.value = true
        }) {
            Icon(
                imageVector = Icons.Default.Delete, contentDescription = "archive selected button"
            )
        }
        FloatingActionButton(onClick = {
            mvm.deselectAll()
        }) {
            Icon(
                imageVector = Icons.Default.Close, contentDescription = "deselect button"
            )
        }
    } else {
        FloatingActionButton(onClick = {
            mvm.addNewIdeaAndProject("", callback = addNewIdeaCallback)
        }) {
            Icon(
                imageVector = Icons.Default.Add, contentDescription = "add idea button"
            )
        }
        FloatingActionButton(onClick = {
            mvm.toggleSortFilterModal()
        }) {
            Icon(
                imageVector = Icons.Default.MoreVert, contentDescription = "sort filter button"
            )
        }
    }
    var isSearchOpen by remember {
        mutableStateOf(mvm.state.value.searchQuery.isNotEmpty())
    }
    val searchFocus = remember {
        FocusRequester()
    }
    if (!isSearchOpen) Row {
        if (mvm.state.collectAsState().value.searchQuery.isNotBlank()) {
            FloatingActionButton(onClick = { mvm.updateSearchQuery("") }) {
                Icon(imageVector = Icons.Default.Clear, contentDescription = "clear search button")
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        FloatingActionButton(onClick = {
            isSearchOpen = true
        }) {
            Icon(
                imageVector = Icons.Default.Search, contentDescription = "open search button"
            )
        }
    } else {
        LaunchedEffect(key1 = Unit) {
            searchFocus.requestFocus()
        }
        OutlinedTextField(
            modifier = Modifier
                .padding(32.dp, 0.dp, 0.dp, 0.dp)
                .focusRequester(searchFocus),
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            value = mvm.state.collectAsState().value.searchQuery,
            onValueChange = { mvm.updateSearchQuery(it) },
            trailingIcon = {
                IconButton(onClick = {
                    mvm.updateSearchQuery("")
                    isSearchOpen = false
                }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "clear search icon",
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                isSearchOpen = false
            })
        )
    }
}

@Composable
fun ArchiveScreenFloatingButtons(avm: ArchiveScreenViewModel) {
    val isConfirmationOpen = remember {
        mutableStateOf(false)
    }
    val confirmationType = remember {
        mutableStateOf(ArchiveConfirmType.UNARCHIVE)
    }
    if (isConfirmationOpen.value) {
        ScreenSelectedDialog(title = stringResource(id = R.string.label_are_you_sure),
            entries = listOf(
                DialogEntry(
                    Icons.Default.CheckCircle, stringResource(id = R.string.label_confirm), {
                        when (confirmationType.value) {
                            ArchiveConfirmType.DELETE -> avm.removeSelected()
                            ArchiveConfirmType.UNARCHIVE -> avm.unarchiveSelected()
                        }
                    }, needsConfirmation = false
                ), DialogEntry(
                    null,
                    stringResource(id = R.string.label_cancel),
                    onClick = { isConfirmationOpen.value = false },
                    needsConfirmation = false
                )
            ),
            onDismiss = { isConfirmationOpen.value = false })
    }
    if (avm.selection.collectAsState().value.inSelectionMode) {
        FloatingActionButton(onClick = { avm.unarchiveSelected() }) {
            Icon(
                imageVector = Icons.Default.Refresh, contentDescription = "restore selected button"
            )
        }
        FloatingActionButton(onClick = {
            isConfirmationOpen.value = true
            confirmationType.value = ArchiveConfirmType.DELETE
        }) {
            Icon(
                imageVector = Icons.Default.Delete, contentDescription = "delete selected button"
            )
        }
        FloatingActionButton(onClick = { avm.deselectAll() }) {
            Icon(
                imageVector = Icons.Default.Close, contentDescription = "deselect button"
            )
        }
    } else if (avm.state.collectAsState().value.isNotEmpty()) {
        FloatingActionButton(onClick = {
            avm.selectAll()
        }) {
            Icon(
                imageVector = Icons.Default.Menu, contentDescription = "select all button button"
            )
        }
    }
}