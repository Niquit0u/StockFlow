---
name: room-entity
description: Crea una nueva tabla Room en StockFlow (entidad + DAO + registro en StockFlowDatabase + repositorio opcional). Usar cuando se pida agregar una tabla, entidad o DAO nuevo.
argument-hint: <NombreEntidad> [campos]
---

# Nueva entidad Room

Argumentos: $ARGUMENTS

Pasos:

1. Crear `app/src/main/java/com/example/stockflow/data/local/entity/<Nombre>Entity.kt`
   - `@Entity(tableName = "<plural_snake_case>")`
   - PK `@PrimaryKey(autoGenerate = true) val id<Nombre>: Int = 0` salvo que se pida otra cosa.
   - Por cada `ForeignKey`, agregar un `Index` sobre la columna hija.
   - Fechas como `Long` (epoch millis).
2. Crear `data/local/dao/<Nombre>Dao.kt` siguiendo el estilo de `ProductoDao`:
   `insertar` (devuelve `Long`), `obtenerPorId`, `actualizar`, `eliminar`, y un `observarTodos(): Flow<List<...>>`.
3. En `data/local/StockFlowDatabase.kt`:
   - Agregar la entidad a `entities`.
   - Agregar `abstract fun <nombre>Dao(): <Nombre>Dao`.
   - Subir `version` en 1.
4. Si la UI la va a usar, crear `data/repository/<Nombre>Repository.kt`.
5. Verificar con `./gradlew :app:compileDebugKotlin` (KSP valida las consultas SQL).
