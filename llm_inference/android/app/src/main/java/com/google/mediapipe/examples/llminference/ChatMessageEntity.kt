package com.google.mediapipe.examples.llminference

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "raw_message") val rawMessage: String,
    val author: String,
    val timestamp: Long = System.currentTimeMillis()
)