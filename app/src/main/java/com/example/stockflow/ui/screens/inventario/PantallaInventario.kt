package com.example.stockflow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAlertas(lotes: List<LoteItem>) {

    var categoriaSeleccionada by remember {
        mutableStateOf<String?>(null)
    }

    var textoBusqueda by remember {
        mutableStateOf("")
    }

    var mostrarBusqueda by remember {
        mutableStateOf(false)
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

                        OutlinedTextField(
                            value = textoBusqueda,

                            onValueChange = {
                                textoBusqueda = it
                            },

                            modifier = Modifier
                                .fillMaxWidth(),

                            placeholder = {
                                Text(
                                    "Buscar producto..."
                                )
                            },

                            singleLine = true
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

                    if (
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

                    /*
                     * LA LUPA AHORA APARECE TAMBIÉN
                     * EN LA PANTALLA PRINCIPAL.
                     */
                    IconButton(

                        onClick = {

                            mostrarBusqueda =
                                !mostrarBusqueda

                            if (!mostrarBusqueda) {

                                textoBusqueda = ""
                            }
                        }

                    ) {

                        Icon(
                            imageVector =
                                if (mostrarBusqueda)
                                    Icons.Default.Close
                                else
                                    Icons.Default.Search,

                            contentDescription =
                                if (mostrarBusqueda)
                                    "Cerrar búsqueda"
                                else
                                    "Buscar producto"
                        )
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
                                            productoAExpandir
                            )
                        }
                    }
                }
            }
        }
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

    expandidoInicial: Boolean = false

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
                            lote = lote
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

    lote: LoteItem

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
    }
}