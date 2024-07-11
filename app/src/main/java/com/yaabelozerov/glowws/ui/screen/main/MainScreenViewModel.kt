package com.yaabelozerov.glowws.ui.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.data.local.room.Idea
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.domain.mapper.IdeaMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val dao: IdeaDao, private val mapper: IdeaMapper
) : ViewModel() {
    private var _state = MutableStateFlow(MainScreenState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch { getMainScreenIdeas() }
    }

    fun getMainScreenIdeas() {
        viewModelScope.launch {
            dao.getGroupsWithIdeasNotArchived().map { mapper.toDomainModel(it) }.collect { ideas ->
                _state.update { it.copy(ideas = ideas) }
            }
        }
    }

    fun archiveIdea(ideaId: Long) {
        viewModelScope.launch {
            try {
                dao.archiveStrayIdea(ideaId)
                getMainScreenIdeas()
            } catch (e: Error) {
                e.printStackTrace()
            }
        }
    }

    fun addNewIdeaAndProject(content: String, callback: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = dao.createIdeaAndGroup(content)
            callback?.invoke(id)
            getMainScreenIdeas()
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
}