package com.yaabelozerov.glowws.ui.screen.idea

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.yaabelozerov.glowws.data.local.media.MediaManager
import com.yaabelozerov.glowws.data.local.room.IdeaDao
import com.yaabelozerov.glowws.data.local.room.Point
import com.yaabelozerov.glowws.data.local.room.PointType
import com.yaabelozerov.glowws.domain.model.PointDomainModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IdeaScreenViewModel @Inject constructor(
    private val dao: IdeaDao,
    private val mediaManager: MediaManager,
    val imageLoader: ImageLoader
) : ViewModel() {
    private var _points = MutableStateFlow(emptyList<PointDomainModel>())
    val points = _points.asStateFlow()

    private var _saved = MutableStateFlow(Pair(-1L, 0L))

    val onPickMedia: MutableStateFlow<(() -> Unit)?> = MutableStateFlow(null)

    fun refreshPoints(ideaId: Long) {
        viewModelScope.launch {
            dao.getIdeaPoints(ideaId).flowOn(Dispatchers.IO).distinctUntilChanged()
                .collectLatest { points ->
                    _points.update {
                        points.map { PointDomainModel(it.pointId, it.type, it.pointContent, it.isMain) }
                    }
                }
        }
    }

    fun addPointAtIndex(pointType: PointType, ideaId: Long, index: Long, content: String = "") {
        viewModelScope.launch {
            when (pointType) {
                PointType.TEXT -> dao.insertPointUpdateIdeaAtIndex(
                    Point(
                        pointId = 0,
                        ideaParentId = ideaId,
                        pointContent = content,
                        index = index,
                        type = pointType,
                        isMain = false
                    )
                )

                PointType.IMAGE -> addImage(ideaId, index)
            }
        }
    }

    fun modifyPoint(pointId: Long, content: String? = null, isMain: Boolean? = null) {
        viewModelScope.launch {
            val point = dao.getPoint(pointId).first()
            dao.upsertPointUpdateIdea(
                point.copy(
                    pointContent = content ?: point.pointContent, isMain = isMain ?: point.isMain
                )
            )
        }
    }

    fun removePoint(pointId: Long) {
        viewModelScope.launch {
            val pt = dao.getPoint(pointId).first()
            if (pt.type == PointType.IMAGE) {
                mediaManager.removeMedia(pt.pointContent)
            }
            val ideaId = pt.ideaParentId
            dao.deletePointAndIndex(pointId)
            dao.updateIdeaContentFromPoints(ideaId)
        }
    }

    fun addImage(ideaId: Long, index: Long) {
        _saved.update { Pair(ideaId, index) }
        onPickMedia.value?.invoke()
    }

    suspend fun importImage(uri: Uri) {
        mediaManager.importMedia(uri) {
            viewModelScope.launch {
                dao.insertPointUpdateIdeaAtIndex(
                    Point(
                        pointId = 0,
                        ideaParentId = _saved.value.first,
                        pointContent = it,
                        index = _saved.value.second,
                        type = PointType.IMAGE,
                        isMain = false
                    )
                )
            }
        }
    }
}
