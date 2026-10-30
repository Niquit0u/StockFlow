package com.example.stockflow.ui.componentes

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Campo de cantidad: solo acepta dígitos y abre el teclado numérico. */
@Composable
fun CampoCantidad(
    valor: String,
    onValorChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    etiqueta: String = "Cantidad (Unidades)",
    esError: Boolean = false,
    textoAyuda: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo -> onValorChange(nuevo.filter { it.isDigit() }) },
        label = { Text(etiqueta) },
        isError = esError,
        supportingText = textoAyuda,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier.visibleAlEscribir()
    )
}

/**
 * Campo de fecha DD/MM/AAAA.
 * Se escriben solo los números y las barras aparecen solas; el ícono de calendario abre un selector.
 *
 * @param fecha la fecha con barras ("12/03/2027"), o incompleta mientras se escribe ("12/0").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    fecha: String,
    onFechaChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    etiqueta: String = "Fecha Vencimiento (DD/MM/AAAA)",
    esError: Boolean = false,
    textoAyuda: (@Composable () -> Unit)? = null
) {
    var mostrarCalendario by remember { mutableStateOf(false) }
    val digitos = fecha.filter { it.isDigit() }.take(8)

    OutlinedTextField(
        value = digitos,
        onValueChange = { nuevo -> onFechaChange(conBarras(nuevo.filter { it.isDigit() }.take(8))) },
        label = { Text(etiqueta) },
        placeholder = { Text("DD/MM/AAAA") },
        isError = esError,
        supportingText = textoAyuda,
        visualTransformation = TransformacionFecha,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { mostrarCalendario = true }) {
                Icon(Icons.Default.DateRange, contentDescription = "Elegir fecha en el calendario")
            }
        },
        modifier = modifier.visibleAlEscribir()
    )

    if (mostrarCalendario) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = aMillisUtc(fecha))

        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(
                    enabled = estado.selectedDateMillis != null,
                    onClick = {
                        estado.selectedDateMillis?.let { onFechaChange(desdeMillisUtc(it)) }
                        mostrarCalendario = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estado)
        }
    }
}

/** true si [fecha] está completa y es una fecha real (rechaza 31/02, 45/13, etc.). */
fun esFechaValida(fecha: String): Boolean = aMillisUtc(fecha) != null

// "12032027" -> "12/03/2027"; "123" -> "12/3"
private fun conBarras(digitos: String): String = buildString {
    digitos.forEachIndexed { i, c ->
        if (i == 2 || i == 4) append('/')
        append(c)
    }
}

/** Muestra las barras sobre el texto (que solo tiene dígitos) sin que el usuario tenga que escribirlas. */
private object TransformacionFecha : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformado = conBarras(text.text)
        val mapeo = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 2 -> offset
                offset <= 4 -> offset + 1
                else -> offset + 2
            }.coerceAtMost(transformado.length)

            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 2 -> offset
                offset <= 5 -> offset - 1
                else -> offset - 2
            }.coerceIn(0, text.length)
        }
        return TransformedText(AnnotatedString(transformado), mapeo)
    }
}

// El DatePicker de Material trabaja con medianoche UTC; se convierte en UTC para no correr el día.
private fun formatoUtc() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
    timeZone = TimeZone.getTimeZone("UTC")
    isLenient = false
}

private fun aMillisUtc(fecha: String): Long? {
    if (fecha.length != 10) return null
    return try {
        formatoUtc().parse(fecha)?.time
    } catch (_: ParseException) {
        null
    }
}

private fun desdeMillisUtc(millis: Long): String = formatoUtc().format(millis)
