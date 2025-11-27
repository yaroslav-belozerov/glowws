package com.yaabelozerov.glowws.ui.screen.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.ui.common.ScreenDialog
import com.yaabelozerov.glowws.ui.model.DialogEntry

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreenFloatingButtons(
    modifier: Modifier = Modifier,
    mvm: MainScreenViewModel,
    addNewIdeaCallback: (Long) -> Unit
) {
  var isConfirmationOpen by remember { mutableStateOf(false) }
  val select by mvm.select.collectAsState()
  if (isConfirmationOpen) {
    ScreenDialog(
        title = stringResource(id = R.string.dialog_archive_all),
        entries =
            listOf(
                DialogEntry(
                    Icons.Default.CheckCircle,
                    stringResource(id = R.string.label_confirm),
                    { mvm.toggleArchiveSelected(false) }),
                DialogEntry(
                    null,
                    stringResource(id = R.string.label_cancel),
                    onClick = { isConfirmationOpen = false })),
        onDismiss = { isConfirmationOpen = false })
  }
  Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.End,
      modifier = modifier) {
        val inSelection = select.inSelectionMode
        if (inSelection) {
          FloatingActionButton(onClick = { isConfirmationOpen = true }) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "archive selected button")
          }
          FloatingActionButton(onClick = { mvm.deselectAll() }) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "deselect button")
          }
        } else {
          FloatingActionButton(onClick = { mvm.toggleSortFilterModal() }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "sort filter button")
          }
        }
        val isSearchOpen = mvm.searchOpen.collectAsState().value
        if (!isSearchOpen) {
          Row {
            if (mvm.filter.collectAsState().value.searchQuery.isNotBlank()) {
              FloatingActionButton(onClick = { mvm.updateSearchQuery("") }) {
                Icon(imageVector = Icons.Default.Clear, contentDescription = "clear search button")
              }
              Spacer(modifier = Modifier.width(8.dp))
            }
            FloatingActionButton(onClick = { mvm.setSearch(true) }) {
              Icon(imageVector = Icons.Default.Search, contentDescription = "open search button")
            }
          }
        }
        if (!inSelection) {
          FloatingActionButton(onClick = { mvm.addNewIdea(callback = addNewIdeaCallback) }) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "add idea button")
          }
        }
      }
}
