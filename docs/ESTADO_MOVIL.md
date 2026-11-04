# ESTADO DEL PROYECTO MÓVIL (KMP) — GYM VIP
**Fecha de corte:** 03 de Noviembre de 2026 (America/Lima)  
**Módulo:** `gymvip-mobile`  
**Responsable Técnico Principal:** André Acosta  
**Rama activa:** `feature/s3-hu05-hu13-hu29-hu30-hu41-acosta`  
**Plataforma:** Kotlin Multiplatform (Android + iOS) con Compose Multiplatform 1.6.11, Kotlin 2.0.21, Material 3, Clean Architecture + MVVM, Ktor 2.3.12, SQLDelight 2.0.2 y GitLive Firebase Remote Config.

---

## 1. Conciliación Normativa de Historias de Usuario (Sprint 3)

De acuerdo con el **Product Backlog GYM VIP V1.0** (hoja *Fichas HU* y *Sprint 3*), se concilia la numeración de historias de usuario frente a borradores preliminares del repositorio:

| ID Oficial | Denominación en Product Backlog | Equivalencia / Trazabilidad Anterior | Alcance y Criterios de Aceptación Observables |
| :--- | :--- | :--- | :--- |
| **HU-05** | Habilitar acceso a la app móvil (Alta Presencial) | HU-05 (Backend/Mobile) | El socio activa su acceso con el código presencial entregado en mostrador y establece su contraseña segura (mínimo 8 caracteres, alfanumérica y mayúsculas). No utiliza DNI ni datos predecibles. |
| **HU-13** | Credencial QR de socio y verificación de ingreso | HU-13 (Web/Backend/Mobile) | Generación de credencial QR dinámica (`GET /api/v1/member-app/qr`) con TTL de 60 segundos (`gymvip://access/v1?cid=...&code=...&nonce=...&exp=...`). Pantalla "Mi código de socio" con advertencia explícita: *"No es autorización de ingreso. Presente este código en mostrador para verificación"*. |
| **HU-29** | Iniciar sesión en la app móvil | Anteriormente listado como HU-28 | Inicio de sesión con código de socio (VIP-XXXXX) o email y contraseña. Almacenamiento seguro de tokens en `PlatformSecureTokenStore` (respaldado por Android Keystore / iOS Keychain). Estados de demo en UI: credenciales erróneas, cuenta inactiva y offline. |
| **HU-30** | Consultar membresía activa | Anteriormente listado como HU-29 | Consulta autoritativa de cobertura: 30 días calendario continuos, cero días de gracia, visualización de días restantes, fecha de inicio, último día válido y condición de pago presencial. Variantes de estado: Vigente, Por vencer, Vencida, Cancelada, Sin plan. |
| **HU-41** | Favoritos propios del catálogo con nota personal | Propuesta CRUD de socio | Gestión de productos y suplementos favoritos del catálogo con nota de recordatorio personal (hasta 250 caracteres). Persistencia local offline-first en SQLite (`member_favorites`), cola de sincronización diferida (`sync_queue`) y resolución de conflictos 409. |

---

## 2. Evaluación por Hitos Académicos

| Hito | Alcance y Objetivos | Estado | Evidencia Observable |
| :--- | :--- | :--- | :--- |
| **HITO 1: Aplicación Inicial** | Setup de proyecto KMP con Gradle 8.11.1, Kotlin 2.0.21, Compose Multiplatform 1.6.11; Clean Architecture + MVVM; NavigationBar (5 tabs: Inicio, Membresía, Catálogo, Avisos, Perfil), Drawer lateral, Scaffold; tokens oficiales Dark & Gold Luxury (Dark, Light, System) persistidos. | **APROBADO** | Compilación de UI compartida en `commonMain`, navegación fluida, temas claro/oscuro implementados y APK Debug generado con éxito (`composeApp-debug.apk`). |
| **HITO 2: Integración y Persistencia** | Cliente HTTP Ktor con Auth Bearer, timeouts y logging seguro; DTOs y mappers explícitos; SQLDelight SQLite local offline-first (.sq); cola durable `sync_queue` con deduplicación y resolución de conflictos 409; almacenamiento seguro `SecureTokenStore` (Keystore en Android, Keychain en iOS); pruebas unitarias con Ktor MockEngine. | **APROBADO** | 6 suites de pruebas unitarias (`commonTest`) ejecutadas y aprobadas al 100% (21 tests, 0 fallas) en `:composeApp:testDebugUnitTest`. |
| **HITO 3: Producto y Distribución** | Refinamiento de experiencia del socio acorde al prototipo Figma (GYM VIP APP UI); integración acotada de Firebase Remote Config con defaults locales seguros; verificación en dispositivo/emulador; soporte para compilación iOS nativa y pipeline de firmas. | **PARCIALMENTE APROBADO / ENTORNO CONDICIONADO** | Target Android compilado y probado (`composeApp-debug.apk`). Firebase adaptado con fallback local sin romper arranque. Firma release AAB y build nativo iOS registrados formalmente como bloqueados/pendientes por ausencia de Mac/Xcode y keystore privado. |

