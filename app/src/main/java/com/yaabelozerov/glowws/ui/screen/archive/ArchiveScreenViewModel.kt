package com.yaabelozerov.glowws.ui.screen.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.domain.mapper.IdeaMapper
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import com.yaabelozerov.glowws.ui.model.SelectionState
import com.yaabelozerov.glowws.ui.model.select
import com.yaabelozerov.glowws.ui.model.selectAll
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArchiveScreenViewModel @Inject constructor(
    private val dao: IdeaDao,
    private val mapper: IdeaMapper
) : ViewModel() {
    private var _state: MutableStateFlow<List<IdeaDomainModel>> = MutableStateFlow(emptyList())
    val state = _state.asStateFlow()

    private var _selectionState: MutableStateFlow<SelectionState<Long>> = MutableStateFlow(SelectionState())
    val selection = _selectionState.asStateFlow()

    init {
        viewModelScope.launch { getArchiveScreenIdeas() }
    }

    fun getArchiveScreenIdeas() {
        viewModelScope.launch {
            dao.getArchivedIdeas().map { mapper.toDomainModelFlat(it) }.collect { ideas ->
                _state.update { ideas }
            }
        }
    }

    fun unarchiveIdea(ideaId: Long) {
        viewModelScope.launch {
            dao.unarchiveIdea(ideaId)
            getArchiveScreenIdeas()
        }
    }

    fun removeIdea(ideaId: Long) {
        viewModelScope.launch {
            dao.deleteIdeaAndPoints(ideaId)
            getArchiveScreenIdeas()
        }
    }

    fun removeSelected() =
        _selectionState.value.entries.forEach { removeIdea(it) }.also { deselectAll() }

    fun unarchiveSelected() =
        _selectionState.value.entries.forEach { unarchiveIdea(it) }.also { deselectAll() }

    fun selectAll() = _selectionState.update { sel -> sel.selectAll(_state.value.map { it.id }) }
    fun deselectAll() = _selectionState.update { SelectionState() }
    fun onSelect(ideaId: Long) = _selectionState.update { it.select(ideaId) }
}
