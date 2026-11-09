# ESTADO DEL PROYECTO MÓVIL (KMP) — GYM VIP
**Fecha de corte:** 09 de Noviembre de 2026 (America/Lima)  
**Módulo:** `gymvip-mobile`  
**Responsable Técnico Principal:** André Acosta  
**Rama activa:** `s3-mobiles-v2`  
**Plataforma:** Kotlin Multiplatform (Android + iOS) con Compose Multiplatform 1.6.11, Kotlin 2.0.21, Material 3, Clean Architecture + MVVM, Koin 3.5.6 (Dependency Injection), Ktor 2.3.12, SQLDelight 2.0.2 y GitLive Firebase Remote Config.

---

## 1. Matriz de Cobertura y Trazabilidad de Historias de Usuario (Sprint 3)

De acuerdo con el **Product Backlog GYM VIP V1.0** (hoja *Fichas HU* y *Sprint 3*), se consolida la implementación completa del alcance móvil:

| ID Oficial | Denominación Oficial | Módulos Involucrados | Criterios de Aceptación y Comportamiento Observable | Estado |
| :--- | :--- | :--- | :--- | :--- |
| **HU-05** | Habilitar acceso y consentimientos legales | `gymvip-mobile`<br>`gymvip-backend` | Activación de acceso presencial. Aceptación explícita y revocable de Términos de Servicio y Política de Privacidad (`TermsAndPrivacyScreen`). Persistencia en backend (`POST /api/v1/member-app/consents`) y base local SQLite (`member_consent`). | **APROBADO** |
| **HU-13** | Credencial QR dinámica de socio | `gymvip-mobile`<br>`gymvip-backend` | Generación y refresco periódico de credencial QR con payload seguro firmado y TTL de 60 segundos (`gymvip://access/v1?cid=...&code=...&nonce=...&exp=...`). Aviso normativo visible: *"No es autorización de ingreso. Presente este código en mostrador para verificación"*. | **APROBADO** |
| **HU-15** | Historial de pagos presenciales | `gymvip-mobile`<br>`gymvip-backend` | Consulta de recibos emitidos en mostrador con detalle de moneda (PEN), método de pago, fecha y estado de conciliación. Persistencia local en SQLite para consulta offline. | **APROBADO** |
| **HU-26** | Buzón de avisos y notificaciones | `gymvip-mobile`<br>`gymvip-backend` | Bandeja de avisos institucionales del gimnasio con soporte para marcado de lectura (`POST /api/v1/member-app/inbox/{id}/read`), contador de no leídos y persistencia offline. | **APROBADO** |
| **HU-27** | Preferencias de recordatorios | `gymvip-mobile` | Pantalla de configuración de notificaciones para vencimiento de membresía, ofertas y avisos de operación. Persistencia de preferencias del usuario. | **APROBADO** |
| **HU-28** | Centro de ayuda y preguntas frecuentes (FAQ) | `gymvip-mobile`<br>`gymvip-backend` | Módulo interactivo con listado categorizado y acordeones expandibles. Sincronización remota desde `GET /api/v1/member-app/faqs` con respaldo en SQLite local. | **APROBADO** |
| **HU-29** | Iniciar sesión en la app móvil | `gymvip-mobile`<br>`gymvip-backend` | Autenticación con código de socio (VIP-XXXXX) o correo y contraseña. Almacenamiento seguro de credenciales con token JWT en `SecureTokenStore`. Soporte para "Recordar credenciales en este dispositivo" (almacena únicamente identificador de socio en Keystore/Keychain, nunca contraseñas en claro). Estados demo: credenciales erróneas, cuenta inactiva y offline. | **APROBADO** |
| **HU-30** | Consultar membresía activa | `gymvip-mobile`<br>`gymvip-backend` | Regla autoritativa de 30 días calendario continuos, cero días de gracia, días restantes exactos, último día válido y estado (Vigente, Por vencer, Vencida, Cancelada, Sin plan). | **APROBADO** |
| **HU-31** | Catálogo y promociones de suplementos | `gymvip-mobile`<br>`gymvip-backend` | Visualización de productos de nutrición y promociones vigentes desde `/api/v1/catalog/products`. Búsqueda, filtrado por categorías y vista de detalle. | **APROBADO** |
| **HU-33** | Instalaciones y servicios VIP | `gymvip-mobile`<br>`gymvip-backend` | Nueva pantalla `ServicesScreen` y drawer lateral que detalla lockers, sauna, duchas, estacionamiento y cafetería. Sincronización desde `/api/v1/member-app/services` y caché SQLite. | **APROBADO** |
| **HU-34** | Estado de sincronización y modo offline | `gymvip-mobile` | Monitoreo de conectividad, banner interactivo de sincronización (`SyncStatusScreen`), cola durable en SQLite (`sync_queue`), deduplicación y resolución de conflictos. | **APROBADO** |
| **HU-35** | Ajustes de cuenta y cierre de sesión | `gymvip-mobile` | Gestión de preferencias de tema (Dark, Light, System), idioma y cierre de sesión seguro. Preserva la cola de sincronización local para no perder transacciones pendientes del socio. | **APROBADO** |
| **HU-41** | Favoritos propios con nota personal | `gymvip-mobile`<br>`gymvip-backend` | Gestión de suplementos favoritos con nota personal de hasta 250 caracteres. Offline-first en SQLite (`member_favorites`), cola transaccional y resolución de conflictos HTTP 409. | **APROBADO** |
| **HU-32** | Rutinas y entrenadores | N/A | **CONDICIONAL / EXCLUIDA** del alcance de Sprint 3 por no contar con aprobación ni especificación técnica del negocio en este ciclo. No se implementó código no autorizado. | **EXCLUIDA** |

