package com.yaabelozerov.glowws.ui.screen.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.ui.common.ScreenDialog
import com.yaabelozerov.glowws.ui.model.DialogEntry

@Composable
fun MainScreenFloatingButtons(mvm: MainScreenViewModel, addNewIdeaCallback: (Long) -> Unit) {
    var isConfirmationOpen by remember {
        mutableStateOf(false)
    }
    if (isConfirmationOpen) {
        ScreenDialog(title = stringResource(id = R.string.dialog_archive_all),
            entries = listOf(DialogEntry(Icons.Default.CheckCircle,
                stringResource(id = R.string.label_confirm),
                { mvm.archiveSelected() }), DialogEntry(null,
                stringResource(id = R.string.label_cancel),
                onClick = { isConfirmationOpen = false })),
            onDismiss = { isConfirmationOpen = false })
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End
    ) {
        if (mvm.selection.collectAsState().value.inSelectionMode) {
            FloatingActionButton(onClick = {
                isConfirmationOpen = true
            }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "archive selected button"
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
                mvm.addNewIdea(callback = addNewIdeaCallback)
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
        if (!isSearchOpen) {
            Row {
                if (mvm.state.collectAsState().value.searchQuery.isNotBlank()) {
                    FloatingActionButton(onClick = { mvm.updateSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "clear search button"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                FloatingActionButton(onClick = {
                    isSearchOpen = true
                }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "open search button"
                    )
                }
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
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
}
