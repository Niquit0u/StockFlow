package com.example.stockflow.ui.screens.cuenta

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.stockflow.Rol
import com.example.stockflow.SesionUsuario
import com.example.stockflow.UsuarioItem
import kotlinx.coroutines.launch
import com.example.stockflow.ui.componentes.visibleAlEscribir

/**
 * Pestaña "Cuenta": quién inició sesión y el botón para cerrarla.
 * El dueño además ve la lista de usuarios y puede crear o eliminar empleados y administradores.
 *
 * @param onCrearUsuario devuelve null si se creó, o el mensaje de error para mostrar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCuenta(
    sesion: SesionUsuario,
    usuarios: List<UsuarioItem>,
    onCrearUsuario: suspend (nombre: String, contrasena: String, rol: Rol) -> String?,
    onEliminarUsuario: (UsuarioItem) -> Unit,
    onCerrarSesion: () -> Unit
) {
    var mostrarNuevoUsuario by remember { mutableStateOf(false) }
    var usuarioAEliminar by remember { mutableStateOf<UsuarioItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cuenta", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        floatingActionButton = {
            if (sesion.rol.puedeGestionarUsuarios) {
                ExtendedFloatingActionButton(
                    onClick = { mostrarNuevoUsuario = true },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("Nuevo usuario") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            // Espacio abajo para que el botón flotante no tape el último usuario
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(sesion.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            sesion.rol.nombreVisible,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = descripcionDelRol(sesion.rol),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onCerrarSesion) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cerrar sesión")
                        }
                    }
                }
            }

            if (sesion.rol.puedeGestionarUsuarios) {
                item {
                    Text(
                        "Usuarios",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                items(usuarios, key = { it.idUsuario }) { usuario ->
                    FilaUsuario(
                        usuario = usuario,
                        esUsuarioActual = usuario.idUsuario == sesion.idUsuario,
                        onEliminar = { usuarioAEliminar = usuario }
                    )
                }

                if (usuarios.size <= 1) {
                    item {
                        Text(
                            "Todavía no creaste usuarios. Con \"Nuevo usuario\" le das acceso a un empleado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (mostrarNuevoUsuario) {
        DialogoNuevoUsuario(
            onCrear = onCrearUsuario,
            onCerrar = { mostrarNuevoUsuario = false }
        )
    }

    usuarioAEliminar?.let { usuario ->
        AlertDialog(
            onDismissRequest = { usuarioAEliminar = null },
            title = { Text("Eliminar a ${usuario.nombre}") },
            text = { Text("No va a poder volver a ingresar con ese usuario.") },
            confirmButton = {
                TextButton(onClick = {
                    onEliminarUsuario(usuario)
                    usuarioAEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { usuarioAEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun FilaUsuario(
    usuario: UsuarioItem,
    esUsuarioActual: Boolean,
    onEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (esUsuarioActual) "${usuario.nombre} (vos)" else usuario.nombre,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = usuario.rol.nombreVisible,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // El dueño no se puede eliminar (ni a sí mismo)
            if (usuario.rol != Rol.DUENO) {
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar a ${usuario.nombre}")
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun DialogoNuevoUsuario(
    onCrear: suspend (nombre: String, contrasena: String, rol: Rol) -> String?,
    onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var repetir by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf(Rol.EMPLEADO) }
    var error by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun crear() {
        error = when {
            nombre.isBlank() || contrasena.isBlank() -> "Completá el nombre y la contraseña"
            contrasena.length < MINIMO_CONTRASENA -> "La contraseña debe tener al menos $MINIMO_CONTRASENA caracteres"
            contrasena != repetir -> "Las contraseñas no coinciden"
            else -> null
        }
        if (error != null) return
        guardando = true
        scope.launch {
            try {
                val resultado = onCrear(nombre.trim(), contrasena, rol)
                if (resultado == null) onCerrar() else error = resultado
            } finally {
                guardando = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nuevo usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; error = null },
                    label = { Text("Nombre de usuario") },
                    singleLine = true,
                    enabled = !guardando,
                    modifier = Modifier.visibleAlEscribir()
                )
                OutlinedTextField(
                    value = contrasena,
                    onValueChange = { contrasena = it; error = null },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    enabled = !guardando,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.visibleAlEscribir()
                )
                OutlinedTextField(
                    value = repetir,
                    onValueChange = { repetir = it; error = null },
                    label = { Text("Repetir contraseña") },
                    singleLine = true,
                    enabled = !guardando,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.visibleAlEscribir()
                )

                Text("Permisos", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
                Column(modifier = Modifier.selectableGroup()) {
                    listOf(Rol.EMPLEADO, Rol.ADMINISTRADOR).forEach { opcion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = rol == opcion,
                                    onClick = { rol = opcion },
                                    role = Role.RadioButton,
                                    enabled = !guardando
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            RadioButton(selected = rol == opcion, onClick = null)
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(opcion.nombreVisible)
                                Text(
                                    descripcionDelRol(opcion),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = ::crear, enabled = !guardando) { Text("Crear usuario") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !guardando) { Text("Cancelar") }
        }
    )
}

private fun descripcionDelRol(rol: Rol): String = when (rol) {
    Rol.DUENO -> "Todos los permisos, incluido crear usuarios."
    Rol.ADMINISTRADOR -> "Puede hacer todo menos crear usuarios: editar el inventario, ingresar stock y ver ventas."
    Rol.EMPLEADO -> "Ve el inventario y el historial de ventas, y usa el escáner para buscar o descontar stock. No puede editar ni ingresar stock."
}

private const val MINIMO_CONTRASENA = 4