---

## 2. Evaluación por Hitos Académicos

| Hito | Alcance y Objetivos | Estado | Evidencia Observable |
| :--- | :--- | :--- | :--- |
| **HITO 1: Aplicación Inicial** | Setup de proyecto KMP con Gradle 8.11.1, Kotlin 2.0.21, Compose Multiplatform 1.6.11; Clean Architecture + MVVM; Koin 3.5.6; NavigationBar (5 tabs: Inicio, Membresía, Catálogo, Avisos, Perfil), Drawer lateral, Scaffold; tokens oficiales Dark & Gold Luxury (Dark, Light, System) persistidos. | **APROBADO** | Compilación de UI compartida en `commonMain`, navegación fluida, temas claro/oscuro implementados y APK Debug generado con éxito (`composeApp-debug.apk`). |
| **HITO 2: Integración y Persistencia** | Cliente HTTP Ktor con Auth Bearer, timeouts y logging seguro; DTOs y mappers explícitos; SQLDelight SQLite local offline-first (.sq); cola durable `sync_queue` con deduplicación y resolución de conflictos 409; almacenamiento seguro `SecureTokenStore` (Keystore en Android, Keychain en iOS); inyección de dependencias Koin; pruebas unitarias con Ktor MockEngine. | **APROBADO** | 6 suites de pruebas unitarias (`commonTest`) ejecutadas y aprobadas al 100% (27 tests, 0 fallas) en `:composeApp:testDebugUnitTest`. |
| **HITO 3: Producto y Distribución** | Refinamiento de experiencia del socio acorde al prototipo Figma (GYM VIP APP UI); integración acotada de Firebase Remote Config con defaults locales seguros; verificación en dispositivo/emulador; soporte para compilación iOS nativa y pipeline de firmas. | **APROBADO (ANDROID) / CONDICIONADO (IOS)** | Target Android compilado y probado (`composeApp-debug.apk`). Firebase adaptado con fallback local sin romper arranque. Firma release AAB y build nativo iOS registrados formalmente como bloqueados/pendientes por ausencia de Mac/Xcode y keystore privado en el host Windows. |

---

## 3. Matriz de Componentes Técnicos y Evidencias de Verificación

