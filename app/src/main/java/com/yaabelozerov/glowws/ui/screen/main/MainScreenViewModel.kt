package com.yaabelozerov.glowws.ui.screen.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.di.SettingsManager
import com.yaabelozerov.glowws.domain.mapper.IdeaMapper
import com.yaabelozerov.glowws.domain.mapper.SettingsMapper
import com.yaabelozerov.glowws.ui.model.FilterFlag
import com.yaabelozerov.glowws.ui.model.FilterModel
import com.yaabelozerov.glowws.ui.model.SelectionState
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType
import com.yaabelozerov.glowws.ui.model.reversed
import com.yaabelozerov.glowws.ui.model.select
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    val imageLoader: ImageLoader,
    private val dao: IdeaDao,
    private val ideaMapper: IdeaMapper,
    private val settingsMapper: SettingsMapper,
    private val settingsManager: SettingsManager
) : ViewModel() {
    private var _state: MutableStateFlow<MainScreenState> = MutableStateFlow(MainScreenState())
    val state = _state.asStateFlow()

    private var _selectionState: MutableStateFlow<SelectionState<Long>> =
        MutableStateFlow(SelectionState())
    val selection = _selectionState.asStateFlow()

    private var _isSortFilterOpen = MutableStateFlow(false)
    val sortFilterOpen = _isSortFilterOpen.asStateFlow()

    init {
        fetchSort()
    }

    fun appFirstVisit(callback: suspend () -> Unit = {}) {
        viewModelScope.launch {
            when (settingsManager.getAppVisits().also { Log.i("App visits", it.toString()) }) {
                0L -> callback()
            }
            settingsManager.visitApp()
        }
    }

    fun resetFilter() {
        _state.update { it.copy(filter = FilterModel(emptyMap())) }
    }

    fun fetchSort() = viewModelScope.launch {
        _state.update {
            it.copy(
                sort = settingsMapper.getSorting(settingsManager.fetchSettings())
            )
        }
        fetchMainScreen()
    }

    fun toggleSortFilterModal() = _isSortFilterOpen.update { !_isSortFilterOpen.value }

    fun fetchMainScreen() {
        viewModelScope.launch {
            if (_state.value.searchQuery.isBlank()) {
                dao.getAllIdeas().collect { items ->
                    _state.update {
                        it.copy(
                            ideas = ideaMapper.toDomainModel(
                                items,
                                _state.value.filter,
                                _state.value.sort
                            )
                        )
                    }
                }
            } else {
                dao.getAllIdeasSearch("%${_state.value.searchQuery}%").collect { ideas ->
                    _state.update {
                        it.copy(
                            ideas = ideaMapper.toDomainModel(
                                ideas,
                                _state.value.filter,
                                _state.value.sort
                            )
                        )
                    }
                }
            }
        }
    }

    fun setFilterFlag(flag: FilterFlag, value: Boolean) =
        _state.update { it.copy(filter = it.filter.copy(flags = it.filter.flags + (flag to value))) }
            .also { fetchMainScreen() }

    fun setSortType(type: SortType) =
        _state.update { it.copy(sort = it.sort.copy(type = type)) }.also { fetchMainScreen() }

    fun setSortOrder(order: SortOrder) =
        _state.update { it.copy(sort = it.sort.copy(order = order)) }.also { fetchMainScreen() }

    fun reverseSortOrder() = setSortOrder(_state.value.sort.order.reversed())

    fun archiveIdea(ideaId: Long) {
        viewModelScope.launch {
            dao.setArchiveIdea(ideaId)
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
        _selectionState.value.entries.forEach {
            viewModelScope.launch { dao.setArchiveIdea(it) }
        }
        fetchMainScreen()
        deselectAll()
    }

    fun deselectAll() = _selectionState.update { SelectionState() }
    fun onSelect(ideaId: Long) = _selectionState.update { it.select(ideaId) }

    fun updateSearchQuery(newQuery: String) {
        _state.update { it.copy(searchQuery = newQuery) }
        fetchMainScreen()
    }

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
}
