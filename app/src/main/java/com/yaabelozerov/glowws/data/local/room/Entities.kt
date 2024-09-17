package com.yaabelozerov.glowws.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Idea(
    @PrimaryKey(autoGenerate = true) val ideaId: Long,
    val isArchived: Boolean,
    val timestampCreated: Long,
    val timestampModified: Long,
    val ideaContent: String
)

@Entity
data class Point(
    @PrimaryKey(autoGenerate = true) val pointId: Long,
    val ideaParentId: Long,
    var pointContent: String,
    val index: Long,
    var type: Int,
    var isMain: Boolean,
)
