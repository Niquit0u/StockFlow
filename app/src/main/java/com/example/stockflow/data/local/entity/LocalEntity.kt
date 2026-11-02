package com.example.stockflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un comercio. Cada dueño tiene un local, y los usuarios, productos y ventas pertenecen a uno.
 * Los lotes y movimientos cuelgan de su producto, así que heredan el local.
 */
@Entity(tableName = "locales")
data class LocalEntity(
    @PrimaryKey(autoGenerate = true)
    val idLocal: Int = 0,

    val nombre: String,

    val fechaCreacion: Long = System.currentTimeMillis()
)
