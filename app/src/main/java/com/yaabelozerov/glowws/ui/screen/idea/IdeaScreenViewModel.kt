package com.yaabelozerov.glowws.ui.screen.idea

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.data.local.room.Point
import com.yaabelozerov.glowws.domain.model.PointDomainModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IdeaScreenViewModel @Inject constructor(private val dao: IdeaDao) : ViewModel() {
    private var _state = MutableStateFlow(IdeaScreenState())
    val state = _state.asStateFlow()

    fun getPointsByIdeaId(ideaId: Long) {
        viewModelScope.launch {
            dao.getIdeaPoints(ideaId).collect { points ->
                _state.update {
                    it.copy(points = points.map { point ->
                        PointDomainModel(
                            point.pointId, point.content, point.isMain
                        )
                    })
                }
            }
        }
    }

    fun addPoint(ideaId: Long) {
        viewModelScope.launch {
            dao.upsertPoint(
                Point(pointId = 0, ideaParentId = ideaId, content = "", type = 0, isMain = false)
            )
        }
        getPointsByIdeaId(ideaId)
    }

    fun modifyPoint(pointId: Long, newText: String) {
        viewModelScope.launch {
            dao.updatePoint(pointId, newText)
        }
    }

    fun removePoint(pointId: Long) {
        viewModelScope.launch {
            dao.deletePoint(pointId)
        }
    }
}