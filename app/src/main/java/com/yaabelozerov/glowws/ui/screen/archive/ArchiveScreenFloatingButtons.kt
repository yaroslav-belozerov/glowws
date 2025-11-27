package com.yaabelozerov.glowws.ui.screen.archive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.ui.common.ScreenDialog
import com.yaabelozerov.glowws.ui.model.DialogEntry
import com.yaabelozerov.glowws.ui.screen.main.MainScreenViewModel

@Composable
fun ArchiveScreenFloatingButtons(mvm: MainScreenViewModel) {
  var isConfirmationOpen by remember { mutableStateOf(false) }
  val select by mvm.archiveSelect.collectAsState()
  if (isConfirmationOpen) {
    ScreenDialog(
        title = stringResource(id = R.string.label_are_you_sure),
        entries =
            listOf(
                DialogEntry(
                    Icons.Default.CheckCircle,
                    stringResource(id = R.string.label_confirm),
                    { mvm.removeSelected() }),
                DialogEntry(
                    null,
                    stringResource(id = R.string.label_cancel),
                    onClick = { isConfirmationOpen = false })),
        onDismiss = { isConfirmationOpen = false })
  }
  if (select.inSelectionMode) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
      FloatingActionButton(onClick = { mvm.toggleArchiveSelected(true) }) {
        Icon(imageVector = Icons.Default.Refresh, contentDescription = "restore selected button")
      }
      FloatingActionButton(onClick = { isConfirmationOpen = true }) {
        Icon(imageVector = Icons.Default.Delete, contentDescription = "delete selected button")
      }
      FloatingActionButton(onClick = { mvm.deselectAllArchive() }) {
        Icon(imageVector = Icons.Default.Close, contentDescription = "deselect button")
      }
    }
  } else if (mvm.state.collectAsState().value.archivedIdeas.isNotEmpty()) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
      FloatingActionButton(onClick = { mvm.selectAllArchive() }) {
        Icon(imageVector = Icons.Default.Menu, contentDescription = "select all button button")
      }
    }
  }
}
