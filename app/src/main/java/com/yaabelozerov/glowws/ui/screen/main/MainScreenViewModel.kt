package com.yaabelozerov.glowws.ui.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.R
import com.yaabelozerov.glowws.data.local.room.Idea
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.di.SettingsManager
import com.yaabelozerov.glowws.domain.mapper.IdeaMapper
import com.yaabelozerov.glowws.domain.mapper.SettingsMapper
import com.yaabelozerov.glowws.ui.model.FilterFlag
import com.yaabelozerov.glowws.ui.model.SelectionState
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType
import com.yaabelozerov.glowws.ui.model.reversed
import com.yaabelozerov.glowws.ui.model.select
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
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

    private var _selectionState: MutableStateFlow<SelectionState<Long>> = MutableStateFlow(SelectionState())
    val selection = _selectionState.asStateFlow()

    private var _isSortFilterOpen = MutableStateFlow(false)
    val sortFilterOpen = _isSortFilterOpen.asStateFlow()

    private var _tooltipBarState = MutableStateFlow(TooltipBarState())
    val tooltipBarState = _tooltipBarState.asStateFlow()

    init {
        fetchSortFilter()
        viewModelScope.launch {
            when (settingsManager.getAppVisits()) {
                0L -> sendTooltipMessage(R.string.tooltipbar_welcome, R.string.placeholder_dismiss)
            }
            settingsManager.visitApp()
        }
    }

    fun sendTooltipMessage(vararg msgResId: Int) = _tooltipBarState.update {
        TooltipBarState(true, msgResId.toList()) { _tooltipBarState.update { TooltipBarState() } }
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
            if (_state.value.searchQuery.isBlank()) {
                dao.getGroupsWithIdeasNotArchived()
                    .collect { items ->
                        val new = ideaMapper.toDomainModel(
                            items,
                            _state.value.filter,
                            _state.value.sort
                        )
                        _state.update {
                            it.copy(ideas = new)
                        }
                    }
            } else {
                dao.getGroupsIdsNotArchivedQuery("%${_state.value.searchQuery}%")
                    .collect { groupIds ->
                        groupIds.map { dao.getGroupsWithIdeasNotArchivedRestricted(it) }.merge()
                            .collect { map ->
                                _state.update {
                                    it.copy(
                                        ideas = ideaMapper.toDomainModel(
                                            map,
                                            _state.value.filter,
                                            _state.value.sort
                                        )
                                    )
                                }
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
            dao.archiveStrayIdea(ideaId)
            fetchMainScreen()
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
        _selectionState.value.entries.forEach { archiveIdea(it) }
        deselectAll()
    }

    fun deselectAll() = _selectionState.update { SelectionState() }
    fun onSelect(ideaId: Long) = _selectionState.update { it.select(ideaId) }

    fun updateSearchQuery(newQuery: String) {
        _state.update { it.copy(searchQuery = newQuery) }
        fetchMainScreen()
    }
}
