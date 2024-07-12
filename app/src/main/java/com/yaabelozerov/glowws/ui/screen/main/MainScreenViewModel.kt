package com.yaabelozerov.glowws.ui.screen.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.data.local.datastore.SettingsKeys
import com.yaabelozerov.glowws.data.local.room.Group
import com.yaabelozerov.glowws.data.local.room.Idea
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.di.SettingsManager
import com.yaabelozerov.glowws.domain.mapper.IdeaMapper
import com.yaabelozerov.glowws.domain.mapper.SettingsMapper
import com.yaabelozerov.glowws.ui.model.FilterFlag
import com.yaabelozerov.glowws.ui.model.SortOrder
import com.yaabelozerov.glowws.ui.model.SortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
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

    init {
        viewModelScope.launch {
            setSortFilter()
            getMainScreenIdeas()
        }
    }

    fun setSortFilter() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    sort = settingsMapper.getSorting(
                        settingsManager.fetchSettings()
                    ), filter = settingsMapper.getFilter(settingsManager.fetchSettings())
                )
            }
            getMainScreenIdeas()
        }
    }

    fun getMainScreenIdeas() {
        viewModelScope.launch {
            dao.getGroupsWithIdeasNotArchived().collect { items ->
                val comparator = when (_state.value.sort.type) {
                    SortType.ALPHABETICAL -> compareBy<Group> { it.name }.thenBy { it.groupId }
                        .apply { if (_state.value.sort.order == SortOrder.DESCENDING) reversed() }

                    SortType.TIMESTAMP_CREATED -> compareBy<Group> { it.timestampCreated }.thenBy { it.groupId }
                        .apply { if (_state.value.sort.order == SortOrder.DESCENDING) reversed() }

                    SortType.TIMESTAMP_MODIFIED -> compareBy<Group> { it.timestampModified }.thenBy { it.groupId }
                        .apply { if (_state.value.sort.order == SortOrder.DESCENDING) reversed() }
                }
                val new = ideaMapper.toDomainModel(
                    items.filter { (if (_state.value.filter.flags[FilterFlag.IN_GROUP] == true) it.value.size > 1 else true) },
                    comparator
                )
                _state.update {
                    _state.value.copy(ideas = new)
                }
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