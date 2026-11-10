@file:kotlin.OptIn(ExperimentalMaterial3Api::class)

package com.example.stockflow

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.fillMaxSize
import com.example.stockflow.ui.screens.cuenta.PantallaCuenta
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.saveable.Saver
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.stockflow.data.local.StockFlowDatabase
import com.example.stockflow.data.repository.CategoriaRepository
import com.example.stockflow.data.repository.DispositivoRepository
import com.example.stockflow.data.repository.InventarioRepository
import com.example.stockflow.data.repository.SesionRepository
import com.example.stockflow.data.repository.UsuarioRepository
import com.example.stockflow.data.repository.HistorialRepository
import com.example.stockflow.ui.screens.historial.PantallaHistorial
import androidx.lifecycle.lifecycleScope
import com.example.stockflow.ui.screens.escaner.PantallaEscaner
import kotlinx.coroutines.launch
import androidx.camera.core.ExperimentalGetImage
import androidx.annotation.OptIn
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.stockflow.ui.componentes.CentrarCamposAlEscribir
import com.example.stockflow.ui.theme.StockFlowTheme
import com.example.stockflow.ui.screens.login.PantallaLogin
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.luminance
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.stockflow.notificaciones.AvisosVencimiento

/**
 * En Android 13+ las notificaciones necesitan permiso: se pide una vez, al entrar con sesión.
 * Si lo concede, se revisan los vencimientos en el momento para no esperar a la revisión diaria.
 */
