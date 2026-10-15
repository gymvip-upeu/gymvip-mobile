# ESTADO DEL PROYECTO MÓVIL (KMP) — GYM VIP
**Fecha de corte:** 15 de Octubre de 2026  
**Módulo:** `gymvip-mobile`  
**Rama activa:** `feature/mobile-kmp-clean-architecture`  
**Plataforma:** Kotlin Multiplatform (Android + iOS) con Compose Multiplatform, Material 3, Clean Architecture + MVVM, Ktor, SQLDelight y GitLive Firebase Remote Config.

---

## 1. Evaluación por Hitos Académicos

| Hito | Alcance y Objetivos | Estado | Evidencia Observable |
| :--- | :--- | :--- | :--- |
| **HITO 1: Aplicación Inicial** | Setup de proyecto KMP con Gradle 8.11.1, Kotlin 2.0.21, Compose Multiplatform 1.6.11; Clean Architecture + MVVM; NavigationBar (5 tabs), Drawer lateral, Scaffold; tokens oficiales Dark & Gold Luxury (Dark, Light, System) persistidos. | **APROBADO** | Compilación de UI compartida en `commonMain`, navegación fluida, temas claro/oscuro implementados y APK Debug generado con éxito (`composeApp-debug.apk`). |
| **HITO 2: Integración y Persistencia** | Cliente HTTP Ktor con Auth Bearer, timeouts y logging seguro; DTOs y mappers explícitos; SQLDelight SQLite local offline-first (.sq); cola durable `sync_queue` con deduplicación y resolución de conflictos 409; almacenamiento seguro `SecureTokenStore` (Keystore en Android, Keychain en iOS); pruebas unitarias con Ktor MockEngine. | **APROBADO** | 6 suites de pruebas unitarias (`commonTest`) ejecutadas y aprobadas al 100% (MoneyPen, MembershipValidation, DtoMapper, KtorMockEngine, SyncQueueManager, ManageFavorites). |
| **HITO 3: Producto y Distribución** | Refinamiento de experiencia del socio; integración acotada de Firebase Remote Config con defaults locales seguros; verificación en dispositivo/emulador; soporte para compilación iOS nativa y pipeline de firmas. | **PARCIALMENTE APROBADO / ENTORNO CONDICIONADO** | Target Android compilado y probado (`composeApp-debug.apk`). Firebase adaptado con fallback local sin romper arranque. Firma release AAB y build nativo iOS registrados formalmente como bloqueados/pendientes por ausencia de Mac/Xcode y keystore privado. |

---

## 2. Matriz de Alcance por Historia de Usuario (HU)

| ID | Historia de Usuario / Requisito | Estado | Comportamiento Implementado |
| :--- | :--- | :--- | :--- |
| **HU-05** | Activación presencial de cuenta | **APROBADO** | Pantalla y caso de uso `ActivateAccountUseCase` que exige código presencial de alta entregado en recepción y contraseña propia del titular (mínimo 8 caracteres, mayúsculas, minúsculas y dígitos). No usa DNI ni datos predecibles. |
| **HU-28** | Inicio de sesión y token seguro | **APROBADO** | `LoginUseCase`, autenticación Bearer en `GymVipHttpClient`, almacenamiento seguro en `PlatformSecureTokenStore` (respaldado por Keystore en Android) y cierre de sesión que purga tokens y separa caché local por cuenta. |
| **HU-29** | Consulta de membresía activa | **APROBADO** | Resumen de membresía con regla estricta de **30 días calendario continuos**, cero tolerancia de días de gracia y último día válido. Muestra datos del servidor sin alterar vigencias en el cliente. |
| **HU-30** | Historial de pagos presenciales (PEN) | **APROBADO** | Consulta de solo lectura en Soles con minor units Long (`MoneyPen`), fecha, método (YAPE, PLIN, etc.) y referencia. **Accesible aunque la membresía esté vencida**, siempre que la cuenta continúe habilitada. Sin botones de cobro online. |
| **HU-25/26/34** | Catálogo de servicios y promociones | **APROBADO** | Visualización de planes, suplementos y productos con filtro por categoría, etiquetas de descuento y caché local SQLite para consulta sin conexión. |
| **HU-27** | FAQ y Centro de Ayuda | **APROBADO** | Preguntas frecuentes con acordeones interactivos accesibles desde el Drawer lateral, detallando horarios, reglas de 30 días y política presencial. |
| **HU-33** | Buzón de avisos y comunicados | **APROBADO** | Lista de avisos categorizados (Recordatorio, Vencimiento, Comunicado) con badge de no leídos y marcado local reactivo. |
| **PROPUESTA CRUD** | "Mis favoritos del catálogo" con nota personal | **APROBADO** | Ampliación académica respetando el dominio socio (E05/E06): guardar, consultar, editar nota personal (hasta 250 caracteres) y quitar favorito propio. Persistencia local en SQLite con cola durable offline, deduplicación (`CREATE` + `UPDATE`), y manejo de conflictos 409. |

