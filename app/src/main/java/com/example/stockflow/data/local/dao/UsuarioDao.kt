package com.example.stockflow.data.local.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stockflow.data.local.entity.UsuarioEntity

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarUsuario(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :nombre LIMIT 1")
    suspend fun buscarUsuario(nombre: String): UsuarioEntity?
}