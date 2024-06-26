package com.yaabelozerov.glowws.data.local.room

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.coroutines.flow.Flow

@Entity
data class Idea(
    @PrimaryKey(autoGenerate = true) val ideaId: Long,
    val groupParentId: Long,
    val content: String
)

@Entity
data class Group(
    @PrimaryKey(autoGenerate = true) val groupId: Long,
    val name: String
)