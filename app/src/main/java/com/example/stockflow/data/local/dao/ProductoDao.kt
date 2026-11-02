package com.example.stockflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.stockflow.data.local.entity.ProductoEntity

@Dao
interface ProductoDao {

    @Insert
    suspend fun insertar(producto: ProductoEntity): Long

    @Query("SELECT * FROM productos WHERE idProducto = :id")
    suspend fun obtenerPorId(id: Int): ProductoEntity?

    @Query("SELECT * FROM productos WHERE idLocal = :idLocal AND codigoBarra = :codigo")
    suspend fun obtenerPorCodigoBarra(idLocal: Int, codigo: String): ProductoEntity?

    @Query(
        "SELECT * FROM productos WHERE idLocal = :idLocal AND codigoBarra IS NULL AND nombre = :nombre COLLATE NOCASE LIMIT 1"
    )
    suspend fun obtenerSinCodigoPorNombre(idLocal: Int, nombre: String): ProductoEntity?

    @Update
    suspend fun actualizar(producto: ProductoEntity)

    @Delete
    suspend fun eliminar(producto: ProductoEntity)
}
