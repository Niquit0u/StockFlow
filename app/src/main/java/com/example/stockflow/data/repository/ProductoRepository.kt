package com.example.stockflow.data.repository


class ProductoRepository(private val productoDao: ProductoDao) {

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