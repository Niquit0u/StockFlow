package com.example.stockflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos")
data class ProductoEntity(
    @PrimaryKey(autoGenerate = true)
    val idProducto: Int = 0,

    val nombre: String,

    val codigoBarra: String,

    val precioVenta: Double,

    val stockMinimo: Int
)