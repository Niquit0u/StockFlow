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

    // Una categoría con productos con stock no se puede borrar.
    // Los productos sin stock (lotes eliminados, en 0) no se ven en Inventario, así que no cuentan.
    @Query(
        """
        SELECT COUNT(DISTINCT productos.idProducto) FROM productos
        INNER JOIN lotes ON lotes.idProducto = productos.idProducto
        WHERE productos.idCategoria = :idCategoria AND lotes.cantidadActual > 0
        """
    )
    suspend fun contarProductosConStock(idCategoria: Int): Int

    // Pasa todos los productos de una categoría a otra (los que quedan sin stock al borrarla)
    @Query("UPDATE productos SET idCategoria = :hacia WHERE idCategoria = :desde")
    suspend fun moverProductos(desde: Int, hacia: Int)

    @Query("DELETE FROM categorias WHERE idCategoria = :idCategoria")
    suspend fun eliminarPorId(idCategoria: Int)
}
