package com.example.stockflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Categoría de productos de un local (la crea el dueño o un administrador desde Inventario). */
@Entity(
    tableName = "categorias",
    foreignKeys = [
        ForeignKey(
            entity = LocalEntity::class,
            parentColumns = ["idLocal"],
            childColumns = ["idLocal"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        // No puede haber dos categorías con el mismo nombre en un local
        Index(value = ["idLocal", "nombre"], unique = true)
    ]
)
data class CategoriaEntity(
    @PrimaryKey(autoGenerate = true)
    val idCategoria: Int = 0,

    val idLocal: Int,

    val nombre: String,

    // Posición en la lista de Inventario (la elige el usuario arrastrando); 0 = primera
    val orden: Int = 0
)