| Componente | Estado | Detalle y Evidencia Real |
| :--- | :--- | :--- |
| **Arquitectura Clean + MVVM** | **APROBADO** | Capas estrictas en `composeApp/src/commonMain`: `domain` (Kotlin puro sin frameworks), `data` (Ktor, SQLDelight, SecureStore), `presentation` (Compose, ViewModels con StateFlow). |
| **Inyección de Dependencias (Koin)** | **APROBADO** | Módulos centralizados en `KoinModules.kt` (`coreModule`, `repositoryModule`, `useCaseModule`, `viewModelModule`). `AppContainer` refactorizado como `KoinComponent`. |
| **Tokens Dark & Gold Luxury (Figma)** | **APROBADO** | Colores oficiales Dark (`#0B0D12`, `#151922`, `#E5C158`, `#F5F5F4`) y Light (`#F7F6F2`, `#735711`, `#1D1C19`) integrados en `GymVipTheme`. Splash screen con fondo de atleta idéntico a Figma. |
| **Ktor REST Client** | **APROBADO** | Motor OkHttp en Android, Darwin en iOS; ContentNegotiation con kotlinx.serialization; Bearer Auth; logging con redacción de contraseñas y tokens. Endpoints de FAQs, servicios, avisos y consentimientos implementados. |
| **SQLDelight SQLite Local** | **APROBADO** | Esquema `GymVipDatabase.sq` con tablas `member_profile`, `membership_summary`, `payment_history`, `catalog_product`, `faq_item`, `inbox_item`, `favorite_item`, `gym_service`, `member_consent` y `sync_queue`. |
| **Offline-First & Reconciliación** | **APROBADO** | `SyncQueueManager` probado con SQLite en memoria; resuelve consolidación de operaciones y reconciliación de ID temporal con ID del servidor. |
| **Almacenamiento Seguro** | **APROBADO** | `SecureTokenStore` implementado en Android con `EncryptedSharedPreferences` / `MasterKeys` y en iOS con mutex / keychain placeholder. Soporta persistencia de identificador de socio sin guardar contraseñas. |
| **Android APK Debug** | **APROBADO** | Tarea `:composeApp:assembleDebug` completada con éxito. Archivo: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`. |
| **Suites de Pruebas Unitarias** | **APROBADO** | 6 suites ejecutadas en `:composeApp:testDebugUnitTest`: `MoneyPenTest` (3 tests), `MembershipValidationTest` (3 tests), `ManageFavoritesUseCaseTest` (3 tests), `DtoMapperTest` (4 tests), `KtorMockEngineTest` (11 tests), `SyncQueueManagerTest` (3 tests). Total: **27 tests pasados con 0 fallas** (Gradle BUILD SUCCESSFUL). |
| **Firebase Remote Config** | **ADAPTADO** | Adaptador en `ConfigRepositoryImpl` configurado con defaults locales seguros (banners informativos). |
| **Compilación iOS Nativa / IPA** | **CONDICIONADO** | El código compartido y el host nativo SwiftUI (`iosApp/iosApp/iOSApp.swift` y `MainViewController.kt`) están construidos y listos. No ejecutado en este host debido a que Windows carece de macOS/Xcode. |
| **Firma Release AAB** | **PENDIENTE** | Pipeline de build preparado. Requiere keystore y credenciales de producción del titular. |

---

## 4. Contratos de Integración Backend (Spring Boot 3.4.3)

Los servicios para la aplicación móvil se encuentran centralizados en el backend en el controlador `MemberAppController` bajo `/api/v1/member-app/**`, requiriendo rol `CLIENTE` y token JWT:

1. `GET /api/v1/member-app/session`: Consulta si el socio debe cambiar contraseña temporal antes de operar.
2. `POST /api/v1/member-app/password`: Cambio forzoso de contraseña que revoca sesiones concurrentes.
3. `GET /api/v1/member-app/me`: Ficha de perfil y resumen de membresía (30 días exactos, días restantes, último día válido).
4. `GET /api/v1/member-app/attendance`: Historial de asistencias registradas en mostrador.
5. `GET /api/v1/member-app/qr`: Token QR dinámico con TTL de 60 segundos (`gymvip://access/v1?...`) y disclaimer legal.
6. `GET /api/v1/member-app/faqs`: Listado oficial de preguntas frecuentes del gimnasio categorizadas por orden.
7. `GET /api/v1/member-app/services`: Catálogo de instalaciones y servicios VIP (lockers, sauna, duchas, estacionamiento, cafetería).
8. `GET /api/v1/member-app/inbox`: Bandeja de avisos oficiales e institucionales para el socio.
9. `POST /api/v1/member-app/inbox/{id}/read`: Marcado de aviso como leído.
10. `GET /api/v1/member-app/consents`: Estado de consentimientos legales (términos y políticas de privacidad).
11. `POST /api/v1/member-app/consents`: Registro o actualización de consentimiento legal del socio.
12. `GET /api/v1/member-app/favorites`: Lista de favoritos del socio autenticado.
13. `POST /api/v1/member-app/favorites`: Registro de nuevo producto favorito con nota personal (HU-41).
14. `PUT /api/v1/member-app/favorites/{id}`: Edición de la nota personal del producto favorito (hasta 250 caracteres).
15. `DELETE /api/v1/member-app/favorites/{id}`: Eliminación del favorito del catálogo.
16. `GET /api/v1/catalog/products`: Consulta de catálogo de productos y promociones para socios con rol `CLIENTE`.
