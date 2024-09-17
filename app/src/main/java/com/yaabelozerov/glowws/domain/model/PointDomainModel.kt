package com.yaabelozerov.glowws.domain.model

import com.yaabelozerov.glowws.data.local.room.PointType

data class PointDomainModel(
    val id: Long,
    val type: PointType,
    val content: String,
    val isMain: Boolean
)
