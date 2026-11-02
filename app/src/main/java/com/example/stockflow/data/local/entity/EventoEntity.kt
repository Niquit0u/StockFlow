package com.example.stockflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Una entrada del historial del local: una venta, un ingreso de stock o un cambio en el inventario.
 * Los textos se guardan tal como eran en ese momento, así el historial no cambia si después
 * se renombra un producto o se elimina al usuario.
 */
@Entity(
    tableName = "historial",
    foreignKeys = [
        ForeignKey(
            entity = LocalEntity::class,
            parentColumns = ["idLocal"],
            childColumns = ["idLocal"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["idUsuario"],
            // Si se elimina el usuario, su historial se conserva (queda el nombre en "usuario")
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["idLocal", "fechaHora"]),
        Index(value = ["idUsuario"])
    ]
)
data class EventoEntity(
    @PrimaryKey(autoGenerate = true)
    val idEvento: Int = 0,

    val idLocal: Int,

    // TipoEvento.name: VENTA, INGRESO o CAMBIO
    val tipo: String,

    val fechaHora: Long = System.currentTimeMillis(),

    val idUsuario: Int?,

    // Nombre del usuario en ese momento
    val usuario: String,

    // Resumen en una línea, por ejemplo "Ingreso de 10 u. de Leche Entera 1L"
    val titulo: String,

    // Detalle en varias líneas (productos de la venta, qué cambió); vacío si no hay
    val detalle: String = "",

    // Solo en ventas: el total cobrado
    val monto: Double? = null
)
