package com.example.stockflow.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockflow.data.local.entity.UsuarioEntity
import com.example.stockflow.data.repository.UsuarioRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val usuarioRepository: UsuarioRepository) : ViewModel() {

    fun registrarDueno(nombre: String, clave: String) {
        viewModelScope.launch {
            usuarioRepository.registrar(nombre, clave, UsuarioEntity.ROL_DUENO)
            // Si devuelve false el nombre ya existe: mostrar error en la pantalla
        }
    }

    fun registrarCliente(nombre: String, clave: String) {
        viewModelScope.launch {
            usuarioRepository.registrar(nombre, clave, UsuarioEntity.ROL_CLIENTE)
            // Si devuelve false el nombre ya existe: mostrar error en la pantalla
        }
    }

    fun intentarLogin(nombreIngresado: String, claveIngresada: String) {
        viewModelScope.launch {
            val usuario = usuarioRepository.validar(nombreIngresado, claveIngresada)

            if (usuario != null) {
                when (usuario.rol) {
                    UsuarioEntity.ROL_DUENO -> {
                        // Lógica para actualizar el estado y navegar al panel de control
                    }
                    UsuarioEntity.ROL_CLIENTE -> {
                        // Lógica para actualizar el estado y navegar al catálogo
                    }
                }
            } else {
                // Lógica para mostrar un error en la pantalla
            }
        }
    }
}
