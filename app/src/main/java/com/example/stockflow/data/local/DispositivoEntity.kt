package com.example.stockflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dispositivos")
data class DispositivoEntity(
    @PrimaryKey
    val deviceId: String,

    val tokenPush: String
)