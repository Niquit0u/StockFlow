package com.example.stockflow.data.repository

import com.example.stockflow.Rol
import com.example.stockflow.SesionUsuario
import com.example.stockflow.UsuarioItem
import androidx.room.withTransaction
import com.example.stockflow.data.local.StockFlowDatabase
import com.example.stockflow.data.local.entity.LocalEntity
import com.example.stockflow.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UsuarioRepository(private val database: StockFlowDatabase) {

    private val usuarioDao = database.usuarioDao()
    private val localDao = database.localDao()

    enum class ResultadoRegistro { CREADO, EMAIL_EN_USO, NO_PERMITIDO }

    /** Usuarios del local (para la lista que ve el dueño en Cuenta). */
    fun observarUsuarios(idLocal: Int): Flow<List<UsuarioItem>> = usuarioDao.observarDelLocal(idLocal).map { lista ->
        lista.map { UsuarioItem(it.id, it.nombreUsuario, it.email, Rol.desdeClave(it.rol)) }
    }

    /**
     * "Crear cuenta" del login: crea un local nuevo y su dueño, juntos (si algo falla no queda ninguno).
     * Cada dueño tiene su propio local; los empleados los crea después el dueño con [crearUsuario].
     */
    suspend fun registrarDueno(nombreLocal: String, nombre: String, email: String, clave: String): ResultadoRegistro {
        if (usuarioDao.buscarPorEmail(normalizarEmail(email)) != null) return ResultadoRegistro.EMAIL_EN_USO
        return database.withTransaction {
            val idLocal = localDao.insertar(LocalEntity(nombre = nombreLocal.trim())).toInt()
            insertar(idLocal, nombre, email, clave, Rol.DUENO)
        }
    }

    /** El dueño crea un usuario (empleado o administrador) de su propio local. */
    suspend fun crearUsuario(
        solicitante: SesionUsuario,
        nombre: String,
        email: String,
        clave: String,
        rol: Rol
    ): ResultadoRegistro {
        // Se valida acá también, no solo ocultando el botón en la pantalla
        if (!solicitante.rol.puedeGestionarUsuarios || rol == Rol.DUENO) return ResultadoRegistro.NO_PERMITIDO
        return insertar(solicitante.idLocal, nombre, email, clave, rol)
    }

    /**
     * El dueño elimina un usuario de su local. No puede eliminarse a sí mismo, a otro dueño
     * ni a usuarios de otro local. @return true si se eliminó.
     */
    suspend fun eliminarUsuario(solicitante: SesionUsuario, idUsuario: Int): Boolean {
        if (!solicitante.rol.puedeGestionarUsuarios || idUsuario == solicitante.idUsuario) return false
        val usuario = usuarioDao.buscarPorId(idUsuario) ?: return false
        if (usuario.idLocal != solicitante.idLocal || Rol.desdeClave(usuario.rol) == Rol.DUENO) return false
        usuarioDao.eliminarPorId(idUsuario)
        return true
    }

    /** Sesión del usuario con ese id (para restaurar el inicio de sesión guardado), o null si ya no existe. */
    suspend fun sesionPorId(idUsuario: Int): SesionUsuario? {
        val usuario = usuarioDao.buscarPorId(idUsuario) ?: return null
        return armarSesion(usuario)
    }

    /**
     * Inicio de sesión: solo con correo (es único; el nombre puede repetirse) y contraseña.
     * @return la sesión (con su local) si la contraseña coincide.
     */
    suspend fun validar(email: String, clave: String): SesionUsuario? {
        val usuario = usuarioDao.buscarPorEmail(normalizarEmail(email)) ?: return null
        if (!verificar(clave, usuario.contrasena)) return null
        return armarSesion(usuario)
    }

    private suspend fun armarSesion(usuario: UsuarioEntity): SesionUsuario? {
        val local = localDao.obtenerPorId(usuario.idLocal) ?: return null
        return SesionUsuario(
            idUsuario = usuario.id,
            nombre = usuario.nombreUsuario,
            rol = Rol.desdeClave(usuario.rol),
            idLocal = local.idLocal,
            nombreLocal = local.nombre
        )
    }

    private suspend fun insertar(idLocal: Int, nombre: String, email: String, clave: String, rol: Rol): ResultadoRegistro {
        val emailLimpio = normalizarEmail(email)
        if (usuarioDao.buscarPorEmail(emailLimpio) != null) return ResultadoRegistro.EMAIL_EN_USO

        val usuario = UsuarioEntity(
            idLocal = idLocal,
            nombreUsuario = nombre.trim(),
            email = emailLimpio,
            contrasena = hashear(clave),
            rol = rol.clave
        )
        // Por si otra cuenta con el mismo email se creó justo entre la búsqueda y el insert
        return if (usuarioDao.insertarUsuario(usuario) != -1L) ResultadoRegistro.CREADO else ResultadoRegistro.EMAIL_EN_USO
    }

    private fun normalizarEmail(email: String) = email.trim().lowercase()

    private companion object {
        // PBKDF2WithHmacSHA1 está disponible desde API 10 (minSdk 24 no tiene la variante SHA256 garantizada)
        const val ALGORITMO = "PBKDF2WithHmacSHA1"
        const val ITERACIONES = 10_000
        const val LARGO_BITS = 256

        fun hashear(clave: String): String {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            return "${salt.aHex()}:${derivar(clave, salt).aHex()}"
        }

        fun verificar(clave: String, guardado: String): Boolean {
            val partes = guardado.split(":")
            if (partes.size != 2) return false
            val esperado = partes[1].deHex()
            return MessageDigest.isEqual(derivar(clave, partes[0].deHex()), esperado)
        }

        fun derivar(clave: String, salt: ByteArray): ByteArray {
            val spec = PBEKeySpec(clave.toCharArray(), salt, ITERACIONES, LARGO_BITS)
            return try {
                SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
            }
        }

        fun ByteArray.aHex(): String = joinToString("") { "%02x".format(it) }

        fun String.deHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}
