package com.example.stockflow.data.repository

import androidx.room.withTransaction
import com.example.stockflow.EstadoSemaforo
import com.example.stockflow.ItemDescuento
import com.example.stockflow.LoteItem
import com.example.stockflow.data.local.StockFlowDatabase
import com.example.stockflow.data.local.entity.LoteConProducto
import com.example.stockflow.data.local.entity.LoteEntity
import com.example.stockflow.data.local.entity.MovimientoEntity
import com.example.stockflow.data.local.entity.ProductoEntity
import com.example.stockflow.datosSimuladosLotes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Puente entre la base de datos y las pantallas de lotes/vencimientos.
 * Las pantallas siguen trabajando con [LoteItem]; acá se convierte desde y hacia las entidades.
 */
class InventarioRepository(private val database: StockFlowDatabase) {

    private val productoDao = database.productoDao()
    private val loteDao = database.loteDao()
    private val movimientoDao = database.movimientoDao()

    fun observarLotes(): Flow<List<LoteItem>> {
        return loteDao.observarConProducto().map { lotes -> lotes.map { it.aLoteItem() } }
    }

    /**
     * Guarda un lote ingresado desde la pantalla "Ingresar Stock".
     * Crea el producto si no existe y registra el movimiento de ENTRADA, todo en una transacción.
     *
     * @throws IllegalArgumentException si la fecha no tiene formato dd/MM/yyyy o la cantidad no es positiva.
     */
    suspend fun registrarLote(lote: LoteItem): Long {
        val vencimiento = parsearFecha(lote.fechaVencimiento)
            ?: throw IllegalArgumentException("Fecha inválida: '${lote.fechaVencimiento}'. Usá DD/MM/AAAA")
        require(lote.cantidadActual > 0) { "La cantidad debe ser mayor a 0" }

        return registrar(
            nombre = lote.nombreProducto.trim(),
            codigoBarra = normalizarCodigo(lote.codigoBarra),
            categoria = lote.categoria,
            cantidad = lote.cantidadActual,
            vencimiento = vencimiento,
            precio = lote.precio
        )
    }

    /**
     * Descuenta del stock todos los productos escaneados, en una sola transacción:
     * si algo falla no se descuenta nada.
     * Usa FEFO (primero los lotes que vencen antes) y registra un movimiento de SALIDA por cada lote tocado.
     * Los lotes que quedan en 0 no se borran (sus movimientos los referencian); dejan de mostrarse.
     *
     * @throws IllegalStateException si un producto no existe o no tiene stock suficiente.
     */
    suspend fun descontarStock(items: List<ItemDescuento>) {
        database.withTransaction {
            items.forEach { item ->
                val producto = productoDao.obtenerPorCodigoBarra(item.codigoBarra)
                    ?: throw IllegalStateException("${item.nombreProducto} ya no existe")
                val lotes = loteDao.obtenerPorProducto(producto.idProducto).filter { it.cantidadActual > 0 }
                check(lotes.sumOf { it.cantidadActual } >= item.cantidad) {
                    "No hay stock suficiente de ${item.nombreProducto}"
                }

                var restante = item.cantidad
                for (lote in lotes) {
                    if (restante == 0) break
                    val aDescontar = minOf(restante, lote.cantidadActual)
                    loteDao.actualizar(lote.copy(cantidadActual = lote.cantidadActual - aDescontar))
                    movimientoDao.insertar(
                        MovimientoEntity(
                            tipo = MovimientoEntity.TIPO_SALIDA,
                            idLote = lote.idLote,
                            cantidad = aDescontar
                        )
                    )
                    restante -= aDescontar
                }
            }
        }
    }

    /** Carga los datos de ejemplo la primera vez que se abre la app, para que las pantallas no arranquen vacías. */
    suspend fun sembrarDatosDePruebaSiVacio() {
        if (productoDao.contar() > 0) return
        database.withTransaction {
            datosSimuladosLotes.distinct().forEach { lote ->
                // Fechas relativas a hoy para que el semáforo se vea igual que en la maqueta
                registrar(
                    nombre = lote.nombreProducto,
                    codigoBarra = normalizarCodigo(lote.codigoBarra),
                    categoria = lote.categoria,
                    cantidad = lote.cantidadActual,
                    vencimiento = inicioDelDia(System.currentTimeMillis()) +
                        TimeUnit.DAYS.toMillis(lote.diasParaVencer.toLong())
                )
            }
        }
    }

    private suspend fun registrar(
        nombre: String,
        codigoBarra: String?,
        categoria: String,
        cantidad: Int,
        vencimiento: Long,
        precio: Double? = null
    ): Long = database.withTransaction {
        val existente = if (codigoBarra != null) {
            productoDao.obtenerPorCodigoBarra(codigoBarra)
        } else {
            productoDao.obtenerSinCodigoPorNombre(nombre)
        }
        val idProducto = existente?.idProducto ?: productoDao.insertar(
            ProductoEntity(
                nombre = nombre,
                codigoBarra = codigoBarra,
                categoria = categoria,
                precioVenta = precio ?: 0.0
            )
        ).toInt()

        val idLote = loteDao.insertar(
            LoteEntity(idProducto = idProducto, cantidadActual = cantidad, fechaVencimiento = vencimiento)
        )
        movimientoDao.insertar(
            MovimientoEntity(tipo = MovimientoEntity.TIPO_ENTRADA, idLote = idLote.toInt(), cantidad = cantidad)
        )
        idLote
    }

    private fun LoteConProducto.aLoteItem(): LoteItem {
        val dias = diasHasta(lote.fechaVencimiento)
        return LoteItem(
            idLote = lote.idLote.toString(),
            nombreProducto = nombre,
            categoria = categoria,
            codigoBarra = codigoBarra.orEmpty(),
            cantidadActual = lote.cantidadActual,
            fechaVencimiento = formatoFecha().format(lote.fechaVencimiento),
            diasParaVencer = dias,
            estado = estadoPara(dias)
        )
    }

    companion object {
        /** Código que pone PantallaAgregar cuando el usuario deja el campo vacío; no es un producto real. */
        private const val CODIGO_GENERICO = "779000000000"

        private const val DIAS_CRITICO = 7
        private const val DIAS_ADVERTENCIA = 15

        fun estadoPara(diasParaVencer: Int): EstadoSemaforo = when {
            diasParaVencer <= DIAS_CRITICO -> EstadoSemaforo.CRITICO
            diasParaVencer <= DIAS_ADVERTENCIA -> EstadoSemaforo.ADVERTENCIA
            else -> EstadoSemaforo.OPTIMO
        }

        private fun normalizarCodigo(codigo: String): String? =
            codigo.trim().takeUnless { it.isEmpty() || it == CODIGO_GENERICO }

        // SimpleDateFormat no es thread-safe: una instancia por uso
        private fun formatoFecha() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }

        fun parsearFecha(texto: String): Long? = try {
            formatoFecha().parse(texto.trim())?.time?.let(::inicioDelDia)
        } catch (_: ParseException) {
            null
        }

        private fun inicioDelDia(millis: Long): Long = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        private fun diasHasta(vencimiento: Long): Int {
            val diferencia = inicioDelDia(vencimiento) - inicioDelDia(System.currentTimeMillis())
            // round: los cambios de horario de verano pueden dejar días de 23 o 25 horas
            return Math.round(diferencia / TimeUnit.DAYS.toMillis(1).toDouble()).toInt()
        }
    }
}
