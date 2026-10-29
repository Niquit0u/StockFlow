package com.example.stockflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.stockflow.data.local.entity.DispositivoEntity

@Dao
interface DispositivoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(dispositivo: DispositivoEntity)

    // IGNORE: si el teléfono ya estaba registrado no se pisa su token
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun registrarSiNoExiste(dispositivo: DispositivoEntity)

    @Query("SELECT * FROM dispositivos WHERE deviceId = :deviceId")
    suspend fun obtener(deviceId: String): DispositivoEntity?

    @Query("DELETE FROM dispositivos WHERE deviceId = :deviceId")
    suspend fun eliminar(deviceId: String)
}