@Composable
private fun PedirPermisoNotificaciones() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) AvisosVencimiento.revisarAhora(context.applicationContext)
    }
    LaunchedEffect(Unit) {
        val concedido = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!concedido) lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = StockFlowDatabase.getDatabase(this)
        val usuarios = UsuarioRepository(database)
        val dispositivos = DispositivoRepository(database.dispositivoDao())
        val sesionGuardada = SesionRepository(this, usuarios)

        lifecycleScope.launch { dispositivos.registrarEsteDispositivo(applicationContext) }
        // Revisión diaria de vencimientos (y una ahora, por si hay avisos pendientes)
        AvisosVencimiento.programar(applicationContext)

        setContent {
            StockFlowTheme {
                // Al tocar un campo de texto la pantalla se desplaza para dejarlo centrado
                CentrarCamposAlEscribir {
                    // Usuario que inició sesión (null = mostrar login). Se conserva al girar el teléfono
                    var sesion by rememberSaveable(stateSaver = GuardadoSesion) { mutableStateOf<SesionUsuario?>(null) }

                    // La hora y la batería se dibujan sobre la barra de título o, en el login,
                    // sobre el fondo de la pantalla: se eligen íconos claros u oscuros según ese color
                    val colorDetrasDeBarraEstado =
                        if (sesion != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background
                    val vista = LocalView.current
                    SideEffect {
                        WindowCompat.getInsetsController(window, vista).isAppearanceLightStatusBars =
                            colorDetrasDeBarraEstado.luminance() > 0.5f
                    }
                    // true mientras se busca la sesión guardada (pantalla vacía por un instante)
                    var restaurando by remember { mutableStateOf(true) }

                    // Inicia o cierra sesión y lo recuerda para la próxima vez que se abra la app
                    fun cambiarSesion(nueva: SesionUsuario?) {
                        if (nueva != null) sesionGuardada.guardar(nueva) else sesionGuardada.borrar()
                        // Al entrar se revisan los vencimientos del local de este usuario
                        if (nueva != null) AvisosVencimiento.revisarAhora(applicationContext)
                        sesion = nueva
                    }

                    LaunchedEffect(sesion) {
                        if (sesion != null) return@LaunchedEffect
                        // Al abrir la app: si había una sesión guardada se entra directo, sin pasar por el login
                        val restaurada = sesionGuardada.restaurar()
                        if (restaurada != null) sesion = restaurada
                        restaurando = false
                    }

                    val sesionActual = sesion
                    when {
                        sesionActual != null -> {
                            PedirPermisoNotificaciones()
                            // Inventario e historial trabajan solo con el local de la sesión.
                            // El inventario además sabe quién es el usuario, para anotarlo en el historial
                            val inventario = remember(sesionActual.idLocal) {
                                InventarioRepository(database, sesionActual)
                            }
                            val historial = remember(sesionActual.idLocal) {
                                HistorialRepository(database.eventoDao(), sesionActual.idLocal)
                            }
                            val categorias = remember(sesionActual.idLocal) {
                                CategoriaRepository(database, sesionActual.idLocal)
                            }
                            AplicacionInteractivaStockFlow(
                                sesion = sesionActual,
                                inventario = inventario,
                                historial = historial,
                                categorias = categorias,
                                usuarios = usuarios,
                                onCerrarSesion = { cambiarSesion(null) }
                            )
                        }

                        restaurando -> Surface(modifier = Modifier.fillMaxSize()) {}

                        else -> PantallaLogin(
                            onIniciarSesion = { email, contrasena ->
                                val encontrada = usuarios.validar(email, contrasena)
                                if (encontrada != null) cambiarSesion(encontrada)
                                encontrada != null
                            },
                            onCrearCuenta = { nombreLocal, nombre, email, contrasena ->
                                when (usuarios.registrarDueno(nombreLocal, nombre, email, contrasena)) {
                                    UsuarioRepository.ResultadoRegistro.CREADO -> {
                                        // El dueño del local nuevo entra directamente
                                        cambiarSesion(usuarios.validar(email, contrasena))
                                        null
                                    }
                                    UsuarioRepository.ResultadoRegistro.EMAIL_EN_USO -> "Ya hay una cuenta con ese correo"
                                    UsuarioRepository.ResultadoRegistro.NO_PERMITIDO -> "No se pudo crear la cuenta"
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// Guarda la sesión como lista simple para que sobreviva a la rotación de pantalla
private val GuardadoSesion = Saver<SesionUsuario?, List<Any>>(
    save = { sesion -> sesion?.let { listOf(it.idUsuario, it.nombre, it.rol.clave, it.idLocal, it.nombreLocal) } },
    restore = {
        SesionUsuario(
            idUsuario = it[0] as Int,
            nombre = it[1] as String,
            rol = Rol.desdeClave(it[2] as String),
            idLocal = it[3] as Int,
            nombreLocal = it[4] as String
        )
    }
)

// Pestañas de la barra inferior
private enum class Seccion(val titulo: String, val icono: ImageVector) {
    INVENTARIO("Inventario", Icons.Default.Inventory2),
    ESCANER("Escáner", Icons.Default.Search),
    INGRESAR("Ingresar Stock", Icons.Default.Add),
    CUENTA("Cuenta", Icons.Default.Person)
}


@OptIn(ExperimentalGetImage::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun AplicacionInteractivaStockFlow(
    sesion: SesionUsuario,
    inventario: InventarioRepository,
    historial: HistorialRepository,
    categorias: CategoriaRepository,
    usuarios: UsuarioRepository,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listaLotes by remember(inventario) { inventario.observarLotes() }.collectAsState(initial = emptyList())

    val rol = sesion.rol

    // Categorías del local: la primera vez se crea "Sin categoría"
    LaunchedEffect(categorias) { categorias.asegurarCategoriaPorDefecto() }
    val listaCategorias by remember(categorias) { categorias.observar() }.collectAsState(initial = emptyList())

    // Las pestañas dependen de los permisos: el empleado no ve "Ingresar Stock"
    val secciones = remember(rol) {
        Seccion.entries.filter { it != Seccion.INGRESAR || rol.puedeIngresarStock }
    }
    var pestañaSeleccionada by rememberSaveable { mutableStateOf(Seccion.INVENTARIO) }

    val listaUsuarios by remember(usuarios, sesion.idLocal) {
        usuarios.observarUsuarios(sesion.idLocal)
    }.collectAsState(initial = emptyList())

    // Historial (ventas, ingresos y cambios) abierto dentro de la pestaña Inventario
    var mostrarHistorial by remember { mutableStateOf(false) }
    val eventosHistorial by remember(historial) { historial.observar() }.collectAsState(initial = emptyList())

    // Texto que el escáner manda a buscar en la pantalla de Inventario (opción "Buscar")
    var busquedaInventario by remember { mutableStateOf("") }

    // Agrupa los lotes con ese código en un Producto con su stock total, o null si no existe
    fun buscarProductoPorCodigo(codigo: String): Producto? {
        val lotes = listaLotes.filter { it.codigoBarra == codigo }
        val primero = lotes.firstOrNull() ?: return null
        return Producto(
            nombreProducto = primero.nombreProducto,
            categoria = primero.categoria,
            codigoBarra = codigo,
            totalCantidad = lotes.sumOf { it.cantidadActual },
            lotes = lotes,
            precio = primero.precio
        )
    }

    fun cambiarPestaña(pestaña: Seccion) {
        busquedaInventario = ""
        mostrarHistorial = false
        pestañaSeleccionada = pestaña
    }

    Scaffold(
        // Arriba no se reserva nada: la barra de título de cada pantalla ya se acomoda bajo la barra de estado
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Con sus insets por defecto la barra queda justo encima de los botones del sistema
            // (atrás, inicio, recientes) en vez de quedar tapada por ellos
            NavigationBar {
                secciones.forEach { seccion ->
                    NavigationBarItem(
                        selected = pestañaSeleccionada == seccion,
                        onClick = { cambiarPestaña(seccion) },
                        icon = { Icon(seccion.icono, contentDescription = seccion.titulo) },
                        label = { Text(seccion.titulo) }
                    )
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .padding(padding)
                // Las barras del sistema ya se reservaron acá: así las pantallas internas
                // no vuelven a dejar ese espacio vacío (la "barra invisible")
                .consumeWindowInsets(padding)
        ) {
            when (pestañaSeleccionada) {
                Seccion.INVENTARIO -> if (mostrarHistorial) {
                    PantallaHistorial(
                        eventos = eventosHistorial,
                        onVolver = { mostrarHistorial = false }
                    )
                } else PantallaAlertas(
                    onAbrirHistorial = { mostrarHistorial = true },
                    categorias = listaCategorias,
                    // Crear y eliminar categorías: los mismos roles que pueden editar el inventario
                    onCrearCategoria = if (rol.puedeEditarInventario) {
                        { nombre -> categorias.crear(nombre) }
                    } else {
                        null
                    },
                    onReordenarCategorias = if (rol.puedeEditarInventario) {
                        { ids -> scope.launch { categorias.reordenar(ids) } }
                    } else {
                        null
                    },
                    onEliminarCategoria = if (rol.puedeEditarInventario) {
                        { categoria ->
                            scope.launch {
                                val mensaje = when (val resultado = categorias.eliminar(categoria.idCategoria)) {
                                    CategoriaRepository.ResultadoEliminar.Eliminada ->
                                        "Categoría ${categoria.nombre} eliminada"
                                    is CategoriaRepository.ResultadoEliminar.TieneProductos ->
                                        "No se puede eliminar \"${categoria.nombre}\": tiene ${resultado.cantidad} " +
                                            (if (resultado.cantidad == 1) "producto" else "productos") +
                                            " dentro. Movelos o eliminalos primero"
                                    CategoriaRepository.ResultadoEliminar.EsLaUltima ->
                                        "Tiene que quedar al menos una categoría"
                                }
                                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        null
                    },
                    lotes = listaLotes,
                    busquedaInicial = busquedaInventario,
                    onEditarLote = if (!rol.puedeEditarInventario) null else { cambios ->
                        scope.launch {
                            try {
                                inventario.editarLote(cambios)
                                Toast.makeText(context, "Lote actualizado", Toast.LENGTH_SHORT).show()
                            } catch (e: IllegalArgumentException) {
                                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )

                Seccion.ESCANER -> PantallaEscaner(
                    buscarProducto = ::buscarProductoPorCodigo,
                    onBuscarEnInventario = { producto ->
                        // Abre Inventario con la búsqueda del producto ya cargada
                        busquedaInventario = producto.nombreProducto
                        pestañaSeleccionada = Seccion.INVENTARIO
                    },
                    onDescontar = { items ->
                        scope.launch {
                            try {
                                val idVenta = inventario.descontarStock(items)
                                Toast.makeText(
                                    context,
                                    "Venta #$idVenta registrada: ${items.sumOf { it.cantidad }} unidades",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: IllegalStateException) {
                                Toast.makeText(context, "No se registró la venta: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )

                // Si el rol no puede ingresar stock la pestaña no existe; por las dudas se muestra el inventario
                Seccion.INGRESAR -> if (!rol.puedeIngresarStock) {
                    LaunchedEffect(Unit) { cambiarPestaña(Seccion.INVENTARIO) }
                } else PantallaAgregar(
                    categorias = listaCategorias.map { it.nombre },
                    buscarProductoPorCodigo = { codigo ->
                        listaLotes.firstOrNull { it.codigoBarra == codigo }
                    },
                    onAgregarLote = { nuevoLote ->
                        scope.launch {
                            try {
                                inventario.registrarLote(nuevoLote)
                            } catch (e: IllegalArgumentException) {
                                Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )

                Seccion.CUENTA -> PantallaCuenta(
                    sesion = sesion,
                    usuarios = listaUsuarios,
                    onCrearUsuario = { nombre, email, contrasena, rolNuevo ->
                        when (usuarios.crearUsuario(sesion, nombre, email, contrasena, rolNuevo)) {
                            UsuarioRepository.ResultadoRegistro.CREADO -> {
                                Toast.makeText(context, "Usuario $nombre creado", Toast.LENGTH_SHORT).show()
                                null
                            }
                            UsuarioRepository.ResultadoRegistro.EMAIL_EN_USO -> "Ya hay un usuario con ese correo"
                            UsuarioRepository.ResultadoRegistro.NO_PERMITIDO -> "Solo el dueño puede crear usuarios"
                        }
                    },
                    onEliminarUsuario = { usuario ->
                        scope.launch {
                            if (usuarios.eliminarUsuario(sesion, usuario.idUsuario)) {
                                Toast.makeText(context, "Usuario ${usuario.nombre} eliminado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onCerrarSesion = onCerrarSesion
                )
            }
        }
    }
}
