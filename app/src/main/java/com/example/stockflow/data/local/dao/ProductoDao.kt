package com.example.stockflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.stockflow.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {

    @Insert
    suspend fun insertar(producto: ProductoEntity): Long

    @Query("SELECT * FROM productos ORDER BY nombre")
    suspend fun obtenerTodos(): List<ProductoEntity>

    @Query("SELECT * FROM productos ORDER BY nombre")
    fun observarTodos(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE idProducto = :id")
    suspend fun obtenerPorId(id: Int): ProductoEntity?

    @Query("SELECT * FROM productos WHERE codigoBarra = :codigo")
    suspend fun obtenerPorCodigoBarra(codigo: String): ProductoEntity?

    @Query("SELECT * FROM productos WHERE codigoBarra IS NULL AND nombre = :nombre COLLATE NOCASE LIMIT 1")
    suspend fun obtenerSinCodigoPorNombre(nombre: String): ProductoEntity?

    @Query("SELECT COUNT(*) FROM productos")
    suspend fun contar(): Int

    @Update
    suspend fun actualizar(producto: ProductoEntity)

    @Delete
    suspend fun eliminar(producto: ProductoEntity)
}
