package com.example.stockflow.data.repository

import androidx.room.withTransaction
import com.example.stockflow.CambiosLote
import com.example.stockflow.EstadoSemaforo
import com.example.stockflow.sePuedeVender
import com.example.stockflow.NOMBRE_SIN_CATEGORIA
import com.example.stockflow.ItemDescuento
import com.example.stockflow.LoteItem
import com.example.stockflow.SesionUsuario
import com.example.stockflow.TipoEvento
import com.example.stockflow.formatearPrecio
import com.example.stockflow.data.local.entity.EventoEntity
import com.example.stockflow.data.local.StockFlowDatabase
import com.example.stockflow.data.local.entity.LoteConProducto
import com.example.stockflow.data.local.entity.LoteEntity
import com.example.stockflow.data.local.entity.MovimientoEntity
import com.example.stockflow.data.local.entity.ProductoEntity
import com.example.stockflow.data.local.entity.CategoriaEntity
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
class InventarioRepository(
    private val database: StockFlowDatabase,
    // Usuario que está usando la app: queda registrado en el historial de lo que hace
    private val sesion: SesionUsuario
) {

    // Local de la sesión: solo se ven y modifican sus productos, lotes y ventas
    private val idLocal = sesion.idLocal

    private val productoDao = database.productoDao()
    private val loteDao = database.loteDao()
    private val movimientoDao = database.movimientoDao()
    private val categoriaDao = database.categoriaDao()
    private val ventaDao = database.ventaDao()
    private val detalleVentaDao = database.detalleVentaDao()
    private val eventoDao = database.eventoDao()

    /** Agrega una entrada al historial del local (se llama dentro de la transacción de cada operación). */
    private suspend fun registrarEnHistorial(
        tipo: TipoEvento,
        titulo: String,
        detalle: List<String> = emptyList(),
        monto: Double? = null
    ) {
        eventoDao.insertar(
            EventoEntity(
                idLocal = idLocal,
                tipo = tipo.name,
                idUsuario = sesion.idUsuario,
                usuario = sesion.nombre,
                titulo = titulo,
                detalle = detalle.joinToString("\n"),
                monto = monto
            )
        )
    }

    fun observarLotes(): Flow<List<LoteItem>> {
        return loteDao.observarConProducto(idLocal).map { lotes -> lotes.map { it.aLoteItem() } }
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
     * - Crea la venta (fecha, monto total y usuario que la hizo) y un detalle por producto con su subtotal.
     * - Descuenta por FEFO (primero los lotes que vencen antes) y registra un movimiento de SALIDA por lote.
     *   Los lotes vencidos o a 1 día de vencer (en rojo) no se venden: se saltean.
     * Los lotes que quedan en 0 no se borran (sus movimientos los referencian); dejan de mostrarse.
     * Productos sin precio cargado suman $0 al total.
     *
     * También queda en el historial, con cada producto vendido.
     *
     * @return el id de la venta creada.
     * @throws IllegalStateException si un producto no existe o no tiene stock sin vencer suficiente.
     */
    suspend fun descontarStock(items: List<ItemDescuento>): Long = database.withTransaction {
        val total = items.sumOf { it.subtotal ?: 0.0 }
        val idVenta = ventaDao.insertar(
            VentaEntity(
                fechaHora = System.currentTimeMillis(),
                montoTotal = total,
                idLocal = idLocal,
                idUsuario = sesion.idUsuario,
                vendedor = sesion.nombre
            )
        ).toInt()

        items.forEach { item ->
            val producto = productoDao.obtenerPorCodigoBarra(idLocal, item.codigoBarra)
                ?: throw IllegalStateException("${item.nombreProducto} ya no existe")
            detalleVentaDao.insertar(
                DetalleVentaEntity(
                    idVenta = idVenta,
                    idProducto = producto.idProducto,
                    cantidad = item.cantidad,
                    subtotal = item.subtotal ?: 0.0
                )
            )
            val lotes = loteDao.obtenerPorProducto(producto.idProducto)
                .filter { it.cantidadActual > 0 && sePuedeVender(diasHasta(it.fechaVencimiento)) }
            check(lotes.sumOf { it.cantidadActual } >= item.cantidad) {
                "No hay stock vendible suficiente de \"${item.nombreProducto}\" (lo vencido o por vencer no se vende)"
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

        registrarEnHistorial(
            tipo = TipoEvento.VENTA,
            titulo = "Venta #$idVenta",
            detalle = items.map { item ->
                "${item.nombreProducto} x${item.cantidad}: " + (item.subtotal?.let(::formatearPrecio) ?: "sin precio")
            },
            monto = total
        )

        idVenta.toLong()
    }

    /**
     * Guarda los cambios hechos desde el lápiz de un lote en Inventario.
     * Nombre, código de barras, precio y categoría son del producto (afectan a todos sus lotes);
     * cantidad y vencimiento son del lote.
     * Si cambia la cantidad se registra un movimiento de ENTRADA o SALIDA por la diferencia.
     * Lo que haya cambiado queda en el historial ("Precio: $100,00 → $120,00").
     *
     * @throws IllegalArgumentException si los datos no son válidos o el código ya es de otro producto.
     */
    /**
     * Elimina los lotes elegidos desde Inventario: quedan en 0 unidades y dejan de mostrarse
     * (no se borran de la base porque sus movimientos y ventas los referencian).
     * Registra un movimiento de SALIDA por lo que tenía cada uno y lo anota en el historial.
     *
     * @return las unidades que se dieron de baja en total.
     * @throws IllegalArgumentException si algún lote no existe o es de otro local.
     */
    suspend fun eliminarLotes(idsLotes: List<Int>): Int = database.withTransaction {
        var unidades = 0
        // Detalle para el historial, agrupado por producto
        val detallePorProducto = linkedMapOf<String, MutableList<String>>()

        for (idLote in idsLotes.distinct()) {
            val lote = loteDao.obtenerPorId(idLote)
                ?: throw IllegalArgumentException("El lote $idLote ya no existe")
            val producto = productoDao.obtenerPorId(lote.idProducto)
                ?.takeIf { it.idLocal == idLocal }
                ?: throw IllegalArgumentException("El lote $idLote no es de este local")
            if (lote.cantidadActual <= 0) continue

            loteDao.actualizar(lote.copy(cantidadActual = 0))
            movimientoDao.insertar(
                MovimientoEntity(tipo = MovimientoEntity.TIPO_SALIDA, idLote = idLote, cantidad = lote.cantidadActual)
            )
            unidades += lote.cantidadActual
            detallePorProducto.getOrPut(producto.nombre) { mutableListOf() }.add(
                "Lote #$idLote: ${lote.cantidadActual} u. (vencía ${formatoFecha().format(lote.fechaVencimiento)})"
            )
        }

        detallePorProducto.forEach { (nombre, detalle) ->
            registrarEnHistorial(
                tipo = TipoEvento.CAMBIO,
                titulo = if (detalle.size == 1) "Lote eliminado de $nombre" else "${detalle.size} lotes eliminados de $nombre",
                detalle = detalle
            )
        }
        unidades
    }

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
                ?.takeIf { it.idLocal == idLocal } // un lote de otro local no se puede editar
                ?: throw IllegalArgumentException("El producto ya no existe")

            // El código de barras es único: no puede quedar repetido en dos productos
            if (codigo != null && codigo != producto.codigoBarra) {
                productoDao.obtenerPorCodigoBarra(idLocal, codigo)?.let {
                    throw IllegalArgumentException("El código $codigo ya pertenece a ${it.nombre}")
                }
            }

            val productoNuevo = producto.copy(
                nombre = cambios.nombre.trim(),
                codigoBarra = codigo,
                precioVenta = cambios.precio ?: 0.0,
                idCategoria = idCategoriaPorNombre(cambios.categoria)
            )
            productoDao.actualizar(productoNuevo)
            loteDao.actualizar(lote.copy(cantidadActual = cambios.cantidad, fechaVencimiento = vencimiento))

            // Historial: solo lo que realmente cambió
            val categoriaAntes = categoriaDao.obtenerPorId(producto.idCategoria)?.nombre.orEmpty()
            val categoriaDespues = categoriaDao.obtenerPorId(productoNuevo.idCategoria)?.nombre.orEmpty()
            val diferencias = buildList {
                if (producto.nombre != productoNuevo.nombre) add("Nombre: ${producto.nombre} → ${productoNuevo.nombre}")
                if (producto.codigoBarra != productoNuevo.codigoBarra) {
                    add("Código: ${producto.codigoBarra ?: "sin código"} → ${productoNuevo.codigoBarra ?: "sin código"}")
                }
                if (producto.precioVenta != productoNuevo.precioVenta) {
                    add("Precio: ${textoPrecio(producto.precioVenta)} → ${textoPrecio(productoNuevo.precioVenta)}")
                }
                if (categoriaAntes != categoriaDespues) add("Categoría: $categoriaAntes → $categoriaDespues")
                if (lote.cantidadActual != cambios.cantidad) add("Cantidad: ${lote.cantidadActual} → ${cambios.cantidad} u.")
                if (lote.fechaVencimiento != vencimiento) {
                    add("Vencimiento: ${formatoFecha().format(lote.fechaVencimiento)} → ${formatoFecha().format(vencimiento)}")
                }
            }
            if (diferencias.isNotEmpty()) {
                registrarEnHistorial(
                    tipo = TipoEvento.CAMBIO,
                    titulo = "Cambios en ${productoNuevo.nombre} (lote #${lote.idLote})",
                    detalle = diferencias
                )
            }

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

    /**
     * Id de la categoría del local con ese nombre. Si no existe (por ejemplo, la borraron mientras se
     * completaba el formulario) usa "Sin categoría" o la primera que haya, creándola si el local no tiene ninguna.
     */
    private suspend fun idCategoriaPorNombre(nombre: String): Int {
        categoriaDao.buscarPorNombre(idLocal, nombre.trim())?.let { return it.idCategoria }
        categoriaDao.buscarPorNombre(idLocal, NOMBRE_SIN_CATEGORIA)?.let { return it.idCategoria }
        categoriaDao.obtenerDelLocal(idLocal).firstOrNull()?.let { return it.idCategoria }
        return categoriaDao.insertar(CategoriaEntity(idLocal = idLocal, nombre = NOMBRE_SIN_CATEGORIA)).toInt()
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
            productoDao.obtenerPorCodigoBarra(idLocal, codigoBarra)
        } else {
            productoDao.obtenerSinCodigoPorNombre(idLocal, nombre)
        }
        val idProducto = existente?.idProducto ?: productoDao.insertar(
            ProductoEntity(
                idLocal = idLocal,
                nombre = nombre,
                codigoBarra = codigoBarra,
                idCategoria = idCategoriaPorNombre(categoria),
                precioVenta = precio ?: 0.0
            )
        ).toInt()

        // Un producto que existía pero sin stock (no se ve en Inventario) toma la categoría elegida.
        // Si todavía tiene stock se respeta la suya: no se mueve algo que el usuario ya ve en otra categoría.
        var categoriaNueva: String? = null
        if (existente != null && loteDao.obtenerPorProducto(existente.idProducto).none { it.cantidadActual > 0 }) {
            val idCategoria = idCategoriaPorNombre(categoria)
            if (idCategoria != existente.idCategoria) {
                productoDao.actualizar(existente.copy(idCategoria = idCategoria))
                categoriaNueva = categoriaDao.obtenerPorId(idCategoria)?.nombre ?: categoria
            }
        }

        val idLote = loteDao.insertar(
            LoteEntity(idProducto = idProducto, cantidadActual = cantidad, fechaVencimiento = vencimiento)
        )
        movimientoDao.insertar(
            MovimientoEntity(tipo = MovimientoEntity.TIPO_ENTRADA, idLote = idLote.toInt(), cantidad = cantidad)
        )

        registrarEnHistorial(
            tipo = TipoEvento.INGRESO,
            titulo = "Ingreso de $cantidad u. de $nombre",
            detalle = buildList {
                add("Lote #$idLote, vence ${formatoFecha().format(vencimiento)}")
                if (existente == null) {
                    add("Producto nuevo en ${categoriaDao.obtenerPorId(idCategoriaPorNombre(categoria))?.nombre ?: categoria}")
                    add("Precio: ${textoPrecio(precio ?: 0.0)}")
                }
                categoriaNueva?.let { add("Pasó a la categoría $it") }
            }
        )
        idLote
    }

    private fun textoPrecio(precio: Double) = if (precio > 0) formatearPrecio(precio) else "sin precio"

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

        private const val DIAS_ADVERTENCIA = 14

        fun estadoPara(diasParaVencer: Int): EstadoSemaforo = when {
            !sePuedeVender(diasParaVencer) -> EstadoSemaforo.CRITICO
            diasParaVencer <= DIAS_ADVERTENCIA -> EstadoSemaforo.ADVERTENCIA
            else -> EstadoSemaforo.OPTIMO
        }

        private fun normalizarCodigo(codigo: String): String? =
            codigo.trim().takeUnless { it.isEmpty() || it == CODIGO_GENERICO }

        // SimpleDateFormat no es thread-safe: una instancia por uso
        fun formatoFecha() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }

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

        /** Días que faltan para [vencimiento] (0 = vence hoy, negativo = ya venció). */
        fun diasHasta(vencimiento: Long): Int {
            val diferencia = inicioDelDia(vencimiento) - inicioDelDia(System.currentTimeMillis())
            // round: los cambios de horario de verano pueden dejar días de 23 o 25 horas
            return Math.round(diferencia / TimeUnit.DAYS.toMillis(1).toDouble()).toInt()
        }
    }
}
