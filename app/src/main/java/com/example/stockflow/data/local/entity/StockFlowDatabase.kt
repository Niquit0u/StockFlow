package com.example.stockflow.data.local.entity

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.stockflow.data.local.dao.DetalleVentaDao
import com.example.stockflow.data.local.dao.DispositivoDao
import com.example.stockflow.data.local.dao.LoteDao
import com.example.stockflow.data.local.dao.MovimientoDao
import com.example.stockflow.data.local.dao.ProductoDao
import com.example.stockflow.data.local.dao.UsuarioDao
import com.example.stockflow.data.local.dao.VentaDao

@Database(
    entities = [
        ProductoEntity::class,
        LoteEntity::class,
        VentaEntity::class,
        DetalleVentaEntity::class,
        MovimientoEntity::class,
        DispositivoEntity::class,
        UsuarioEntity::class
    ],
    version = 3, // Incrementa a versión 3
    exportSchema = false
)
// ... resto de la clase igual
abstract class StockFlowDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao
    abstract fun loteDao(): LoteDao
    abstract fun ventaDao(): VentaDao
    abstract fun detalleVentaDao(): DetalleVentaDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun dispositivoDao(): DispositivoDao

    // Nuevo DAO para acceder a los datos del usuario
    abstract fun usuarioDao(): UsuarioDao

    companion object {

        @Volatile
        private var INSTANCE: StockFlowDatabase? = null

        fun getDatabase(context: Context): StockFlowDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockFlowDatabase::class.java,
                    "stockflow_database"
                )
                    // Recrea la base de datos si hay cambios de versión sin una migración definida
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}