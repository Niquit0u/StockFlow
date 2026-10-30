package com.example.stockflow.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.example.stockflow.data.local.dao.DispositivoDao
import com.example.stockflow.data.local.entity.DispositivoEntity

class DispositivoRepository(private val dispositivoDao: DispositivoDao) {

    /**
     * Registra este teléfono la primera vez que se abre la app.
     * El token de notificaciones queda vacío hasta que se integre Firebase Cloud Messaging.
     */
    // ANDROID_ID es único por app + usuario del teléfono; no identifica al hardware
    @SuppressLint("HardwareIds")
    suspend fun registrarEsteDispositivo(context: Context) {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: return
        dispositivoDao.registrarSiNoExiste(DispositivoEntity(deviceId = deviceId))
    }
}
