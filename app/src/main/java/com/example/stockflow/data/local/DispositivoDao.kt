package com.example.stockflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DispositivoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(dispositivo: DispositivoEntity)

    @Query("SELECT * FROM dispositivos WHERE deviceId = :deviceId")
    suspend fun obtener(deviceId: String): DispositivoEntity?

    @Query("DELETE FROM dispositivos WHERE deviceId = :deviceId")
    suspend fun eliminar(deviceId: String)
}