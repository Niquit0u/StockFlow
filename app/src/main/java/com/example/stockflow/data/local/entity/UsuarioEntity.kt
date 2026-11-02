package com.example.stockflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    foreignKeys = [
        ForeignKey(
            entity = LocalEntity::class,
            parentColumns = ["idLocal"],
            childColumns = ["idLocal"],
            // Si se borra un local se borra todo lo suyo
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["idLocal"]),
        // El correo identifica a cada usuario (con él se inicia sesión); el nombre puede repetirse
        Index(value = ["email"], unique = true)
    ]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    // Local al que pertenece (el dueño, sus administradores y empleados comparten el mismo)
    val idLocal: Int,
    // Nombre para mostrar (puede repetirse entre usuarios)
    val nombreUsuario: String,
    // Obligatorio y único. Siempre en minúsculas, para que "Ana@Mail.com" y "ana@mail.com" sean la misma cuenta
    val email: String,
    // Nunca texto plano: "salt:hash" generado por UsuarioRepository
    val contrasena: String,
    // Rol.clave: DUENO, ADMINISTRADOR o EMPLEADO
    val rol: String
)
