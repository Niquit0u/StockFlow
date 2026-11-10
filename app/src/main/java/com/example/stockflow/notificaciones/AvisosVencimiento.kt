package com.example.stockflow.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.stockflow.MainActivity
import com.example.stockflow.R
import com.example.stockflow.data.local.entity.LoteConProducto
import com.example.stockflow.data.repository.InventarioRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Avisos de vencimiento: a 14 días, a 7 días y cuando el lote ya venció.
 * Cada aviso se manda una sola vez por lote (se recuerda en preferencias cuál fue el último).
 */
object AvisosVencimiento {

    /** Escalones de aviso, de menos a más urgente. */
    enum class Nivel { CATORCE_DIAS, SIETE_DIAS, VENCIDO }

    private const val ID_CANAL = "vencimientos"
    private const val PREFERENCIAS = "avisos_vencimiento"
    private const val TRABAJO_DIARIO = "revisar_vencimientos_diario"
    private const val TRABAJO_AHORA = "revisar_vencimientos_ahora"
    private const val HORA_DEL_AVISO = 9 // la revisión diaria corre cerca de las 9 de la mañana

    fun nivelPara(diasParaVencer: Int): Nivel? = when {
        diasParaVencer < 0 -> Nivel.VENCIDO
        diasParaVencer <= 7 -> Nivel.SIETE_DIAS
        diasParaVencer <= 14 -> Nivel.CATORCE_DIAS
        else -> null
    }

    fun textoAviso(nombre: String, diasParaVencer: Int): String {
        val producto = "\"$nombre\""
        return when {
            diasParaVencer < 0 -> "$producto ya venció"
            diasParaVencer == 0 -> "$producto vence hoy"
            diasParaVencer == 1 -> "$producto a 1 día de vencer"
            else -> "$producto a $diasParaVencer días de vencer"
        }
    }

    /** Programa la revisión diaria (si no estaba) y además corre una ahora. Se llama al abrir la app. */
    fun programar(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val diaria = PeriodicWorkRequestBuilder<RevisarVencimientosWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(milisHastaProximoAviso(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(TRABAJO_DIARIO, ExistingPeriodicWorkPolicy.KEEP, diaria)
        revisarAhora(context)
    }

    fun revisarAhora(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            TRABAJO_AHORA,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RevisarVencimientosWorker>().build()
        )
    }

    /**
     * Manda los avisos que correspondan a [lotes] y que todavía no se hayan mandado.
     * Si las notificaciones están bloqueadas no anota nada, así se avisan cuando se habiliten.
     */
    fun avisar(context: Context, lotes: List<LoteConProducto>) {
        if (!puedeNotificar(context)) return
        crearCanal(context)
        val preferencias = context.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
        val manager = NotificationManagerCompat.from(context)

        preferencias.edit {
            for (item in lotes) {
                val lote = item.lote
                val dias = InventarioRepository.diasHasta(lote.fechaVencimiento)
                val nivel = nivelPara(dias) ?: continue
                val clave = "lote_${lote.idLote}"
                // Se guarda "fecha:nivel": si cambian la fecha del lote, los avisos empiezan de nuevo
                val anterior = preferencias.getString(clave, null)?.split(":")
                val yaAvisado = anterior != null &&
                    anterior[0] == lote.fechaVencimiento.toString() &&
                    (anterior.getOrNull(1)?.toIntOrNull() ?: -1) >= nivel.ordinal
                if (yaAvisado) continue

                try {
                    manager.notify(lote.idLote, construir(context, item, dias))
                } catch (_: SecurityException) {
                    return@edit // se quitó el permiso mientras corría
                }
                putString(clave, "${lote.fechaVencimiento}:${nivel.ordinal}")
            }
            // Olvidar lotes que ya no existen o se quedaron sin stock
            val vigentes = lotes.map { "lote_${it.lote.idLote}" }.toSet()
            preferencias.all.keys.filter { it.startsWith("lote_") && it !in vigentes }.forEach(::remove)
        }
    }

    private fun construir(context: Context, item: LoteConProducto, dias: Int) =
        NotificationCompat.Builder(context, ID_CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setColor(ContextCompat.getColor(context, R.color.flujo))
            .setContentTitle(textoAviso(item.nombre, dias))
            .setContentText(
                "${item.lote.cantidadActual} u. · vence el " +
                    InventarioRepository.formatoFecha().format(item.lote.fechaVencimiento)
            )
            // Prioridad alta: aparece como ventana arriba de la pantalla (Android 7 y anteriores)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(abrirApp(context))
            .setAutoCancel(true)
            .build()

    private fun abrirApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // Importancia alta: Android 8+ la muestra como ventana emergente arriba de la pantalla
        val canal = NotificationChannel(ID_CANAL, "Vencimientos", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisos de productos por vencer o vencidos"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private fun puedeNotificar(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun milisHastaProximoAviso(): Long {
        val ahora = Calendar.getInstance()
        val proximo = (ahora.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, HORA_DEL_AVISO)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(ahora)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return proximo.timeInMillis - ahora.timeInMillis
    }
}
