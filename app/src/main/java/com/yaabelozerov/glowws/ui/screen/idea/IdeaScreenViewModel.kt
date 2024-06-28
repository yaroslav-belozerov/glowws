package com.yaabelozerov.glowws.ui.screen.idea

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.data.local.room.Point
import com.yaabelozerov.glowws.domain.model.PointDomainModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IdeaScreenViewModel @Inject constructor(private val dao: IdeaDao) : ViewModel() {
    private var _state = MutableStateFlow(IdeaScreenState())
    val state = _state.asStateFlow()

    fun refreshPoints(ideaId: Long) {
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
    }

    fun modifyPoint(pointId: Long, content: String? = null, isMain: Boolean? = null) {
        viewModelScope.launch {
            val point = dao.getPoint(pointId).first()
            dao.upsertPoint(
                point.copy(
                    content = content ?: point.content, isMain = isMain ?: point.isMain
                )
            )
        }
    }

    fun removePoint(pointId: Long) {
        viewModelScope.launch {
            dao.deletePoint(pointId)
        }
    }
}