---

## 3. Matriz de Componentes Técnicos y Evidencias de Verificación

| Componente | Estado | Detalle y Evidencia Real |
| :--- | :--- | :--- |
| **Arquitectura Clean + MVVM** | **APROBADO** | Capas estrictas en `composeApp/src/commonMain`: `domain` (Kotlin puro sin frameworks), `data` (Ktor, SQLDelight, SecureStore), `presentation` (Compose, ViewModels con StateFlow). |
| **Tokens Dark & Gold Luxury (Figma)** | **APROBADO** | Colores oficiales Dark (`#0B0D12`, `#151922`, `#E5C158`, `#F5F5F4`) y Light (`#F7F6F2`, `#735711`, `#1D1C19`) integrados en `GymVipTheme`. Splash screen con fondo de atleta idéntico a Figma. |
| **Ktor REST Client** | **APROBADO** | Motor OkHttp en Android, Darwin en iOS; ContentNegotiation con kotlinx.serialization; Bearer Auth; logging con redacción de contraseñas y tokens. |
| **SQLDelight SQLite Local** | **APROBADO** | Esquema `GymVipDatabase.sq` compilado con tablas `member_profile`, `membership_summary`, `payment_history`, `catalog_product`, `faq_item`, `inbox_item`, `favorite_item` y `sync_queue`. |
| **Offline-First & Reconciliación** | **APROBADO** | `SyncQueueManager` probado con SQLite en memoria; resuelve consolidación de operaciones y reconciliación de ID temporal con ID del servidor. |
| **Android APK Debug** | **APROBADO** | Tarea `:composeApp:assembleDebug` completada con éxito. Archivo: `composeApp/build/outputs/apk/debug/composeApp-debug.apk` (11.87 MB). |
| **Suites de Pruebas Unitarias** | **APROBADO** | 6 suites ejecutadas en `:composeApp:testDebugUnitTest`: `MoneyPenTest` (3 tests), `MembershipValidationTest` (3 tests), `ManageFavoritesUseCaseTest` (3 tests), `DtoMapperTest` (3 tests), `KtorMockEngineTest` (6 tests), `SyncQueueManagerTest` (3 tests). Total: **21 tests pasados con 0 fallas** (Gradle BUILD SUCCESSFUL). |
| **Firebase Remote Config** | **PENDIENTE (ADAPTADO)** | Adaptador en `ConfigRepositoryImpl` configurado con defaults locales seguros (banners informativos). Marcado como **NO VERIFICADO EN PRODUCCIÓN** por ausencia de proyecto/credenciales Google Services. |
| **Compilación iOS Nativa / IPA** | **NO EJECUTADO (CONDICIONADO)** | El código compartido y el host nativo SwiftUI (`iosApp/iosApp/iOSApp.swift` y `MainViewController.kt`) están construidos y listos. No ejecutado en este host debido a que Windows carece de macOS/Xcode. |
| **Firma Release AAB** | **NO EJECUTADO (PENDIENTE)** | Pipeline de build preparado. No se generó firma ficticia para proteger la integridad de publicación (pendiente de keystore y contraseñas privadas provistas por el usuario). |

---

## 4. Contratos de Integración Backend (Spring Boot 3.4.3)

Los servicios para la aplicación móvil se encuentran centralizados en el backend en el controlador `MemberAppController` bajo `/api/v1/member-app/**`, requiriendo rol `CLIENTE` y token JWT:

1. `GET /api/v1/member-app/session`: Consulta si el socio debe cambiar contraseña temporal antes de operar.
2. `POST /api/v1/member-app/password`: Cambio forzoso de contraseña que revoca sesiones concurrentes.
3. `GET /api/v1/member-app/me`: Ficha de perfil y resumen de membresía (30 días exactos, días restantes, último día válido).
4. `GET /api/v1/member-app/attendance`: Historial de asistencias registradas en mostrador.
5. `GET /api/v1/member-app/qr`: Token QR dinámico con TTL de 60 segundos (`gymvip://access/v1?...`) y disclaimer legal.
6. `GET /api/v1/member-app/favorites`: Lista de favoritos del socio autenticado.
7. `POST /api/v1/member-app/favorites`: Registro de nuevo producto favorito con nota personal (HU-41).
8. `PUT /api/v1/member-app/favorites/{id}`: Edición de la nota personal del producto favorito (hasta 250 caracteres).
9. `DELETE /api/v1/member-app/favorites/{id}`: Eliminación del favorito del catálogo.
