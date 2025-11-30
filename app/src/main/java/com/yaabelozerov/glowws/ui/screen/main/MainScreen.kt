package com.yaabelozerov.glowws.ui.screen.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.yaabelozerov.glowws.Const
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.room.PointType
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import com.yaabelozerov.glowws.domain.model.PointDomainModel
import com.yaabelozerov.glowws.domain.model.SettingDomainModel
import com.yaabelozerov.glowws.ui.common.Nav
import com.yaabelozerov.glowws.ui.common.ScreenDialog
import com.yaabelozerov.glowws.ui.model.DialogEntry
import com.yaabelozerov.glowws.ui.model.SelectionState
import java.io.File
import kotlinx.coroutines.CoroutineScope

fun SettingDomainModel?.boolean() = this?.value.toString() == "true"

fun SettingDomainModel?.booleanOrNull() = this?.let { value.toString() == "true" }

sealed class MainScreenEvent {
  data class OpenIdea(val id: Long) : MainScreenEvent()

  data class ArchiveIdea(val id: Long) : MainScreenEvent()

  data class SelectIdea(val id: Long) : MainScreenEvent()

  data class SetPriority(val id: Long, val priority: Long) : MainScreenEvent()

  data object NavigateToFeedback : MainScreenEvent()
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    mvm: MainScreenViewModel,
    ideas: List<IdeaDomainModel> = emptyList(),
    onEvent: (MainScreenEvent) -> Unit,
    selection: SelectionState<Long>,
    settings: Map<SettingsKeys, SettingDomainModel>,
    snackbar: Pair<SnackbarHostState, CoroutineScope>
) {
  LazyColumn(
      modifier =
          modifier.fillMaxSize().then(modifier).background(MaterialTheme.colorScheme.background),
  ) {
    item {
      Row(
          modifier =
              Modifier.fillParentMaxWidth()
                  .padding(horizontal = 16.dp)
                  .padding(
                      bottom = if (snackbar.first.currentSnackbarData == null) 12.dp else 0.dp,
                      top = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween) {
            Nav.MainScreenRoute.resId?.let { res ->
              Text(
                  text = stringResource(id = res),
                  fontSize = 32.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 16.dp))
            }
          }
    }
    item {
      AnimatedVisibility(
          snackbar.first.currentSnackbarData != null,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically()) {
            SnackbarHost(
                snackbar.first,
                snackbar = {
                  Snackbar(
                      snackbarData = it,
                      shape = MaterialTheme.shapes.medium,
                      modifier = Modifier.clickable { it.dismiss() },
                      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                      contentColor = MaterialTheme.colorScheme.onBackground)
                })
          }
    }
    item {
      val isSearchOpen by mvm.searchOpen.collectAsState()
      val searchFocus = remember { FocusRequester() }
      val specFade = spring<Float>(stiffness = Spring.StiffnessHigh)
      val specExpand = spring<IntSize>(stiffness = Spring.StiffnessHigh)
      AnimatedVisibility(
          isSearchOpen,
          enter = fadeIn(specFade) + expandVertically(specExpand),
          exit = fadeOut(specFade) + shrinkVertically(specExpand)) {
            LaunchedEffect(key1 = Unit) { searchFocus.requestFocus() }
            OutlinedTextField(
                modifier =
                    Modifier.focusRequester(searchFocus)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                value = mvm.filter.collectAsState().value.searchQuery,
                onValueChange = { mvm.updateSearchQuery(it) },
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                trailingIcon = {
                  IconButton(
                      onClick = {
                        mvm.updateSearchQuery("")
                        mvm.setSearch(false)
                      }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "clear search icon",
                        )
                      }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { mvm.setSearch(false) }))
          }
    }
    items(ideas, key = { it.id }) { idea ->
      Idea(
          modifier = Modifier.animateItem(),
          previewPoint = idea.mainPoint,
          modified = idea.modified.string,
          created = idea.created.string,
          priority = idea.priority,
          onClick = { onEvent(MainScreenEvent.OpenIdea(idea.id)) },
          onArchive = { onEvent(MainScreenEvent.ArchiveIdea(idea.id)) },
          onSelect = { onEvent(MainScreenEvent.SelectIdea(idea.id)) },
          onSetPriority = { onEvent(MainScreenEvent.SetPriority(idea.id, it)) },
          inSelectionMode = selection.inSelectionMode,
          isSelected = selection.entries.contains(idea.id),
          displayPlaceholders = settings.getValue(SettingsKeys.SHOW_PLACEHOLDERS).boolean(),
          fullImage = settings.getValue(SettingsKeys.IMAGE_FULL_HEIGHT).boolean())
    }
  }
  if (ideas.isEmpty()) {
    Box(
      modifier = Modifier.fillMaxSize()) {
      Row(modifier = Modifier.padding(bottom = 32.dp, end = 88.dp).align(Alignment.BottomEnd), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          stringResource(R.string.placeholder_add_idea),
          fontSize = 16.sp)
        Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = null)
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Idea(
    modifier: Modifier = Modifier,
    previewPoint: PointDomainModel,
    modified: String,
    created: String,
    priority: Long,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onSelect: () -> Unit,
    onSetPriority: (Long) -> Unit,
    inSelectionMode: Boolean,
    isSelected: Boolean,
    displayPlaceholders: Boolean,
    fullImage: Boolean
) {
  var isDialogOpen by remember { mutableStateOf(false) }
  val outlineColor by
      animateColorAsState(
          if (isSelected) {
            MaterialTheme.colorScheme.surfaceContainerHighest
          } else {
            MaterialTheme.colorScheme.surfaceContainer
          })

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.fillMaxWidth()
                .background(outlineColor)
                .combinedClickable(
                    onClick =
                        if (!inSelectionMode) {
                          onClick
                        } else {
                          onSelect
                        },
                    onLongClick = { if (!inSelectionMode) isDialogOpen = true })) {
          when (previewPoint.type) {
            PointType.TEXT ->
                Text(
                    text =
                        if (previewPoint.content.isBlank() && displayPlaceholders) {
                          stringResource(id = R.string.placeholder_noname)
                        } else {
                          previewPoint.content
                        },
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 16.dp).padding(start = 16.dp).weight(1f),
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 24.sp, lineHeight = 28.sp),
                    color =
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (previewPoint.content.isBlank()) 0.3f else 1f))

            PointType.IMAGE ->
              AsyncImage(
                  modifier =
                      Modifier
                          .then(if (fullImage) Modifier else Modifier.heightIn(0.dp, 128.dp))
                          .clip(MaterialTheme.shapes.medium.copy(topStart = CornerSize(0), bottomStart = CornerSize(0)))
                          .weight(1f),
                  contentScale = ContentScale.FillWidth,
                  model = previewPoint.content.also(::println),
                  contentDescription = null)
          }
          Crossfade(priority != 0L) { show ->
            if (show) {
              Column(
                  modifier = Modifier.fillMaxHeight().padding(4.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                    for (it in 1..Const.UI.MAX_PRIORITY) {
                      Crossfade(it <= priority) { active ->
                        Box(
                            modifier =
                                Modifier.clip(MaterialTheme.shapes.small)
                                    .width(8.dp)
                                    .height(12.dp)
                                    .background(
                                        if (active) {
                                          MaterialTheme.colorScheme.primary
                                        } else {
                                          MaterialTheme.colorScheme.surfaceContainerHigh
                                        }))
                      }
                    }
                  }
            } else {
              Spacer(Modifier.width(16.dp))
            }
          }
        }
    Box(
        modifier =
            Modifier.clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .height(2.dp))
  }

  if (isDialogOpen) {
    ScreenDialog(
        title = if (previewPoint.type == PointType.TEXT) previewPoint.content else "",
        info = listOf(Icons.Default.AddCircleOutline to created, Icons.Default.Edit to modified),
        entries =
            listOf(
                DialogEntry(
                    Icons.Default.Menu, stringResource(id = R.string.label_select), { onSelect() }),
                DialogEntry(
                    Icons.Default.Delete,
                    stringResource(id = R.string.m_archive_idea),
                    onArchive,
                    needsConfirmation = true)),
        onDismiss = { isDialogOpen = false },
        extras = {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (it in 0L..3L) {
                  InputChip(
                      selected = it == priority,
                      onClick = { onSetPriority(it) },
                      label = {
                        Text(
                            text =
                                if (it != 0L) {
                                  it.toString()
                                } else {
                                  stringResource(id = R.string.label_no_priority)
                                })
                      })
                }
              }
        })
  }
}
