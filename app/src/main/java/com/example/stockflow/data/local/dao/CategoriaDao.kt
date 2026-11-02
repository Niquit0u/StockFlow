package com.example.stockflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.stockflow.data.local.entity.CategoriaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {

    @Insert
    suspend fun insertar(categoria: CategoriaEntity): Long

    // En el orden que eligió el usuario
    @Query("SELECT * FROM categorias WHERE idLocal = :idLocal ORDER BY orden, nombre COLLATE NOCASE")
    fun observarDelLocal(idLocal: Int): Flow<List<CategoriaEntity>>

    @Query("SELECT * FROM categorias WHERE idLocal = :idLocal ORDER BY orden, nombre COLLATE NOCASE")
    suspend fun obtenerDelLocal(idLocal: Int): List<CategoriaEntity>

    // Posición para una categoría nueva: queda última
    @Query("SELECT COALESCE(MAX(orden) + 1, 0) FROM categorias WHERE idLocal = :idLocal")
    suspend fun siguienteOrden(idLocal: Int): Int

    @Query("UPDATE categorias SET orden = :orden WHERE idCategoria = :idCategoria AND idLocal = :idLocal")
    suspend fun cambiarOrden(idLocal: Int, idCategoria: Int, orden: Int)

    @Query("SELECT * FROM categorias WHERE idLocal = :idLocal AND nombre = :nombre COLLATE NOCASE LIMIT 1")
    suspend fun buscarPorNombre(idLocal: Int, nombre: String): CategoriaEntity?

    @Query("SELECT * FROM categorias WHERE idCategoria = :idCategoria")
    suspend fun obtenerPorId(idCategoria: Int): CategoriaEntity?

    // Antes de borrar una categoría sus productos pasan a otra
    @Query("UPDATE productos SET idCategoria = :hacia WHERE idCategoria = :desde")
    suspend fun moverProductos(desde: Int, hacia: Int): Int

    @Query("DELETE FROM categorias WHERE idCategoria = :idCategoria")
    suspend fun eliminarPorId(idCategoria: Int)
}
