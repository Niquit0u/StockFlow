package com.example.stockflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dispositivos")
data class DispositivoEntity(
    @PrimaryKey
    val deviceId: String,

    // Token de notificaciones push; null hasta que la app use Firebase Cloud Messaging
    val tokenPush: String? = null,

    val fechaRegistro: Long = System.currentTimeMillis()
)