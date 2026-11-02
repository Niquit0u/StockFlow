package com.example.stockflow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.LocalViewConfiguration
import com.example.stockflow.ui.componentes.EstadoReordenar
import com.example.stockflow.ui.theme.Semaforo
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stockflow.ui.componentes.CampoCantidad
import com.example.stockflow.ui.componentes.CampoFecha
import com.example.stockflow.ui.componentes.SelectorCategoria
import com.example.stockflow.ui.componentes.esFechaValida
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.stockflow.ui.componentes.visibleAlEscribir
import com.example.stockflow.ui.componentes.TituloPantalla
import com.example.stockflow.ui.componentes.coloresBarraTitulo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAlertas(
    lotes: List<LoteItem>,
    busquedaInicial: String = "", // Texto a buscar al llegar desde el escáner
    // Guarda los cambios hechos a un lote con el lápiz; null = sin permiso (no se muestra el lápiz)
    onEditarLote: ((CambiosLote) -> Unit)? = null,
    // Abre el historial (ventas, ingresos y cambios); null = sin permiso (no se muestra el botón)
    onAbrirHistorial: (() -> Unit)? = null,
    // Categorías del local (siempre hay al menos una)
    categorias: List<CategoriaItem> = emptyList(),
    // Crea una categoría; devuelve null si se creó o el error para mostrar. null = sin permiso
    onCrearCategoria: (suspend (String) -> String?)? = null,
    // Elimina una categoría (sus productos pasan a otra). null = sin permiso
    onEliminarCategoria: ((CategoriaItem) -> Unit)? = null,
    // Guarda el orden elegido arrastrando (ids de arriba a abajo). null = sin permiso para reordenar
    onReordenarCategorias: ((List<Int>) -> Unit)? = null
) {

    /*
     * Orden de las categorías mientras se arrastra: se mueve en esta copia y al soltar se guarda.
     * Fuera del arrastre se mantiene igual a lo que viene de la base.
     */
    val ordenCategorias = remember { mutableStateListOf<CategoriaItem>() }
    val estadoListaCategorias = rememberLazyListState()
    val estadoReordenar = remember(estadoListaCategorias) {
        EstadoReordenar(
            lista = estadoListaCategorias,
            // Solo entre las categorías (índices 0 a n-1): nunca debajo de "Agregar categoría"
            rango = { 0..(ordenCategorias.size - 1).coerceAtLeast(0) },
            onMover = { desde, hacia -> ordenCategorias.add(hacia, ordenCategorias.removeAt(desde)) }
        )
    }

    LaunchedEffect(categorias) {
        if (estadoReordenar.indiceArrastrado == null) {
            ordenCategorias.clear()
            ordenCategorias.addAll(categorias)
        }
    }

    fun terminarArrastre() {
        estadoReordenar.terminar()
        val ids = ordenCategorias.map { it.idCategoria }
        // Con una sola categoría (o si se soltó en el mismo lugar) no cambia nada y no se guarda
        if (ids != categorias.map { it.idCategoria }) onReordenarCategorias?.invoke(ids)
    }

    val vibracion = LocalHapticFeedback.current

    // Igual a la configuración del sistema, pero el "mantener apretado" se activa más rápido
    val configuracionSistema = LocalViewConfiguration.current
    val configuracionArrastreRapido = remember(configuracionSistema) {
        object : ViewConfiguration by configuracionSistema {
            override val longPressTimeoutMillis: Long = ESPERA_ARRASTRE_MS
        }
    }

    var mostrarNuevaCategoria by remember {
        mutableStateOf(false)
    }

    // Modo eliminar: las categorías se pintan de rojo con un tacho al lado
    var modoEliminarCategorias by remember {
        mutableStateOf(false)
    }

    // Aviso cuando se intenta eliminar y queda una sola categoría
    var avisoUltimaCategoria by remember {
        mutableStateOf(false)
    }

    // Si al borrar queda una sola categoría, se sale solo del modo eliminar
    LaunchedEffect(categorias.size) {
        if (categorias.size <= 1) modoEliminarCategorias = false
    }

    // Cantidad de productos distintos (con stock) en cada categoría, para mostrar en las tarjetas
    val productosPorCategoria = remember(lotes) {
        lotes.groupBy { it.categoria }.mapValues { (_, lotesCategoria) ->
            lotesCategoria.map { it.nombreProducto }.distinct().size
        }
    }

    // Lote abierto en el diálogo de edición (lápiz)
    var loteEnEdicion by remember {
        mutableStateOf<LoteItem?>(null)
    }

    var categoriaSeleccionada by remember {
        mutableStateOf<String?>(null)
    }

    var textoBusqueda by remember {
        mutableStateOf(busquedaInicial)
    }

    var mostrarBusqueda by remember {
        mutableStateOf(busquedaInicial.isNotBlank())
    }

    // Al tocar la lupa el buscador recibe el foco y se abre el teclado.
    // No pasa cuando se llega desde el escáner con una búsqueda ya cargada.
    var enfocarBusqueda by remember {
        mutableStateOf(false)
    }
    val focoBusqueda = remember { FocusRequester() }
    val teclado = LocalSoftwareKeyboardController.current

    LaunchedEffect(mostrarBusqueda, enfocarBusqueda) {
        if (mostrarBusqueda && enfocarBusqueda) {
            focoBusqueda.requestFocus()
            teclado?.show()
            enfocarBusqueda = false
        }
    }

    fun cerrarBusqueda() {
        mostrarBusqueda = false
        textoBusqueda = ""
        teclado?.hide()
    }

    var ordenSeleccionado by remember {
        mutableStateOf("Próximo a vencer")
    }

    var menuOrdenExpandido by remember {
        mutableStateOf(false)
    }

    /*
     * Producto que debe quedar expandido cuando
     * llegamos a una categoría desde la búsqueda.
     */
    var productoAExpandir by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Resultados de búsqueda global.
     *
     * Busca por nombre del producto en TODOS los lotes,
     * independientemente de la categoría.
     */
    val resultadosBusqueda = remember(
        lotes,
        textoBusqueda
    ) {

        if (textoBusqueda.isBlank()) {
            emptyList()
        } else {

            lotes
                .filter {
                    it.nombreProducto.contains(
                        textoBusqueda,
                        ignoreCase = true
                    )
                }
                .groupBy {
                    it.nombreProducto
                }
                .map { (_, listaLotes) ->

                    val primerLote = listaLotes.first()

                    Producto(
                        nombreProducto = primerLote.nombreProducto,
                        categoria = primerLote.categoria,
                        codigoBarra = primerLote.codigoBarra,
                        totalCantidad = listaLotes.sumOf {
                            it.cantidadActual
                        },
                        lotes = listaLotes
                    )
                }
                .sortedBy {
                    it.nombreProducto
                }
        }
    }

    /*
     * Filtramos los lotes según la categoría seleccionada
     * y el texto de búsqueda.
     */
    val lotesFiltrados = remember(
        lotes,
        categoriaSeleccionada,
        textoBusqueda,
        mostrarBusqueda
    ) {

        lotes.filter { lote ->

            val coincideCategoria =
                categoriaSeleccionada == null ||
                        lote.categoria.equals(
                            categoriaSeleccionada,
                            ignoreCase = true
                        )

            val coincideBusqueda =
                !mostrarBusqueda ||
                        textoBusqueda.isBlank() ||
                        lote.nombreProducto.contains(
                            textoBusqueda,
                            ignoreCase = true
                        )

            coincideCategoria && coincideBusqueda
        }
    }

    /*
     * Agrupamos los lotes por producto.
     */
    val productosAgrupados = remember(
        lotesFiltrados,
        ordenSeleccionado
    ) {

        val productos = lotesFiltrados
            .groupBy {
                it.nombreProducto
            }
            .map { (nombre, listaLotes) ->

                val lotesOrdenados = when (
                    ordenSeleccionado
                ) {

                    "Próximo a vencer" ->
                        listaLotes.sortedBy {
                            convertirFecha(
                                it.fechaVencimiento
                            )
                        }

                    "Más lejano a vencer" ->
                        listaLotes.sortedByDescending {
                            convertirFecha(
                                it.fechaVencimiento
                            )
                        }

                    else ->
                        listaLotes.sortedBy {
                            convertirFecha(
                                it.fechaVencimiento
                            )
                        }
                }

                Producto(
                    nombreProducto = nombre,
                    categoria = listaLotes.firstOrNull()
                        ?.categoria ?: "General",

                    codigoBarra = listaLotes.firstOrNull()
                        ?.codigoBarra ?: "",

                    totalCantidad = listaLotes.sumOf {
                        it.cantidadActual
                    },

                    lotes = lotesOrdenados
                )
            }

        /*
         * También ordenamos los PRODUCTOS completos,
         * no solamente los lotes.
         */
        when (ordenSeleccionado) {

            "Próximo a vencer" ->
                productos.sortedBy {
                    convertirFecha(
                        it.lotes.firstOrNull()
                            ?.fechaVencimiento
                            ?: "31/12/2099"
                    )
                }

            "Más lejano a vencer" ->
                productos.sortedByDescending {
                    convertirFecha(
                        it.lotes.firstOrNull()
                            ?.fechaVencimiento
                            ?: "01/01/1970"
                    )
                }

            else ->
                productos.sortedBy {
                    it.nombreProducto
                }
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    if (mostrarBusqueda) {

                        TextField(
                            value = textoBusqueda,

                            onValueChange = {
                                textoBusqueda = it
                            },

                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp)
                                .focusRequester(focoBusqueda)
                                .visibleAlEscribir(),

                            placeholder = {
                                Text("Buscar producto")
                            },

                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },

                            // Borra lo escrito sin cerrar el buscador
                            trailingIcon = {
                                if (textoBusqueda.isNotEmpty()) {
                                    IconButton(onClick = { textoBusqueda = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Borrar búsqueda"
                                        )
                                    }
                                }
                            },

                            singleLine = true,

                            shape = RoundedCornerShape(50),

                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                // Sin la línea inferior: la forma de píldora ya marca el campo
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),

                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),

                            // "Buscar" en el teclado lo cierra para ver los resultados
                            keyboardActions = KeyboardActions(onSearch = { teclado?.hide() })
                        )

                    } else {

                        // Dentro de una categoría el subtítulo indica de dónde se viene
                        TituloPantalla(
                            titulo = categoriaSeleccionada ?: "Inventario",
                            subtitulo = if (categoriaSeleccionada == null) null else "Inventario"
                        )
                    }
                },

                navigationIcon = {

                    if (mostrarBusqueda) {

                        IconButton(onClick = ::cerrarBusqueda) {

                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Cerrar búsqueda"
                            )
                        }

                    } else if (
                        categoriaSeleccionada != null
                    ) {

                        IconButton(

                            onClick = {

                                categoriaSeleccionada = null
                                textoBusqueda = ""
                                mostrarBusqueda = false
                                productoAExpandir = null
                            }

                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.ArrowBack,

                                contentDescription =
                                    "Volver"
                            )
                        }
                    }
                },

                actions = {

                    // Historial (se oculta mientras se busca, para dejar lugar al buscador)
                    if (!mostrarBusqueda && onAbrirHistorial != null) {

                        IconButton(
                            onClick = onAbrirHistorial
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.History,

                                contentDescription =
                                    "Historial"
                            )
                        }
                    }

                    // Lupa: botón redondo con fondo para que se distinga de los otros íconos
                    if (!mostrarBusqueda) {

                        FilledTonalIconButton(

                            onClick = {
                                mostrarBusqueda = true
                                enfocarBusqueda = true
                            },

                            // Verde azulado ("Flow"): resalta sobre la barra de título
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),

                            modifier = Modifier.padding(end = 4.dp)

                        ) {

                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar producto"
                            )
                        }
                    }
                },

                colors = coloresBarraTitulo()
            )
        }

    ) { paddingValues ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            /*
             * ------------------------------------------------
             * PANTALLA PRINCIPAL: CATEGORÍAS
             * ------------------------------------------------
             */

            if (
                categoriaSeleccionada == null
            ) {

                /*
                 * Si estamos buscando mostramos
                 * los resultados globales.
                 */
                if (
                    mostrarBusqueda &&
                    textoBusqueda.isNotBlank()
                ) {

                    if (
                        resultadosBusqueda.isEmpty()
                    ) {

                        Box(
                            modifier =
                                Modifier.fillMaxSize(),

                            contentAlignment =
                                Alignment.TopCenter
                        ) {

                            Text(
                                text =
                                    "No se encontraron productos.",

                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant,

                                modifier =
                                    Modifier.padding(
                                        top = 30.dp
                                    )
                            )
                        }

                    } else {

                        LazyColumn(

                            modifier =
                                Modifier.fillMaxSize(),

                            verticalArrangement =
                                Arrangement.spacedBy(10.dp),

                            contentPadding =
                                PaddingValues(
                                    top = 16.dp,
                                    bottom = 16.dp
                                )
                        ) {

                            items(
                                items =
                                    resultadosBusqueda,

                                key = {
                                    it.nombreProducto
                                }
                            ) { producto ->

                                TarjetaResultadoBusqueda(

                                    producto = producto,

                                    onClick = {

                                        /*
                                         * Guardamos el producto
                                         * que queremos abrir.
                                         */
                                        productoAExpandir =
                                            producto.nombreProducto

                                        /*
                                         * Entramos automáticamente
                                         * a su categoría.
                                         */
                                        categoriaSeleccionada =
                                            producto.categoria

                                        /*
                                         * Cerramos la búsqueda.
                                         */
                                        mostrarBusqueda =
                                            false

                                        textoBusqueda = ""
                                    }
                                )
                            }
                        }
                    }

                } else {

                    /*
                     * MODO NORMAL:
                     * mostramos las categorías.
                     */
                    LazyColumn(

                        state =
                            estadoListaCategorias,

                        modifier =
                            Modifier.fillMaxSize(),

                        verticalArrangement =
                            Arrangement.spacedBy(12.dp),

                        contentPadding =
                            PaddingValues(
                                vertical = 16.dp
                            )
                    ) {

                        itemsIndexed(
                            items = ordenCategorias,
                            key = { _, categoria -> categoria.idCategoria }
                        ) { indice, categoria ->

                            val indiceActual by rememberUpdatedState(indice)
                            val arrastrando = estadoReordenar.indiceArrastrado == indice

                            // Mantener apretada cualquier parte de la tarjeta (menos tiempo que lo normal) y mover
                            val gestoReordenar =
                                if (onReordenarCategorias == null) {
                                    Modifier
                                } else {
                                    Modifier.pointerInput(Unit) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                vibracion.performHapticFeedback(HapticFeedbackType.LongPress)
                                                estadoReordenar.iniciar(indiceActual)
                                            },
                                            onDrag = { cambio, arrastre ->
                                                cambio.consume()
                                                estadoReordenar.arrastrar(arrastre.y)
                                            },
                                            onDragEnd = { terminarArrastre() },
                                            onDragCancel = { terminarArrastre() }
                                        )
                                    }
                                }

                            CompositionLocalProvider(LocalViewConfiguration provides configuracionArrastreRapido) {

                                TarjetaCategoria(

                                    modifier = Modifier
                                        // La arrastrada se dibuja encima de las demás, corrida y un poco más grande
                                        .zIndex(if (arrastrando) 1f else 0f)
                                        .graphicsLayer {
                                            translationY = if (arrastrando) estadoReordenar.desplazamiento else 0f
                                            val escala = if (arrastrando) 1.03f else 1f
                                            scaleX = escala
                                            scaleY = escala
                                        }
                                        // Las demás se corren con animación para hacerle lugar
                                        .then(if (arrastrando) Modifier else Modifier.animateItem())
                                        .then(gestoReordenar),

                                    arrastrando =
                                        arrastrando,

                                    nombreCategoria =
                                        categoria.nombre,

                                    cantidadProductos =
                                        productosPorCategoria[categoria.nombre] ?: 0,

                                    modoEliminar =
                                        modoEliminarCategorias,

                                    onEliminar = {
                                        onEliminarCategoria?.invoke(categoria)
                                    },

                                    onClick = {

                                        categoriaSeleccionada =
                                            categoria.nombre

                                        productoAExpandir =
                                            null
                                    }
                                )
                            }
                        }

                        // Crear y eliminar categorías: solo quien puede editar el inventario
                        if (onCrearCategoria != null && onEliminarCategoria != null) {

                            item(key = "acciones-categorias") {

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {

                                    TarjetaAgregarCategoria(
                                        onClick = {
                                            modoEliminarCategorias = false
                                            mostrarNuevaCategoria = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    BotonEliminarCategoria(
                                        activo = modoEliminarCategorias,
                                        onClick = {
                                            when {
                                                // Segundo toque: termina de eliminar
                                                modoEliminarCategorias -> modoEliminarCategorias = false
                                                categorias.size <= 1 -> avisoUltimaCategoria = true
                                                else -> modoEliminarCategorias = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

            } else {

                /*
                 * ------------------------------------------------
                 * PRODUCTOS DE UNA CATEGORÍA
                 * ------------------------------------------------
                 */

                Box(

                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {

                    OutlinedButton(

                        onClick = {

                            menuOrdenExpandido =
                                !menuOrdenExpandido
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text =
                                "Ordenar: " +
                                        ordenSeleccionado
                        )

                        Icon(

                            imageVector =

                                if (
                                    menuOrdenExpandido
                                ) {
                                    Icons.Default.KeyboardArrowUp
                                } else {
                                    Icons.Default.KeyboardArrowDown
                                },

                            contentDescription =
                                "Cambiar orden"
                        )
                    }

                    DropdownMenu(

                        expanded =
                            menuOrdenExpandido,

                        onDismissRequest = {

                            menuOrdenExpandido =
                                false
                        }
                    ) {

                        DropdownMenuItem(

                            text = {
                                Text(
                                    "Próximo a vencer"
                                )
                            },

                            onClick = {

                                ordenSeleccionado =
                                    "Próximo a vencer"

                                menuOrdenExpandido =
                                    false
                            }
                        )

                        DropdownMenuItem(

                            text = {
                                Text(
                                    "Más lejano a vencer"
                                )
                            },

                            onClick = {

                                ordenSeleccionado =
                                    "Más lejano a vencer"

                                menuOrdenExpandido =
                                    false
                            }
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                /*
                 * LISTA DE PRODUCTOS
                 */

                if (
                    productosAgrupados.isEmpty()
                ) {

                    Box(

                        modifier =
                            Modifier.fillMaxSize(),

                        contentAlignment =
                            Alignment.TopCenter
                    ) {

                        Text(

                            text =
                                if (
                                    textoBusqueda.isNotBlank()
                                ) {
                                    "No se encontraron productos."
                                } else {
                                    "No hay productos registrados."
                                },

                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,

                            modifier =
                                Modifier.padding(
                                    top = 30.dp
                                )
                        )
                    }

                } else {

                    LazyColumn(

                        modifier =
                            Modifier.fillMaxSize(),

                        verticalArrangement =
                            Arrangement.spacedBy(10.dp),

                        contentPadding =
                            PaddingValues(
                                bottom = 16.dp
                            )
                    ) {

                        items(

                            items =
                                productosAgrupados,

                            key = {
                                it.nombreProducto
                            }

                        ) { producto ->

                            TarjetaProductoExpandible(

                                producto = producto,

                                expandidoInicial =
                                    producto.nombreProducto ==
                                            productoAExpandir,

                                onEditarLote =
                                    if (onEditarLote != null) {
                                        { loteEnEdicion = it }
                                    } else {
                                        null
                                    }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarNuevaCategoria && onCrearCategoria != null) {
        DialogoNuevaCategoria(
            onCrear = onCrearCategoria,
            onCerrar = { mostrarNuevaCategoria = false }
        )
    }

    if (avisoUltimaCategoria) {
        AlertDialog(
            onDismissRequest = { avisoUltimaCategoria = false },
            title = { Text("No se puede eliminar") },
            text = { Text("Tiene que quedar al menos una categoría. Creá otra antes de eliminar esta.") },
            confirmButton = {
                TextButton(onClick = { avisoUltimaCategoria = false }) { Text("Entendido") }
            }
        )
    }

    loteEnEdicion?.let { lote ->

        DialogoEditarLote(

            lote = lote,

            categorias = categorias.map { it.nombre },

            onGuardar = { cambios ->

                onEditarLote?.invoke(cambios)

                loteEnEdicion = null
            },

            onCancelar = {
                loteEnEdicion = null
            }
        )
    }
}


/*
 * ------------------------------------------------
 * RESULTADO DE BÚSQUEDA
 * ------------------------------------------------
 */

@Composable
fun TarjetaResultadoBusqueda(

    producto: Producto,

    onClick: () -> Unit

) {

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },

        shape =
            RoundedCornerShape(12.dp),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            )
    ) {

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(

                imageVector =
                    Icons.Default.List,

                contentDescription =
                    "Producto",

                tint =
                    MaterialTheme
                        .colorScheme
                        .primary,

                modifier =
                    Modifier.size(30.dp)
            )

            Spacer(
                modifier =
                    Modifier.width(14.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    text =
                        producto.nombreProducto,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize =
                        17.sp
                )

                Text(

                    text =
                        producto.categoria,

                    fontSize =
                        13.sp,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(

                imageVector =
                    Icons.Default.KeyboardArrowDown,

                contentDescription =
                    "Abrir"
            )
        }
    }
}


/*
 * ------------------------------------------------
 * CONVERSIÓN DE FECHA
 * ------------------------------------------------
 */

fun convertirFecha(
    fecha: String
): Long {

    return try {

        val formato =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

        formato.isLenient = false

        formato.parse(
            fecha
        )?.time ?: Long.MAX_VALUE

    } catch (
        e: Exception
    ) {

        Long.MAX_VALUE
    }
}


/*
 * ------------------------------------------------
 * TARJETA DE CATEGORÍA
 * ------------------------------------------------
 */

@Composable
fun TarjetaCategoria(

    nombreCategoria: String,

    cantidadProductos: Int,

    modifier: Modifier = Modifier,

    // true mientras el usuario la está moviendo (se dibuja más elevada)
    arrastrando: Boolean = false,

    // En modo eliminar la tarjeta se pinta de rojo, no se abre al tocarla y muestra un tacho
    modoEliminar: Boolean = false,

    onEliminar: () -> Unit = {},

    onClick: () -> Unit

) {

    Card(

        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(enabled = !modoEliminar) {
                onClick()
            },

        shape =
            RoundedCornerShape(12.dp),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = if (arrastrando) 12.dp else 4.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (modoEliminar)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
            )
    ) {

        Row(

            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 20.dp,
                    end = if (modoEliminar) 8.dp else 20.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(

                imageVector =
                    Icons.Default.List,

                contentDescription =
                    "Categoría",

                tint =
                    if (modoEliminar)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,

                modifier =
                    Modifier.size(32.dp)
            )

            Spacer(
                modifier =
                    Modifier.width(16.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(

                    text =
                        nombreCategoria,

                    fontSize =
                        20.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        if (modoEliminar)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(

                    text = when (cantidadProductos) {
                        0 -> "Sin productos"
                        1 -> "1 producto"
                        else -> "$cantidadProductos productos"
                    },

                    style =
                        MaterialTheme.typography.bodySmall,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (modoEliminar) {

                IconButton(
                    onClick = onEliminar
                ) {

                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar categoría $nombreCategoria",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}


/*
 * ------------------------------------------------
 * CREAR / ELIMINAR CATEGORÍAS
 * ------------------------------------------------
 */

// Cuadro con borde punteado y una cruz: indica que se pueden crear categorías
@Composable
fun TarjetaAgregarCategoria(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Verde azulado de la marca: crear es la acción "positiva" de la pantalla
    val colorBorde = MaterialTheme.colorScheme.secondary

    Row(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color = colorBorde,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx()))
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = colorBorde,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Agregar categoría",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorBorde
        )
    }
}

// Activa el modo eliminar; tocándolo de nuevo ("Listo") se sale
@Composable
fun BotonEliminarCategoria(activo: Boolean, onClick: () -> Unit) {
    val colores = if (activo) {
        CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.error)
    } else {
        CardDefaults.outlinedCardColors()
    }
    val colorContenido = if (activo) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.error

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = colores
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (activo) Icons.Default.Check else Icons.Default.Delete,
                contentDescription = null,
                tint = colorContenido
            )
            Text(
                text = if (activo) "Listo" else "Eliminar",
                style = MaterialTheme.typography.labelMedium,
                color = colorContenido
            )
        }
    }
}

@Composable
fun DialogoNuevaCategoria(
    onCrear: suspend (String) -> String?,
    onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun crear() {
        guardando = true
        scope.launch {
            try {
                val resultado = onCrear(nombre)
                if (resultado == null) onCerrar() else error = resultado
            } finally {
                guardando = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nueva categoría") },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it; error = null },
                label = { Text("Nombre, por ejemplo Lácteos") },
                isError = error != null,
                supportingText = error?.let { texto -> { Text(texto) } },
                singleLine = true,
                enabled = !guardando,
                modifier = Modifier.visibleAlEscribir()
            )
        },
        confirmButton = {
            TextButton(onClick = ::crear, enabled = nombre.isNotBlank() && !guardando) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !guardando) { Text("Cancelar") }
        }
    )
}


/*
 * ------------------------------------------------
 * TARJETA DE PRODUCTO
 * ------------------------------------------------
 */

@Composable
fun TarjetaProductoExpandible(

    producto: Producto,

    expandidoInicial: Boolean = false,

    // null = sin permiso de edición
    onEditarLote: ((LoteItem) -> Unit)? = null

) {

    var expandido by remember(
        producto.nombreProducto
    ) {
        mutableStateOf(
            expandidoInicial
        )
    }

    LaunchedEffect(
        expandidoInicial
    ) {

        if (expandidoInicial) {
            expandido = true
        }
    }

    Card(

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 3.dp
            ),

        shape =
            RoundedCornerShape(12.dp),

        modifier = Modifier
            .fillMaxWidth()
            .clickable {

                expandido =
                    !expandido
            }
    ) {

        Column(

            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(

                        text =
                            producto.nombreProducto,

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            17.sp
                    )

                    Text(

                        text =
                            "EAN: " +
                                    producto.codigoBarra +
                                    " • " +
                                    producto.lotes.size +
                                    " lote(s)",

                        fontSize =
                            12.sp,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(

                    color =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer,

                    shape =
                        RoundedCornerShape(8.dp)
                ) {

                    Text(

                        text =
                            "${producto.totalCantidad} u. total",

                        modifier =
                            Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            ),

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            13.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Icon(

                    imageVector =

                        if (
                            expandido
                        ) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },

                    contentDescription =
                        "Expandir"
                )
            }

            AnimatedVisibility(

                visible =
                    expandido
            ) {

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 12.dp
                            ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    HorizontalDivider(

                        modifier =
                            Modifier.padding(
                                vertical = 4.dp
                            )
                    )

                    Text(

                        text =
                            "Detalle de Lotes:",

                        fontSize =
                            12.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )

                    producto.lotes.forEach { lote ->

                        FilaDetalleLote(
                            lote = lote,
                            onEditar = onEditarLote?.let { editar ->
                                { editar(lote) }
                            }
                        )
                    }
                }
            }
        }
    }
}


/*
 * ------------------------------------------------
 * DETALLE DE LOTE
 * ------------------------------------------------
 */

@Composable
fun FilaDetalleLote(

    lote: LoteItem,

    // null = sin permiso: no se muestra el lápiz
    onEditar: (() -> Unit)? = null

) {

    val colorEstado = when (
        lote.estado
    ) {

        EstadoSemaforo.CRITICO ->
            Semaforo.critico

        EstadoSemaforo.ADVERTENCIA ->
            Semaforo.advertencia

        EstadoSemaforo.OPTIMO ->
            Semaforo.optimo
    }

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                shape =
                    RoundedCornerShape(8.dp)
            )
            .padding(10.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(

            modifier = Modifier
                .size(12.dp)
                .background(
                    color =
                        colorEstado,

                    shape =
                        RoundedCornerShape(
                            6.dp
                        )
                )
        )

        Spacer(
            modifier =
                Modifier.width(10.dp)
        )

        Column(

            modifier =
                Modifier.weight(1f)
        ) {

            Text(

                text =
                    "Lote: ${lote.idLote}",

                fontWeight =
                    FontWeight.Medium,

                fontSize =
                    13.sp
            )

            Text(

                text =
                    "Vence: " +
                            lote.fechaVencimiento,

                fontSize =
                    12.sp,

                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(

            horizontalAlignment =
                Alignment.End
        ) {

            Text(

                text =
                    "${lote.cantidadActual} u.",

                fontWeight =
                    FontWeight.Bold,

                fontSize =
                    14.sp
            )

            Text(

                text =
                    "${lote.diasParaVencer} días",

                fontSize =
                    11.sp,

                color =
                    colorEstado
            )
        }

        if (onEditar != null) {

            IconButton(
                onClick = onEditar
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Edit,

                    contentDescription =
                        "Editar lote ${lote.idLote}",

                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }
        }
    }
}


/*
 * ------------------------------------------------
 * EDICIÓN DE LOTE
 * ------------------------------------------------
 */

@Composable
fun DialogoEditarLote(

    lote: LoteItem,

    // Nombres de las categorías del local
    categorias: List<String>,

    onGuardar: (CambiosLote) -> Unit,

    onCancelar: () -> Unit

) {

    var nombre by remember { mutableStateOf(lote.nombreProducto) }
    var categoria by remember { mutableStateOf(lote.categoria) }
    var codigoBarra by remember { mutableStateOf(lote.codigoBarra) }
    var cantidad by remember { mutableStateOf(lote.cantidadActual.toString()) }
    var precio by remember { mutableStateOf(lote.precio?.toString().orEmpty()) }
    var fecha by remember { mutableStateOf(lote.fechaVencimiento) }

    val cantidadValida = cantidad.toIntOrNull()?.takeIf { it > 0 }
    // Vacío = sin precio; acepta coma o punto como separador decimal
    val precioIngresado = precio.trim().replace(',', '.')
    val precioValido = precioIngresado.isEmpty() ||
        precioIngresado.toDoubleOrNull()?.let { it >= 0 } == true
    val fechaValida = esFechaValida(fecha)

    val todoValido = nombre.isNotBlank() && cantidadValida != null && precioValido && fechaValida

    AlertDialog(

        onDismissRequest = onCancelar,

        title = {
            Text("Editar lote ${lote.idLote}")
        },

        text = {

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del producto") },
                    isError = nombre.isBlank(),
                    singleLine = true,
                    modifier = Modifier.visibleAlEscribir()
                )

                OutlinedTextField(
                    value = codigoBarra,
                    onValueChange = { codigoBarra = it.trim() },
                    label = { Text("Código de Barras (EAN/QR)") },
                    supportingText = {
                        if (codigoBarra.isBlank()) Text("Vacío = producto sin código")
                    },
                    singleLine = true,
                    modifier = Modifier.visibleAlEscribir()
                )

                CampoCantidad(
                    valor = cantidad,
                    onValorChange = { cantidad = it },
                    esError = cantidadValida == null
                )

                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio de venta ($)") },
                    isError = !precioValido,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.visibleAlEscribir()
                )

                CampoFecha(
                    fecha = fecha,
                    onFechaChange = { fecha = it },
                    etiqueta = "Vencimiento (DD/MM/AAAA)",
                    esError = !fechaValida
                )

                SelectorCategoria(
                    categorias = categorias,
                    seleccionada = categoria,
                    onSeleccionar = { categoria = it }
                )

                Text(
                    text = "El nombre, el código, el precio y la categoría se aplican a todos los lotes de este producto.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },

        confirmButton = {

            TextButton(
                enabled = todoValido,
                onClick = {
                    onGuardar(
                        CambiosLote(
                            idLote = lote.idLote.toInt(),
                            nombre = nombre.trim(),
                            codigoBarra = codigoBarra.trim(),
                            cantidad = cantidadValida ?: return@TextButton,
                            precio = precioIngresado.toDoubleOrNull(),
                            fechaVencimiento = fecha,
                            categoria = categoria
                        )
                    )
                }
            ) {
                Text("Guardar")
            }
        },

        dismissButton = {

            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}

// Tiempo que hay que mantener apretada una categoría para empezar a moverla (el del sistema ronda 400-500 ms)
private const val ESPERA_ARRASTRE_MS = 200L
