package com.yaabelozerov.glowws.ui.screen.archive

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.yaabelozerov.glowws.data.local.media.MediaManager
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.data.local.room.PointType
import com.yaabelozerov.glowws.domain.mapper.IdeaMapper
import com.yaabelozerov.glowws.domain.model.IdeaDomainModel
import com.yaabelozerov.glowws.ui.model.SelectionState
import com.yaabelozerov.glowws.ui.model.select
import com.yaabelozerov.glowws.ui.model.selectAll
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArchiveScreenViewModel @Inject constructor(
    val imageLoader: ImageLoader,
    private val dao: IdeaDao,
    private val mapper: IdeaMapper,
    private val mediaManager: MediaManager
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
            dao.getAllIdeas(archived = true).collect { ideas ->
                _state.update { mapper.toDomainModel(ideas) }
            }
        }
    }

    fun unarchiveIdea(ideaId: Long) {
        viewModelScope.launch {
            dao.setNotArchivedIdea(ideaId)
            getArchiveScreenIdeas()
        }
    }

    fun removeIdea(ideaId: Long) {
        viewModelScope.launch {
            dao.getIdeaAttachments(ideaId).collectLatest {
                it.forEach { p ->
                    Log.i("ArchiveScreenViewModel", "removeIdea: $p")
                    if (p.type == PointType.IMAGE) {
                        mediaManager.removeMedia(p.pointContent)
                    }
                }
                dao.deleteIdeaAndPoints(ideaId)
                getArchiveScreenIdeas()
            }
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
