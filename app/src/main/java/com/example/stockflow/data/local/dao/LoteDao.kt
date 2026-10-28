package com.example.stockflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.stockflow.data.local.entity.LoteConProducto
import com.example.stockflow.data.local.entity.LoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LoteDao {

    @Insert
    suspend fun insertar(lote: LoteEntity): Long

    @Query("SELECT * FROM lotes")
    suspend fun obtenerTodos(): List<LoteEntity>

    @Query("SELECT * FROM lotes WHERE idLote = :id")
    suspend fun obtenerPorId(id: Int): LoteEntity?

    // Orden FEFO: primero el que vence antes
    @Query("SELECT * FROM lotes WHERE idProducto = :idProducto ORDER BY fechaVencimiento")
    suspend fun obtenerPorProducto(idProducto: Int): List<LoteEntity>

    @Query(
        """
        SELECT lotes.*, productos.nombre, productos.codigoBarra, productos.categoria
        FROM lotes
        INNER JOIN productos ON productos.idProducto = lotes.idProducto
        WHERE lotes.cantidadActual > 0
        ORDER BY lotes.idLote DESC
        """
    )
    fun observarConProducto(): Flow<List<LoteConProducto>>

    @Update
    suspend fun actualizar(lote: LoteEntity)

    @Delete
    suspend fun eliminar(lote: LoteEntity)

    @Query("DELETE FROM lotes")
    suspend fun eliminarTodos()
}
