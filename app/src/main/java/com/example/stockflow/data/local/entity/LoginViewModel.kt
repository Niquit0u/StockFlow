package com.example.stockflow.data.local.entity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockflow.data.local.dao.UsuarioDao
import kotlinx.coroutines.launch

class LoginViewModel(private val usuarioDao: UsuarioDao) : ViewModel() {

    fun registrarDueno(nombre: String, clave: String) {
        viewModelScope.launch {
            usuarioDao.insertarUsuario(
                UsuarioEntity(nombreUsuario = nombre, contrasena = clave, rol = "DUENO")
            )
        }
    }

    fun registrarCliente(nombre: String, clave: String) {
        viewModelScope.launch {
            usuarioDao.insertarUsuario(
                UsuarioEntity(nombreUsuario = nombre, contrasena = clave, rol = "CLIENTE")
            )
        }
    }

    fun intentarLogin(nombreIngresado: String, claveIngresada: String) {
        viewModelScope.launch {
            val usuario = usuarioDao.buscarUsuario(nombreIngresado)

            if (usuario != null && usuario.contrasena == claveIngresada) {
                when (usuario.rol) {
                    "DUENO" -> {
                        // Lógica para actualizar el estado y navegar al panel de control
                    }
                    "CLIENTE" -> {
                        // Lógica para actualizar el estado y navegar al catálogo
                    }
                }
            } else {
                // Lógica para mostrar un error en la pantalla
            }
        }
    }
}