---

## 3. Matriz de Componentes Técnicos y Evidencias

| Componente | Estado | Detalle y Evidencia Real |
| :--- | :--- | :--- |
| **Arquitectura Clean + MVVM** | **APROBADO** | Capas estrictas en `composeApp/src/commonMain`: `domain` (Kotlin puro sin frameworks), `data` (Ktor, SQLDelight, SecureStore), `presentation` (Compose, ViewModels con StateFlow). |
| **Tokens Dark & Gold Luxury** | **APROBADO** | Colores oficiales Dark (`#0B0D12`, `#151922`, `#E5C158`, `#F5F5F4`) y Light (`#F7F6F2`, `#735711`, `#1D1C19`) integrados en `GymVipTheme`. |
| **Ktor REST Client** | **APROBADO** | Motor OkHttp en Android, Darwin en iOS; ContentNegotiation con kotlinx.serialization; Bearer Auth; logging con redacción de contraseñas y tokens. |
| **SQLDelight SQLite Local** | **APROBADO** | Esquema `GymVipDatabase.sq` compilado con tablas `member_profile`, `membership_summary`, `payment_history`, `catalog_product`, `faq_item`, `inbox_item`, `favorite_item` y `sync_queue`. |
| **Offline-First & Reconciliación** | **APROBADO** | `SyncQueueManager` probado con SQLite en memoria; resuelve consolidación de operaciones y reconciliación de ID temporal con ID del servidor. |
| **Android APK Debug** | **APROBADO** | Tarea `:composeApp:assembleDebug` completada con éxito. Archivo: `composeApp/build/outputs/apk/debug/composeApp-debug.apk` (11.87 MB). |
| **Suites de Pruebas Unitarias** | **APROBADO** | 6 suites ejecutadas en `:composeApp:testDebugUnitTest`: `MoneyPenTest` (3 tests), `MembershipValidationTest` (3 tests), `ManageFavoritesUseCaseTest` (3 tests), `DtoMapperTest` (3 tests), `KtorMockEngineTest` (6 tests), `SyncQueueManagerTest` (3 tests). Total: 21 tests pasados con 0 fallas. |
| **Firebase Remote Config** | **PENDIENTE (ADAPTADO)** | Adaptador en `ConfigRepositoryImpl` configurado con defaults locales seguros (banners informativos). Marcado como **NO VERIFICADO EN PRODUCCIÓN** por ausencia de proyecto/credenciales Google Services. |
| **Compilación iOS Nativa / IPA** | **NO EJECUTADO (CONDICIONADO)** | El código compartido y el host nativo SwiftUI (`iosApp/iosApp/iOSApp.swift` y `MainViewController.kt`) están construidos y listos. No ejecutado en este host debido a que Windows carece de macOS/Xcode. |
| **Firma Release AAB** | **NO EJECUTADO (PENDIENTE)** | Pipeline de build preparado. No se generó firma ficticia para proteger la integridad de publicación (pendiente de keystore y contraseñas privadas provistas por el usuario). |

---

## 4. Dependencias del Backend Registradas

Para la ratificación formal de la integración online en el backend Spring Boot:
1. `GET /api/v1/me/profile`: Consulta de la ficha del socio autenticado a partir del sujeto del token.
2. `GET /api/v1/me/membership`: Resumen de vigencia del socio autenticado (30 días exactos, días restantes).
3. `GET /api/v1/me/payments`: Lista de pagos históricos asociados a la cuenta del socio.
4. `POST /api/v1/auth/activate`: Endpoint presencial para activación de cuenta con token de un solo uso y nueva contraseña.
5. Endpoints orientativos para la propuesta de favoritos:
   - `GET /api/v1/me/favorites`
   - `POST /api/v1/me/favorites`
   - `PUT /api/v1/me/favorites/{id}`
   - `DELETE /api/v1/me/favorites/{id}`
