package com.yaabelozerov.glowws.ui.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.domain.model.IdeaMapper
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

    fun getIdeas() {
        viewModelScope.launch {
            dao.getGroupsWithIdeas().map { mapper.toDomainModel(it) }.collect { ideas ->
                _state.update { it.copy(ideas = ideas) }
            }
        }
    }

    fun removeIdea(ideaId: Long) {
        viewModelScope.launch {
            try {
                dao.deleteIdea(ideaId)
                getIdeas()
            } catch (e: Error) {
                e.printStackTrace()
            }
        }
    }

    fun addIdea(idea: String) {
        viewModelScope.launch {
            dao.createIdeaAndGroup(idea)
        }
    }
}