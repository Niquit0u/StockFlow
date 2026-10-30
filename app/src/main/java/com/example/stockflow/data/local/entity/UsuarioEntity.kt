package com.example.stockflow.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    indices = [
        Index(value = ["nombreUsuario"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombreUsuario: String,
    // Siempre en minúsculas, para que "Ana@Mail.com" y "ana@mail.com" sean la misma cuenta
    val email: String,
    // Nunca texto plano: "salt:hash" generado por UsuarioRepository
    val contrasena: String,
    val rol: String
) {
    companion object {
        const val ROL_DUENO = "DUENO"
        const val ROL_CLIENTE = "CLIENTE"
    }
}
