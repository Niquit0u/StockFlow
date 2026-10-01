package com.example.stockflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ventas")
data class VentaEntity(
    @PrimaryKey(autoGenerate = true)
    val idVenta: Int = 0,

    val fechaHora: Long,

    val montoTotal: Double
)