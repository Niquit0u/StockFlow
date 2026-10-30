package com.example.stockflow.data.repository

import com.example.stockflow.Rol
import com.example.stockflow.SesionUsuario
import com.example.stockflow.UsuarioItem
import com.example.stockflow.data.local.dao.UsuarioDao
import com.example.stockflow.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UsuarioRepository(private val usuarioDao: UsuarioDao) {

    enum class ResultadoRegistro { CREADO, NOMBRE_EN_USO, EMAIL_EN_USO, NO_PERMITIDO }

    /** false mientras el teléfono no tenga ninguna cuenta: el login ofrece crear la del dueño. */
    suspend fun hayUsuarios(): Boolean = usuarioDao.contar() > 0

    fun observarUsuarios(): Flow<List<UsuarioItem>> = usuarioDao.observarTodos().map { lista ->
        lista.map { UsuarioItem(it.id, it.nombreUsuario, it.email, Rol.desdeClave(it.rol)) }
    }

    /**
     * Crea la cuenta del dueño desde el login. Solo se permite cuando todavía no hay ninguna cuenta:
     * después, los usuarios los crea el dueño con [crearUsuario].
     */
    suspend fun registrarDueno(nombre: String, email: String, clave: String): ResultadoRegistro {
        if (hayUsuarios()) return ResultadoRegistro.NO_PERMITIDO
        return insertar(nombre, email.trim().lowercase(), clave, Rol.DUENO)
    }

    /** El dueño crea un usuario (empleado o administrador) con nombre y contraseña. */
    suspend fun crearUsuario(
        solicitante: SesionUsuario,
        nombre: String,
        clave: String,
        rol: Rol
    ): ResultadoRegistro {
        // Se valida acá también, no solo ocultando el botón en la pantalla
        if (!solicitante.rol.puedeGestionarUsuarios || rol == Rol.DUENO) return ResultadoRegistro.NO_PERMITIDO
        return insertar(nombre, null, clave, rol)
    }

    /** El dueño elimina un usuario. No puede eliminarse a sí mismo ni a otro dueño. @return true si se eliminó. */
    suspend fun eliminarUsuario(solicitante: SesionUsuario, idUsuario: Int): Boolean {
        if (!solicitante.rol.puedeGestionarUsuarios || idUsuario == solicitante.idUsuario) return false
        val usuario = usuarioDao.buscarPorId(idUsuario) ?: return false
        if (Rol.desdeClave(usuario.rol) == Rol.DUENO) return false
        usuarioDao.eliminarPorId(idUsuario)
        return true
    }

    /** @param usuarioOEmail el nombre de usuario o el email. @return la sesión si la contraseña coincide. */
    suspend fun validar(usuarioOEmail: String, clave: String): SesionUsuario? {
        val texto = usuarioOEmail.trim()
        val usuario = usuarioDao.buscarUsuario(texto)
            ?: usuarioDao.buscarPorEmail(texto.lowercase())
            ?: return null
        if (!verificar(clave, usuario.contrasena)) return null
        return SesionUsuario(usuario.id, usuario.nombreUsuario, Rol.desdeClave(usuario.rol))
    }

    private suspend fun insertar(nombre: String, email: String?, clave: String, rol: Rol): ResultadoRegistro {
        val nombreLimpio = nombre.trim()
        if (usuarioDao.buscarUsuario(nombreLimpio) != null) return ResultadoRegistro.NOMBRE_EN_USO
        if (email != null && usuarioDao.buscarPorEmail(email) != null) return ResultadoRegistro.EMAIL_EN_USO

        val usuario = UsuarioEntity(
            nombreUsuario = nombreLimpio,
            email = email,
            contrasena = hashear(clave),
            rol = rol.clave
        )
        // Por si otra cuenta con el mismo nombre/email se creó justo entre la búsqueda y el insert
        return if (usuarioDao.insertarUsuario(usuario) != -1L) ResultadoRegistro.CREADO else ResultadoRegistro.NOMBRE_EN_USO
    }

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
