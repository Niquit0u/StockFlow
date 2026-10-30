package com.example.stockflow.data.repository

import android.content.Context
import androidx.core.content.edit
import com.example.stockflow.SesionUsuario

/**
 * Recuerda quién inició sesión aunque se cierre la app.
 * Solo se guarda el id del usuario (nunca la contraseña), en las preferencias privadas de la app.
 */
class SesionRepository(context: Context, private val usuarios: UsuarioRepository) {

    private val preferencias = context.applicationContext.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    fun guardar(sesion: SesionUsuario) {
        preferencias.edit { putInt(CLAVE_ID_USUARIO, sesion.idUsuario) }
    }

    fun borrar() {
        preferencias.edit { remove(CLAVE_ID_USUARIO) }
    }

    /**
     * Sesión de la última vez, leída de nuevo desde la base para tener el nombre y el rol actuales.
     * null si no había sesión o si el usuario ya no existe (por ejemplo, el dueño lo eliminó).
     */
    suspend fun restaurar(): SesionUsuario? {
        if (!preferencias.contains(CLAVE_ID_USUARIO)) return null
        val sesion = usuarios.sesionPorId(preferencias.getInt(CLAVE_ID_USUARIO, 0))
        if (sesion == null) borrar()
        return sesion
    }

    private companion object {
        const val ARCHIVO = "sesion"
        const val CLAVE_ID_USUARIO = "id_usuario"
    }
}
