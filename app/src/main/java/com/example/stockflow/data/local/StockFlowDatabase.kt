package com.example.stockflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductoEntity::class,
        LoteEntity::class,
        VentaEntity::class,
        DetalleVentaEntity::class,
        MovimientoEntity::class,
        DispositivoEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StockFlowDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao

    abstract fun loteDao(): LoteDao

    abstract fun ventaDao(): VentaDao

    abstract fun detalleVentaDao(): DetalleVentaDao

    abstract fun movimientoDao(): MovimientoDao

    abstract fun dispositivoDao(): DispositivoDao

    companion object {

        @Volatile
        private var INSTANCE: StockFlowDatabase? = null

        fun getDatabase(context: Context): StockFlowDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockFlowDatabase::class.java,
                    "stockflow_database"
                ).build()

                INSTANCE = instance

                instance
            }
        }
    }
}