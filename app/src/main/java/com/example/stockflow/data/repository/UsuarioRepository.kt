package com.example.stockflow.data.repository

import com.example.stockflow.data.local.dao.UsuarioDao
import com.example.stockflow.data.local.entity.UsuarioEntity
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UsuarioRepository(private val usuarioDao: UsuarioDao) {

    enum class ResultadoRegistro { CREADO, NOMBRE_EN_USO, EMAIL_EN_USO }

    /**
     * Crea una cuenta nueva. La primera cuenta del teléfono queda como dueño; las siguientes, como cliente.
     */
    suspend fun registrar(nombre: String, email: String, clave: String): ResultadoRegistro {
        val nombreLimpio = nombre.trim()
        val emailLimpio = email.trim().lowercase()

        if (usuarioDao.buscarUsuario(nombreLimpio) != null) return ResultadoRegistro.NOMBRE_EN_USO
        if (usuarioDao.buscarPorEmail(emailLimpio) != null) return ResultadoRegistro.EMAIL_EN_USO

        val rol = if (usuarioDao.contar() == 0) UsuarioEntity.ROL_DUENO else UsuarioEntity.ROL_CLIENTE
        val usuario = UsuarioEntity(
            nombreUsuario = nombreLimpio,
            email = emailLimpio,
            contrasena = hashear(clave),
            rol = rol
        )
        // Por si otra cuenta con el mismo nombre/email se creó justo entre la búsqueda y el insert
        return if (usuarioDao.insertarUsuario(usuario) != -1L) ResultadoRegistro.CREADO else ResultadoRegistro.NOMBRE_EN_USO
    }

    /** @param usuarioOEmail el nombre de usuario o el email. @return el usuario si la contraseña coincide. */
    suspend fun validar(usuarioOEmail: String, clave: String): UsuarioEntity? {
        val texto = usuarioOEmail.trim()
        val usuario = usuarioDao.buscarUsuario(texto)
            ?: usuarioDao.buscarPorEmail(texto.lowercase())
            ?: return null
        return usuario.takeIf { verificar(clave, it.contrasena) }
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
