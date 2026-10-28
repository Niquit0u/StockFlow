package com.example.stockflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stockflow.data.local.entity.UsuarioEntity

@Dao
interface UsuarioDao {

    // IGNORE: si el nombre ya existe devuelve -1 en vez de pisar la cuenta (y la contraseña) existente
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarUsuario(usuario: UsuarioEntity): Long

    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :nombre LIMIT 1")
    suspend fun buscarUsuario(nombre: String): UsuarioEntity?
}
