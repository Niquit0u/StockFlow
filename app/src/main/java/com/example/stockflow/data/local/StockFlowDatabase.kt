package com.example.stockflow.data.local

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
import com.example.stockflow.data.local.entity.DetalleVentaEntity
import com.example.stockflow.data.local.entity.DispositivoEntity
import com.example.stockflow.data.local.entity.LoteEntity
import com.example.stockflow.data.local.entity.MovimientoEntity
import com.example.stockflow.data.local.entity.ProductoEntity
import com.example.stockflow.data.local.entity.UsuarioEntity
import com.example.stockflow.data.local.entity.VentaEntity

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
    version = 8,
    exportSchema = true
)
abstract class StockFlowDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao
    abstract fun loteDao(): LoteDao
    abstract fun ventaDao(): VentaDao
    abstract fun detalleVentaDao(): DetalleVentaDao
    abstract fun movimientoDao(): MovimientoDao
    abstract fun dispositivoDao(): DispositivoDao
    abstract fun usuarioDao(): UsuarioDao

    companion object {

        @Volatile
        private var INSTANCE: StockFlowDatabase? = null

        fun getDatabase(context: Context): StockFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StockFlowDatabase::class.java,
                    "stockflow_database"
                )
                    // Solo para desarrollo: si cambia la versión sin una Migration, se borra y recrea todo.
                    // Antes de publicar la app hay que reemplazar esto por migraciones reales.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
