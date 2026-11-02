package com.example.stockflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.stockflow.data.local.entity.EventoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoDao {

    @Insert
    suspend fun insertar(evento: EventoEntity): Long

    // Lo más reciente primero
    @Query("SELECT * FROM historial WHERE idLocal = :idLocal ORDER BY fechaHora DESC, idEvento DESC")
    fun observarDelLocal(idLocal: Int): Flow<List<EventoEntity>>
}
