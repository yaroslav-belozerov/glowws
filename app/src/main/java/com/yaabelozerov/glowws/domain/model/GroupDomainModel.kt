package com.yaabelozerov.glowws.domain.model

data class GroupDomainModel(
    val id: Long,
    val created: String,
    val modified: String,
    val content: String,
    val entries: List<IdeaDomainModel>
)
