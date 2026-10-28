---
name: nueva-pantalla
description: Crea una pantalla Compose nueva en StockFlow siguiendo la estructura ui/screens/<feature>/Pantalla<Nombre>.kt y la conecta a la barra de navegación de MainActivity.
argument-hint: <Nombre> [descripción]
disable-model-invocation: true
---

# Nueva pantalla

Argumentos: $ARGUMENTS

1. Crear `app/src/main/java/com/example/stockflow/ui/screens/<feature-en-minúscula>/Pantalla<Nombre>.kt`
   con un `@Composable fun Pantalla<Nombre>(...)` que reciba datos y callbacks por parámetro (sin acceder a la BD directamente).
2. Usar Material3 y `MaterialTheme.colorScheme`; no hardcodear colores.
3. Si necesita datos, pedirlos a un repositorio de `data/repository/` desde `MainActivity` (o un ViewModel) y pasarlos como parámetros.
4. Agregar un `NavigationBarItem` en `AplicacionInteractivaStockFlow` (MainActivity.kt) y su rama en el `when (pestañaSeleccionada)`.
5. Incluir un `@Preview`.
6. Verificar con `./gradlew :app:compileDebugKotlin`.
