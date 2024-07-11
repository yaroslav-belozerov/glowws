package com.yaabelozerov.glowws.domain.model

import java.util.Date

data class IdeaDomainModel(
    val id: Long,
    val modified: String,
    val groupId: Long,
    val content: String
)
