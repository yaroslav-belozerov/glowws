package com.yaabelozerov.glowws.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class IdeaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val content: String
)