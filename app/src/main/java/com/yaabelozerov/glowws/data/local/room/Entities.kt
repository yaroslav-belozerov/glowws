package com.yaabelozerov.glowws.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Idea(
    @PrimaryKey(autoGenerate = true) val ideaId: Long,
    val groupParentId: Long,
    val timestampCreated: Long,
    val timestampModified: Long,
    val content: String
)

@Entity
data class Group(
    @PrimaryKey(autoGenerate = true) val groupId: Long,
    val timestampCreated: Long,
    val timestampModified: Long,
    val name: String,
    val isArchived: Boolean
)

@Entity
data class Point(
    @PrimaryKey(autoGenerate = true) val pointId: Long,
    val ideaParentId: Long,
    var content: String,
    val index: Long,
    var type: Int,
    var isMain: Boolean,
)
