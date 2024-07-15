package com.yaabelozerov.glowws.ui.screen.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.data.local.room.Idea
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.di.SettingsManager
import com.yaabelozerov.glowws.domain.mapper.IdeaMapper
import com.yaabelozerov.glowws.domain.mapper.SettingsMapper
import com.yaabelozerov.glowws.ui.model.FilterFlag
import com.yaabelozerov.glowws.ui.model.Selection
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType
import com.yaabelozerov.glowws.ui.model.reversed
import com.yaabelozerov.glowws.ui.model.select
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val dao: IdeaDao,
    private val ideaMapper: IdeaMapper,
    private val settingsMapper: SettingsMapper,
    private val settingsManager: SettingsManager
) : ViewModel() {
    private var _state: MutableStateFlow<MainScreenState> = MutableStateFlow(MainScreenState())
    val state = _state.asStateFlow()

    private var _selection: MutableStateFlow<Selection<Long>> = MutableStateFlow(Selection())
    val selection = _selection.asStateFlow()

    private var _isSortFilterOpen = MutableStateFlow(false)
    val sortFilterOpen = _isSortFilterOpen.asStateFlow()

    init {
        fetchSortFilter()
    }

    fun fetchSortFilter() {
        fetchSort()
        fetchFilter()
    }

    fun fetchSort() = viewModelScope.launch {
        _state.update {
            it.copy(
                sort = settingsMapper.getSorting(settingsManager.fetchSettings())
            )
        }
        fetchMainScreen()
    }

    fun fetchFilter() = viewModelScope.launch {
        _state.update {
            it.copy(
                filter = settingsMapper.getFilter(settingsManager.fetchSettings())
            )
        }
        fetchMainScreen()
    }

    fun toggleSortFilterModal() = _isSortFilterOpen.update { !_isSortFilterOpen.value }

    fun fetchMainScreen() {
        viewModelScope.launch {
            dao.getGroupsWithIdeasNotArchived().collect { items ->
                val new = ideaMapper.toDomainModel(
                    items, _state.value.filter, _state.value.sort
                )
                _state.update {
                    it.copy(ideas = new)
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
            try {
                dao.archiveStrayIdea(ideaId)
                fetchMainScreen()
            } catch (e: Error) {
                e.printStackTrace()
            }
        }
    }

    fun addNewIdeaAndProject(content: String, callback: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = dao.createIdeaAndGroup(content)
            callback?.invoke(id)
            fetchMainScreen()
        }
    }

    fun addIdeaToGroup(content: String, groupId: Long, callback: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val m = System.currentTimeMillis()
            val id = dao.addIdea(Idea(0, groupId, m, m, content))
            callback?.invoke(id)
        }
    }

    fun modifyGroupName(groupId: Long, newName: String) {
        viewModelScope.launch {
            dao.updateGroupName(groupId, newName)
        }
    }

    fun archiveGroup(groupId: Long) {
        viewModelScope.launch {
            dao.archiveGroup(groupId)
        }
    }

    fun archiveSelected() {
        _selection.value.entries.forEach { archiveIdea(it) }
        deselectAll()
    }

    fun deselectAll() = _selection.update { Selection() }
    fun onSelect(ideaId: Long) = _selection.update { it.select(ideaId) }
}