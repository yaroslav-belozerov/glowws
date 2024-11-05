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

@Composable
fun ArchiveScreenFloatingButtons(avm: ArchiveScreenViewModel) {
  var isConfirmationOpen by remember { mutableStateOf(false) }
  if (isConfirmationOpen) {
    ScreenDialog(
        title = stringResource(id = R.string.label_are_you_sure),
        entries =
            listOf(
                DialogEntry(
                    Icons.Default.CheckCircle,
                    stringResource(id = R.string.label_confirm),
                    { avm.removeSelected() }),
                DialogEntry(
                    null,
                    stringResource(id = R.string.label_cancel),
                    onClick = { isConfirmationOpen = false })),
        onDismiss = { isConfirmationOpen = false })
  }
  if (avm.selection.collectAsState().value.inSelectionMode) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
      FloatingActionButton(onClick = { avm.unarchiveSelected() }) {
        Icon(imageVector = Icons.Default.Refresh, contentDescription = "restore selected button")
      }
      FloatingActionButton(onClick = { isConfirmationOpen = true }) {
        Icon(imageVector = Icons.Default.Delete, contentDescription = "delete selected button")
      }
      FloatingActionButton(onClick = { avm.deselectAll() }) {
        Icon(imageVector = Icons.Default.Close, contentDescription = "deselect button")
      }
    }
  } else if (avm.state.collectAsState().value.isNotEmpty()) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
      FloatingActionButton(onClick = { avm.selectAll() }) {
        Icon(imageVector = Icons.Default.Menu, contentDescription = "select all button button")
      }
    }
  }
}
