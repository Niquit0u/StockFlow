package com.example.stockflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ventas",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["idUsuario"],
            // Si se elimina el usuario la venta se conserva (queda el nombre en "vendedor")
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = LocalEntity::class,
            parentColumns = ["idLocal"],
            childColumns = ["idLocal"],
            // Si se borra un local se borra todo lo suyo
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["idUsuario"]),
        Index(value = ["idLocal"])
    ]
)
data class VentaEntity(
    @PrimaryKey(autoGenerate = true)
    val idVenta: Int = 0,

    val idLocal: Int,

    val fechaHora: Long,

    val montoTotal: Double,

    // Usuario que hizo la venta; null si después fue eliminado
    val idUsuario: Int? = null,

    // Nombre del usuario al momento de la venta, para que el historial lo siga mostrando
    val vendedor: String = ""
)
