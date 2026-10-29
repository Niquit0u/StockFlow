package com.example.stockflow.data.repository

import androidx.room.withTransaction
import com.example.stockflow.CambiosLote
import com.example.stockflow.EstadoSemaforo
import com.example.stockflow.ItemDescuento
import com.example.stockflow.LoteItem
import com.example.stockflow.data.local.StockFlowDatabase
import com.example.stockflow.data.local.entity.LoteConProducto
import com.example.stockflow.data.local.entity.LoteEntity
import com.example.stockflow.data.local.entity.MovimientoEntity
import com.example.stockflow.data.local.entity.ProductoEntity
import com.example.stockflow.data.local.entity.DetalleVentaEntity
import com.example.stockflow.data.local.entity.VentaEntity
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
    private val ventaDao = database.ventaDao()
    private val detalleVentaDao = database.detalleVentaDao()

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
     * Registra como venta todos los productos escaneados en "Descontar productos" y descuenta su stock,
     * en una sola transacción: si algo falla no se guarda nada.
     * - Crea la venta (fecha y monto total) y un detalle por producto con su subtotal.
     * - Descuenta por FEFO (primero los lotes que vencen antes) y registra un movimiento de SALIDA por lote.
     * Los lotes que quedan en 0 no se borran (sus movimientos los referencian); dejan de mostrarse.
     * Productos sin precio cargado suman $0 al total.
     *
     * @return el id de la venta creada.
     * @throws IllegalStateException si un producto no existe o no tiene stock suficiente.
     */
    suspend fun descontarStock(items: List<ItemDescuento>): Long = database.withTransaction {
        val idVenta = ventaDao.insertar(
            VentaEntity(
                fechaHora = System.currentTimeMillis(),
                montoTotal = items.sumOf { it.subtotal ?: 0.0 }
            )
        ).toInt()

        items.forEach { item ->
            val producto = productoDao.obtenerPorCodigoBarra(item.codigoBarra)
                ?: throw IllegalStateException("${item.nombreProducto} ya no existe")
            detalleVentaDao.insertar(
                DetalleVentaEntity(
                    idVenta = idVenta,
                    idProducto = producto.idProducto,
                    cantidad = item.cantidad,
                    subtotal = item.subtotal ?: 0.0
                )
            )
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

        idVenta.toLong()
    }

    /**
     * Guarda los cambios hechos desde el lápiz de un lote en Inventario.
     * Nombre, código de barras y precio son del producto (afectan a todos sus lotes);
     * cantidad y vencimiento son del lote.
     * Si cambia la cantidad se registra un movimiento de ENTRADA o SALIDA por la diferencia.
     *
     * @throws IllegalArgumentException si los datos no son válidos o el código ya es de otro producto.
     */
    suspend fun editarLote(cambios: CambiosLote) {
        val vencimiento = parsearFecha(cambios.fechaVencimiento)
            ?: throw IllegalArgumentException("Fecha inválida: '${cambios.fechaVencimiento}'. Usá DD/MM/AAAA")
        require(cambios.nombre.isNotBlank()) { "El nombre no puede estar vacío" }
        require(cambios.cantidad > 0) { "La cantidad debe ser mayor a 0" }
        require(cambios.precio == null || cambios.precio >= 0) { "El precio no puede ser negativo" }
        val codigo = normalizarCodigo(cambios.codigoBarra)

        database.withTransaction {
            val lote = loteDao.obtenerPorId(cambios.idLote)
                ?: throw IllegalArgumentException("El lote ya no existe")
            val producto = productoDao.obtenerPorId(lote.idProducto)
                ?: throw IllegalArgumentException("El producto ya no existe")

            // El código de barras es único: no puede quedar repetido en dos productos
            if (codigo != null && codigo != producto.codigoBarra) {
                productoDao.obtenerPorCodigoBarra(codigo)?.let {
                    throw IllegalArgumentException("El código $codigo ya pertenece a ${it.nombre}")
                }
            }

            productoDao.actualizar(
                producto.copy(
                    nombre = cambios.nombre.trim(),
                    codigoBarra = codigo,
                    precioVenta = cambios.precio ?: 0.0
                )
            )
            loteDao.actualizar(lote.copy(cantidadActual = cambios.cantidad, fechaVencimiento = vencimiento))

            val diferencia = cambios.cantidad - lote.cantidadActual
            if (diferencia != 0) {
                movimientoDao.insertar(
                    MovimientoEntity(
                        tipo = if (diferencia > 0) MovimientoEntity.TIPO_ENTRADA else MovimientoEntity.TIPO_SALIDA,
                        idLote = cambios.idLote,
                        cantidad = kotlin.math.abs(diferencia)
                    )
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
            estado = estadoPara(dias),
            // 0 = producto cargado sin precio (por ejemplo, los datos de prueba)
            precio = precioVenta.takeIf { it > 0 }
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
