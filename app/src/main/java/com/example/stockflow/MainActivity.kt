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
import com.example.stockflow.data.repository.DispositivoRepository
import com.example.stockflow.data.repository.InventarioRepository
import com.example.stockflow.data.repository.SesionRepository
import com.example.stockflow.data.repository.UsuarioRepository
import com.example.stockflow.data.repository.VentaRepository
import com.example.stockflow.ui.screens.ventas.PantallaHistorialVentas
import androidx.lifecycle.lifecycleScope
import com.example.stockflow.ui.screens.escaner.PantallaEscaner
import kotlinx.coroutines.launch
import androidx.camera.core.ExperimentalGetImage
import androidx.annotation.OptIn
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.stockflow.ui.componentes.CentrarCamposAlEscribir
import com.example.stockflow.ui.screens.login.PantallaLogin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = StockFlowDatabase.getDatabase(this)
        val inventario = InventarioRepository(database)
        val ventas = VentaRepository(database.ventaDao())
        val usuarios = UsuarioRepository(database.usuarioDao())
        val dispositivos = DispositivoRepository(database.dispositivoDao())
        val sesionGuardada = SesionRepository(this, usuarios)

        lifecycleScope.launch { dispositivos.registrarEsteDispositivo(applicationContext) }

        setContent {
            MaterialTheme {
                // Al tocar un campo de texto la pantalla se desplaza para dejarlo centrado
                CentrarCamposAlEscribir {
                    // Usuario que inició sesión (null = mostrar login). Se conserva al girar el teléfono
                    var sesion by rememberSaveable(stateSaver = GuardadoSesion) { mutableStateOf<SesionUsuario?>(null) }
                    // null mientras se consulta; false = el teléfono no tiene cuentas y se puede crear la del dueño
                    var hayUsuarios by remember { mutableStateOf<Boolean?>(null) }

                    // Inicia o cierra sesión y lo recuerda para la próxima vez que se abra la app
                    fun cambiarSesion(nueva: SesionUsuario?) {
                        if (nueva != null) sesionGuardada.guardar(nueva) else sesionGuardada.borrar()
                        sesion = nueva
                    }

                    LaunchedEffect(sesion) {
                        if (sesion != null) return@LaunchedEffect
                        // Al abrir la app: si había una sesión guardada se entra directo, sin pasar por el login
                        val restaurada = sesionGuardada.restaurar()
                        if (restaurada != null) sesion = restaurada else hayUsuarios = usuarios.hayUsuarios()
                    }

                    val sesionActual = sesion
                    when {
                        sesionActual != null -> AplicacionInteractivaStockFlow(
                            sesion = sesionActual,
                            inventario = inventario,
                            ventas = ventas,
                            usuarios = usuarios,
                            onCerrarSesion = { cambiarSesion(null) }
                        )

                        // Consultando la base: pantalla vacía por un instante
                        hayUsuarios == null -> Surface(modifier = Modifier.fillMaxSize()) {}

                        else -> PantallaLogin(
                            permitirCrearCuenta = hayUsuarios == false,
                            onIniciarSesion = { usuarioOEmail, contrasena ->
                                val encontrada = usuarios.validar(usuarioOEmail, contrasena)
                                if (encontrada != null) cambiarSesion(encontrada)
                                encontrada != null
                            },
                            onCrearCuenta = { nombre, email, contrasena ->
                                when (usuarios.registrarDueno(nombre, email, contrasena)) {
                                    UsuarioRepository.ResultadoRegistro.CREADO -> {
                                        // La cuenta del dueño entra directamente
                                        cambiarSesion(usuarios.validar(nombre, contrasena))
                                        null
                                    }
                                    UsuarioRepository.ResultadoRegistro.NOMBRE_EN_USO -> "Ese nombre de usuario ya existe"
                                    UsuarioRepository.ResultadoRegistro.EMAIL_EN_USO -> "Ya hay una cuenta con ese email"
                                    UsuarioRepository.ResultadoRegistro.NO_PERMITIDO -> {
                                        hayUsuarios = true
                                        "Ya existe la cuenta del dueño. Pedile un usuario."
                                    }
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
    save = { sesion -> sesion?.let { listOf(it.idUsuario, it.nombre, it.rol.clave) } },
    restore = { SesionUsuario(it[0] as Int, it[1] as String, Rol.desdeClave(it[2] as String)) }
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
    ventas: VentaRepository,
    usuarios: UsuarioRepository,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listaLotes by remember(inventario) { inventario.observarLotes() }.collectAsState(initial = emptyList())

    val rol = sesion.rol

    // Las pestañas dependen de los permisos: el empleado no ve "Ingresar Stock"
    val secciones = remember(rol) {
        Seccion.entries.filter { it != Seccion.INGRESAR || rol.puedeIngresarStock }
    }
    var pestañaSeleccionada by rememberSaveable { mutableStateOf(Seccion.INVENTARIO) }

    val listaUsuarios by remember(usuarios) { usuarios.observarUsuarios() }.collectAsState(initial = emptyList())

    // Historial de ventas abierto dentro de la pestaña Inventario
    var mostrarHistorialVentas by remember { mutableStateOf(false) }
    val historialVentas by remember(ventas) { ventas.observarHistorial() }.collectAsState(initial = emptyList())

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
        mostrarHistorialVentas = false
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
                Seccion.INVENTARIO -> if (mostrarHistorialVentas) {
                    PantallaHistorialVentas(
                        ventas = historialVentas,
                        onVolver = { mostrarHistorialVentas = false }
                    )
                } else PantallaAlertas(
                    onAbrirHistorial = { mostrarHistorialVentas = true },
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
                                val idVenta = inventario.descontarStock(items, sesion)
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
                    onCrearUsuario = { nombre, contrasena, rolNuevo ->
                        when (usuarios.crearUsuario(sesion, nombre, contrasena, rolNuevo)) {
                            UsuarioRepository.ResultadoRegistro.CREADO -> {
                                Toast.makeText(context, "Usuario $nombre creado", Toast.LENGTH_SHORT).show()
                                null
                            }
                            UsuarioRepository.ResultadoRegistro.NOMBRE_EN_USO -> "Ese nombre de usuario ya existe"
                            UsuarioRepository.ResultadoRegistro.EMAIL_EN_USO -> "Ya hay una cuenta con ese email"
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
