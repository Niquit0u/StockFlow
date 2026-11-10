package com.example.stockflow.notificaciones

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.stockflow.data.local.StockFlowDatabase
import com.example.stockflow.data.repository.SesionRepository
import com.example.stockflow.data.repository.UsuarioRepository

/** Revisa los lotes del local de la sesión guardada y manda los avisos de vencimiento pendientes. */
class RevisarVencimientosWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = StockFlowDatabase.getDatabase(applicationContext)
        // Sin sesión iniciada no se sabe de qué local avisar
        val sesion = SesionRepository(applicationContext, UsuarioRepository(database)).restaurar()
            ?: return Result.success()
        val lotes = database.loteDao().obtenerConProducto(sesion.idLocal)
        AvisosVencimiento.avisar(applicationContext, lotes)
        return Result.success()
    }
}
