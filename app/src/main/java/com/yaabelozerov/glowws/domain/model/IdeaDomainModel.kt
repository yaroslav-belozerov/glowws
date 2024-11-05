package com.yaabelozerov.glowws.domain.model

data class JoinedTimestamp(val timestamp: Long, val string: String)

data class IdeaDomainModel(
    val id: Long,
    val priority: Long,
    val created: JoinedTimestamp,
    val modified: JoinedTimestamp,
    val mainPoint: PointDomainModel
)
