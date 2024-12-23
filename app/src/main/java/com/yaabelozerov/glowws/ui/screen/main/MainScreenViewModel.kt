package com.yaabelozerov.glowws.ui.screen.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.di.SettingsManager
import com.yaabelozerov.glowws.domain.mapper.IdeaMapper
import com.yaabelozerov.glowws.domain.mapper.SettingsMapper
import com.yaabelozerov.glowws.ui.common.Nav
import com.yaabelozerov.glowws.ui.common.withParam
import com.yaabelozerov.glowws.ui.model.FilterFlag
import com.yaabelozerov.glowws.ui.model.SelectionState
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType
import com.yaabelozerov.glowws.ui.model.reversed
import com.yaabelozerov.glowws.ui.model.select
import com.yaabelozerov.glowws.ui.screen.idea.IdeaScreenViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

@HiltViewModel
class MainScreenViewModel
@Inject
constructor(
    private val dao: IdeaDao,
    private val ideaMapper: IdeaMapper,
    private val settingsMapper: SettingsMapper,
    private val settingsManager: SettingsManager
) : ViewModel() {
  private val _state: MutableStateFlow<MainScreenState> = MutableStateFlow(MainScreenState())
  val state = _state.asStateFlow()

  private val _selectionState: MutableStateFlow<SelectionState<Long>> =
      MutableStateFlow(SelectionState())
  val selection = _selectionState.asStateFlow()

  private val _isSortFilterOpen = MutableStateFlow(false)
  val sortFilterOpen = _isSortFilterOpen.asStateFlow()

  private val _searchOpen = MutableStateFlow(false)
  val searchOpen = _searchOpen.asStateFlow()

  init {
    fetchSort()
  }

  fun appFirstVisit(callback: suspend () -> Unit = {}) {
    viewModelScope.launch {
      when (settingsManager.getAppVisits().also { Log.i("App visits", it.toString()) }) {
        0L -> callback()
        else -> {}
      }
      settingsManager.visitApp()
    }
  }

  fun resetFilter() {
    _state.update { it.copy(filter = MainScreenState().filter) }.also { fetchMainScreen() }
  }

  fun fetchSort() =
      viewModelScope.launch {
        _state.update { it.copy(sort = settingsMapper.getSorting(settingsManager.fetchSettings())) }
        fetchMainScreen()
      }

  fun toggleSortFilterModal() = _isSortFilterOpen.update { !_isSortFilterOpen.value }

  private fun fetchMainScreen() {
    viewModelScope.launch {
      if (_state.value.searchQuery.isBlank()) {
        dao.getAllIdeas().collect { items ->
          _state.update {
            it.copy(ideas = ideaMapper.toDomainModel(items, _state.value.filter, _state.value.sort))
          }
        }
      } else {
        dao.getAllIdeasSearch("%${_state.value.searchQuery.replace(" ", "")}%").collect { ideas ->
          _state.update {
            it.copy(ideas = ideaMapper.toDomainModel(ideas, _state.value.filter, _state.value.sort))
          }
        }
      }
    }
  }

  fun updateFilterFlag(flagType: KClass<FilterFlag>, flag: FilterFlag) {
    _state
      .update { state ->
        state.copy(filter = state.filter + (flagType to flag))
      }
      .also { fetchMainScreen() }
  }

  fun setSortType(type: SortType) =
      _state.update { it.copy(sort = it.sort.copy(type = type)) }.also { fetchMainScreen() }

  private fun setSortOrder(order: SortOrder) =
      _state.update { it.copy(sort = it.sort.copy(order = order)) }.also { fetchMainScreen() }

  fun reverseSortOrder() = setSortOrder(_state.value.sort.order.reversed())

  private fun archiveIdea(ideaId: Long) {
    viewModelScope.launch {
      dao.setArchiveIdea(ideaId)
      fetchMainScreen()
    }
  }

  private fun setPriority(ideaId: Long, priority: Long) {
    viewModelScope.launch {
      dao.setPriorityIdea(ideaId, priority)
      fetchMainScreen()
    }
  }

  fun addNewIdea(callback: ((Long) -> Unit)? = null) {
    viewModelScope.launch {
      val id = dao.createIdea()
      callback?.invoke(id)
      fetchMainScreen()
    }
  }

  fun archiveSelected() {
    _selectionState.value.entries.forEach { viewModelScope.launch { dao.setArchiveIdea(it) } }
    fetchMainScreen()
    deselectAll()
  }

  fun deselectAll() = _selectionState.update { SelectionState() }

  private fun onSelect(ideaId: Long) = _selectionState.update { it.select(ideaId) }

  fun updateSearchQuery(newQuery: String) {
    _state.update { it.copy(searchQuery = newQuery) }
    fetchMainScreen()
  }

  fun setSearch(open: Boolean) = _searchOpen.update { open }

  fun tryDiscardEmpty(ideaId: Long, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      dao.getIdeaPoints(ideaId).first().let {
        if (it.isEmpty() || (it.size == 1 && (it.getOrNull(0)?.pointContent?.isBlank() != false))) {
          dao.deleteIdea(ideaId)
          onSuccess()
        }
      }
    }
  }

  fun onEvent(event: MainScreenEvent, navCtrl: NavController, ivm: IdeaScreenViewModel) {
    when (event) {
      is MainScreenEvent.NavigateToFeedback -> navCtrl.navigate(Nav.FeedbackRoute.route)
      is MainScreenEvent.OpenIdea -> {
        navCtrl.navigate(Nav.IdeaScreenRoute.withParam(event.id))
        ivm.refreshPoints(event.id)
      }
      is MainScreenEvent.ArchiveIdea -> {
        archiveIdea(event.id)
      }
      is MainScreenEvent.SelectIdea -> {
        onSelect(event.id)
      }
      is MainScreenEvent.SetPriority -> {
        setPriority(event.id, event.priority)
      }
    }
  }
}
