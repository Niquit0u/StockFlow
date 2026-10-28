package com.example.stockflow.data.repository

import com.example.stockflow.data.local.dao.UsuarioDao
import com.example.stockflow.data.local.entity.UsuarioEntity
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UsuarioRepository(private val usuarioDao: UsuarioDao) {

    /** @return false si ya existe un usuario con ese nombre. */
    suspend fun registrar(nombre: String, clave: String, rol: String): Boolean {
        val usuario = UsuarioEntity(nombreUsuario = nombre.trim(), contrasena = hashear(clave), rol = rol)
        return usuarioDao.insertarUsuario(usuario) != -1L
    }

    /** @return el usuario si nombre y contraseña coinciden, null si no. */
    suspend fun validar(nombre: String, clave: String): UsuarioEntity? {
        val usuario = usuarioDao.buscarUsuario(nombre.trim()) ?: return null
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
