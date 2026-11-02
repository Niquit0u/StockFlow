package com.example.stockflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.stockflow.data.local.entity.LocalEntity

@Dao
interface LocalDao {

    @Insert
    suspend fun insertar(local: LocalEntity): Long

    @Query("SELECT * FROM locales WHERE idLocal = :idLocal")
    suspend fun obtenerPorId(idLocal: Int): LocalEntity?
}
