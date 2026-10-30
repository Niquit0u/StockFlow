package com.example.stockflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stockflow.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    // IGNORE: si el nombre o el email ya existen devuelve -1 en vez de pisar la cuenta (y la contraseña) existente
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarUsuario(usuario: UsuarioEntity): Long

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contar(): Int

    @Query("SELECT * FROM usuarios WHERE nombreUsuario = :nombre LIMIT 1")
    suspend fun buscarUsuario(nombre: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    suspend fun buscarPorEmail(email: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun buscarPorId(id: Int): UsuarioEntity?

    // Dueño primero, después administradores y empleados, cada grupo por nombre
    @Query(
        """
        SELECT * FROM usuarios
        ORDER BY CASE rol WHEN 'DUENO' THEN 0 WHEN 'ADMINISTRADOR' THEN 1 ELSE 2 END, nombreUsuario COLLATE NOCASE
        """
    )
    fun observarTodos(): Flow<List<UsuarioEntity>>

    @Query("DELETE FROM usuarios WHERE id = :id")
    suspend fun eliminarPorId(id: Int)
}
