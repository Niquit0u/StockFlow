package com.example.stockflow.data.repository

import com.example.stockflow.data.local.dao.ProductoDao
import com.example.stockflow.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.Flow

class ProductoRepository(private val productoDao: ProductoDao) {

    fun observarTodos(): Flow<List<ProductoEntity>> {
        return productoDao.observarTodos()
    }

    suspend fun insertarProducto(producto: ProductoEntity): Long {
        return productoDao.insertar(producto)
    }

    suspend fun obtenerTodos(): List<ProductoEntity> {
        return productoDao.obtenerTodos()
    }

    suspend fun obtenerPorCodigoBarra(codigo: String): ProductoEntity? {
        return productoDao.obtenerPorCodigoBarra(codigo)
    }

    suspend fun actualizarProducto(producto: ProductoEntity) {
        productoDao.actualizar(producto)
    }

    suspend fun eliminarProducto(producto: ProductoEntity) {
        productoDao.eliminar(producto)
    }
}
