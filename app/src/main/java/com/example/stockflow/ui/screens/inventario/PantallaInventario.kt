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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import com.example.stockflow.ui.componentes.esFechaValida
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.stockflow.ui.componentes.visibleAlEscribir

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAlertas(
    lotes: List<LoteItem>,
    busquedaInicial: String = "", // Texto a buscar al llegar desde el escáner
    // Guarda los cambios hechos a un lote con el lápiz; null = sin permiso (no se muestra el lápiz)
    onEditarLote: ((CambiosLote) -> Unit)? = null,
    // Abre el historial de ventas (botón de recibo); null = sin permiso (no se muestra el botón)
    onAbrirHistorial: (() -> Unit)? = null
) {

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

                        Text(
                            text =
                                if (
                                    categoriaSeleccionada == null
                                ) {
                                    "Stock Flow - Inventario"
                                } else {
                                    categoriaSeleccionada!!
                                },

                            fontWeight =
                                FontWeight.Bold
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

                    // Historial de ventas (se oculta mientras se busca, para dejar lugar al buscador)
                    if (!mostrarBusqueda && onAbrirHistorial != null) {

                        IconButton(
                            onClick = onAbrirHistorial
                        ) {

                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.ReceiptLong,

                                contentDescription =
                                    "Historial de ventas"
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

                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
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

                colors =
                    TopAppBarDefaults.topAppBarColors(

                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                    )
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
                                    Color.Gray,

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

                        modifier =
                            Modifier.fillMaxSize(),

                        verticalArrangement =
                            Arrangement.spacedBy(12.dp),

                        contentPadding =
                            PaddingValues(
                                vertical = 16.dp
                            )
                    ) {

                        items(
                            categoriasDisponibles
                        ) { categoria ->

                            TarjetaCategoria(

                                nombreCategoria =
                                    categoria,

                                onClick = {

                                    categoriaSeleccionada =
                                        categoria

                                    productoAExpandir =
                                        null
                                }
                            )
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
                                Color.Gray,

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

    loteEnEdicion?.let { lote ->

        DialogoEditarLote(

            lote = lote,

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
                        Color.Gray
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

    onClick: () -> Unit

) {

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable {
                onClick()
            },

        shape =
            RoundedCornerShape(12.dp),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 4.dp
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
                .fillMaxSize()
                .padding(
                    horizontal = 20.dp
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
                    MaterialTheme
                        .colorScheme
                        .primary,

                modifier =
                    Modifier.size(32.dp)
            )

            Spacer(
                modifier =
                    Modifier.width(16.dp)
            )

            Text(

                text =
                    nombreCategoria,

                fontSize =
                    20.sp,

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
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
                            Color.Gray
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
            Color(0xFFE53935)

        EstadoSemaforo.ADVERTENCIA ->
            Color(0xFFFB8C00)

        EstadoSemaforo.OPTIMO ->
            Color(0xFF43A047)
    }

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFF5F5F5),
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
                    Color.DarkGray
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

    onGuardar: (CambiosLote) -> Unit,

    onCancelar: () -> Unit

) {

    var nombre by remember { mutableStateOf(lote.nombreProducto) }
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

                Text(
                    text = "El nombre, el código y el precio se aplican a todos los lotes de este producto.",
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
                            fechaVencimiento = fecha
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
