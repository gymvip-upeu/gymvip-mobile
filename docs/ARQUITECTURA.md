# Arquitectura móvil

`androidApp` → `shared` (`presentation` → `domain` ← `data`).

- `androidApp/src/main`: arranque Android, configuración de entorno y Activity edge-to-edge.
- `shared/src/commonMain/kotlin/pe/edu/upeu/gymvip`: App, AppConfig, navegación, Compose, contratos, casos de uso, repositorios, mappers y Koin.
- `shared/src/androidMain`: CIO, SQLite y almacén de tokens Android.
- `shared/src/iosMain`: Darwin, SQLite y almacén iOS, MainViewController que inicializa la composición de dependencias.
- `shared/src/commonTest`: pruebas de dominio y HTTP multiplataforma.
- `shared/src/androidHostTest`: pruebas SQLite que necesitan JDBC.
- `iosApp`: host SwiftUI, importando el framework estático Shared.

La migración conserva la lógica previa. Las afirmaciones históricas de integración en ESTADO_MOVIL.md no constituyen evidencia nueva. Consultar DECISIONES.md y PRUEBAS.md para alcance y verificación de esta entrega.
