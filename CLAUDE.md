# StockFlow

App Android (Kotlin + Jetpack Compose + Material3) para controlar stock y vencimientos de lotes en un comercio.
Escanea códigos de barra con CameraX + ML Kit y guarda los datos localmente con Room.

## Build

- Compilar: `./gradlew :app:assembleDebug`
- Solo chequear Kotlin (más rápido): `./gradlew :app:compileDebugKotlin`
- Tests unitarios: `./gradlew :app:testDebugUnitTest`
- Usar el JDK de Android Studio si `java` no está en el PATH: `JAVA_HOME=/opt/android-studio/jbr`
- Dependencias y versiones en `gradle/libs.versions.toml` (version catalog).

## Estructura

```
app/src/main/java/com/example/stockflow/
├── MainActivity.kt              # Navegación por pestañas (Vencimientos / Escáner / Ingresar Stock)
├── data/
│   ├── local/
│   │   ├── StockFlowDatabase.kt # ÚNICA base de datos Room (singleton getDatabase)
│   │   ├── entity/              # Solo @Entity y POJOs de consultas (nada de ViewModels)
│   │   └── dao/                 # Interfaces @Dao
│   ├── repository/              # Lógica de acceso a datos; la UI habla con esto, no con los DAO
│   └── model/Modelos.kt         # Modelos de UI (LoteItem, Producto, EstadoSemaforo)
└── ui/
    ├── screens/<feature>/PantallaX.kt
    └── theme/
```

## Convenciones

- Código, nombres y comentarios en español (`insertar`, `obtenerPorId`, `PantallaAgregar`).
- Pantallas: `ui/screens/<feature>/Pantalla<Nombre>.kt`.
- DAOs: funciones `suspend` para escrituras/lecturas puntuales, `Flow` para lo que la UI observa.
- Fechas en la BD como `Long` (epoch millis, medianoche local). En la UI como `dd/MM/yyyy`.
- minSdk 24: no usar `java.time` sin desugaring; usar `Calendar`/`SimpleDateFormat`.

## Base de datos (Room)

- Al cambiar una entidad: subir `version` en `StockFlowDatabase` y agregarla a `entities` si es nueva.
- El esquema se exporta a `app/schemas/` (commitear esos JSON).
- En desarrollo se usa migración destructiva; antes de publicar hay que escribir `Migration`s reales.
- Operaciones que tocan varias tablas van en `database.withTransaction { }` dentro del repositorio.
