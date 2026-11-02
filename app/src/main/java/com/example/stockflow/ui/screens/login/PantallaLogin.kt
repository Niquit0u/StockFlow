package com.example.stockflow.ui.screens.login

import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Storefront
import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.stockflow.ui.componentes.visibleAlEscribir

/**
 * Pantalla de inicio de sesión, con la opción de crear una cuenta nueva.
 *
 * "Crear cuenta" registra un local nuevo con su dueño. Los empleados de un local no se registran acá:
 * los crea el dueño desde la pestaña Cuenta.
 * @param onIniciarSesion valida correo y contraseña; devuelve true si son correctos.
 * @param onCrearCuenta crea la cuenta (nombre, email, contraseña); devuelve null si se creó,
 * o el mensaje de error para mostrar (por ejemplo, email ya usado).
 */
@Composable
fun PantallaLogin(
    onIniciarSesion: suspend (email: String, contrasena: String) -> Boolean,
    onCrearCuenta: suspend (nombreLocal: String, nombre: String, email: String, contrasena: String) -> String?
) {
    // false = iniciar sesión, true = crear cuenta
    var modoCrearCuenta by rememberSaveable { mutableStateOf(false) }

    var nombreLocal by rememberSaveable { mutableStateOf("") }

    var nombre by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var repetirContrasena by remember { mutableStateOf("") }
    var contrasenaVisible by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Al tocar "Crear cuenta" el primer campo (nombre del local) recibe el foco y se abre el teclado
    val focoPrimerCampo = remember { FocusRequester() }
    val teclado = LocalSoftwareKeyboardController.current
    var enfocarNombre by remember { mutableStateOf(false) }

    LaunchedEffect(modoCrearCuenta, enfocarNombre) {
        if (modoCrearCuenta && enfocarNombre) {
            focoPrimerCampo.requestFocus()
            teclado?.show()
            enfocarNombre = false
        }
    }

    fun cambiarModo() {
        // Solo al pasar a "Crear cuenta"; al volver al login no se enfoca nada
        enfocarNombre = !modoCrearCuenta
        modoCrearCuenta = !modoCrearCuenta
        mensajeError = null
        contrasena = ""
        repetirContrasena = ""
    }

    // Devuelve el error a mostrar, o null si el formulario está completo y es válido
    fun validarFormulario(): String? = when {
        email.isBlank() || contrasena.isBlank() -> "Por favor completa todos los campos"
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Ingresá un correo válido, por ejemplo nombre@mail.com"
        !modoCrearCuenta -> null
        nombreLocal.isBlank() || nombre.isBlank() || repetirContrasena.isBlank() -> "Por favor completa todos los campos"
        contrasena.length < MINIMO_CONTRASENA -> "La contraseña debe tener al menos $MINIMO_CONTRASENA caracteres"
        contrasena != repetirContrasena -> "Las contraseñas no coinciden"
        else -> null
    }

    fun enviar() {
        if (cargando) return
        validarFormulario()?.let {
            mensajeError = it
            return
        }
        focusManager.clearFocus()
        cargando = true
        scope.launch {
            try {
                if (modoCrearCuenta) {
                    onCrearCuenta(nombreLocal.trim(), nombre.trim(), email.trim(), contrasena)?.let { error ->
                        mensajeError = error
                    }
                } else if (!onIniciarSesion(email.trim(), contrasena)) {
                    mensajeError = "Correo o contraseña incorrectos"
                    contrasena = ""
                }
            } finally {
                // Siempre se libera el botón, aunque la validación falle o lance una excepción
                cargando = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding() // Respeta barras del sistema (la app usa edge-to-edge)
                .imePadding() // Sube el contenido cuando aparece el teclado
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Título
            Text(
                text = if (modoCrearCuenta) "Crear cuenta" else "Bienvenido",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (modoCrearCuenta) {
                    "Creá tu local y tu cuenta de dueño"
                } else {
                    "Inicia sesión para continuar"
                },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Nombre del local y del dueño: solo al crear la cuenta (el nombre no sirve para iniciar sesión)
            if (modoCrearCuenta) {
                OutlinedTextField(
                    value = nombreLocal,
                    onValueChange = {
                        nombreLocal = it
                        mensajeError = null
                    },
                    label = { Text("Nombre del local") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    enabled = !cargando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focoPrimerCampo)
                        .visibleAlEscribir(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        mensajeError = null
                    },
                    label = { Text("Tu nombre") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    enabled = !cargando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .visibleAlEscribir(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Correo: es único y es lo único con lo que se inicia sesión
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    mensajeError = null
                },
                label = { Text("Correo electrónico") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null
                    )
                },
                singleLine = true,
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth().visibleAlEscribir(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            CampoContrasena(
                valor = contrasena,
                onValorChange = {
                    contrasena = it
                    mensajeError = null
                },
                etiqueta = "Contraseña",
                visible = contrasenaVisible,
                onCambiarVisibilidad = { contrasenaVisible = !contrasenaVisible },
                habilitado = !cargando,
                esError = mensajeError != null,
                esUltimoCampo = !modoCrearCuenta,
                onListo = ::enviar
            )

            if (modoCrearCuenta) {
                Spacer(modifier = Modifier.height(16.dp))

                CampoContrasena(
                    valor = repetirContrasena,
                    onValorChange = {
                        repetirContrasena = it
                        mensajeError = null
                    },
                    etiqueta = "Repetir contraseña",
                    visible = contrasenaVisible,
                    onCambiarVisibilidad = { contrasenaVisible = !contrasenaVisible },
                    habilitado = !cargando,
                    esError = mensajeError != null,
                    esUltimoCampo = true,
                    onListo = ::enviar
                )
            }

            // Mensaje de error si la validación falla
            mensajeError?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = ::enviar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !cargando
            ) {
                if (cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = if (modoCrearCuenta) "Crear cuenta" else "Iniciar Sesión",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = ::cambiarModo, enabled = !cargando) {
                Text(if (modoCrearCuenta) "Ya tengo cuenta. Iniciar sesión" else "¿Tenés un local? Crear cuenta")
            }

            // Los empleados no se registran solos: evita que creen un local nuevo por error
            Text(
                text = "Si trabajás en un local, pedile al dueño que te cree un usuario con tu correo.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CampoContrasena(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    visible: Boolean,
    onCambiarVisibilidad: () -> Unit,
    habilitado: Boolean,
    esError: Boolean,
    esUltimoCampo: Boolean,
    onListo: () -> Unit
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null
            )
        },
        trailingIcon = {
            IconButton(onClick = onCambiarVisibilidad) {
                Icon(
                    imageVector = if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        },
        singleLine = true,
        enabled = habilitado,
        isError = esError,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth().visibleAlEscribir(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = if (esUltimoCampo) ImeAction.Done else ImeAction.Next
        ),
        // "Listo" en el teclado envía el formulario
        keyboardActions = KeyboardActions(onDone = { onListo() })
    )
}

private const val MINIMO_CONTRASENA = 4

@Preview(showBackground = true)
@Composable
private fun PantallaLoginPreview() {
    MaterialTheme {
        PantallaLogin(
            onIniciarSesion = { _, _ -> true },
            onCrearCuenta = { _, _, _, _ -> null }
        )
    }
}
