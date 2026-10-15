# GYM VIP — Aplicación Móvil del Socio (Kotlin Multiplatform)

**Plataforma tecnológica:** Kotlin Multiplatform (KMP) con Compose Multiplatform, Material 3, Clean Architecture + MVVM, Ktor, SQLDelight offline-first y Firebase Remote Config acotado.  
**Targets oficiales:** Android (`androidMain`) e iOS (`iosArm64`, `iosSimulatorArm64`).  
**Público objetivo:** Socios del gimnasio GYM VIP (canal de autoservicio para consulta de membresías, historial de pagos, catálogo de planes y servicios, avisos del club y gestión de favoritos con nota personal).

---

## 1. Arquitectura del Sistema (Clean Architecture + MVVM)

El código compartido reside íntegramente en `composeApp/src/commonMain/kotlin/com/gymvip/mobile/`:

```
composeApp/src/commonMain/kotlin/com/gymvip/mobile/
├── di/
│   └── AppContainer.kt                   # Composition Root con inyección por constructor
├── domain/                               # Lógica pura de negocio en Kotlin (sin frameworks)
│   ├── error/AppError.kt                 # Sealed class con jerarquía de errores tipados
│   ├── model/                            # Modelos inmutables (MemberProfile, MembershipSummary,
│   │                                     # MoneyPen con centavos Long, Product, Favorite, etc.)
│   ├── repository/                       # Interfaces puras de repositorio (Auth, Member, Catalog, Favorite)
│   └── usecase/                          # Casos de uso con reglas de negocio (Login, 30 días exactos,
│                                         # validación de favoritos, sincronización offline)
├── data/                                 # Implementación de persistencia y red
│   ├── local/                            # SQLDelight SQLite (.sq) y expect/actual SecureTokenStore
│   ├── mapper/                           # Mapeo explícito DTO <-> Domain <-> Database
│   ├── remote/api/                       # Ktor HttpClient con Bearer Auth, timeouts y logging seguro
│   ├── remote/dto/                       # DTOs serializables con kotlinx.serialization
│   ├── repository/                       # Implementaciones concretas de repositorio
│   └── sync/                             # SyncQueueManager: cola durable SQLite con deduplicación y 409
└── presentation/                         # Interfaz de usuario con Compose Multiplatform
    ├── component/                        # Componentes VIP (VipTopAppBar, VipNavigationBar, VipCards, OfflineBanner)
    ├── navigation/                       # Destinos tipados (NavDestination, MainTab)
    ├── screen/                           # Pantallas: Splash, Login, Activación, Inicio, Membresía, Catálogo, Buzón, Perfil
    ├── theme/                            # Tokens oficiales Dark & Gold Luxury (Dark, Light, System)
    └── viewmodel/                        # ViewModels multiplataforma con StateFlow inmutable
```

---

## 2. Requisitos y Entorno de Ejecución

- **JDK:** Java 21 LTS (`JAVA_HOME` apuntando a JDK 21).
- **Gradle:** Wrapper 8.11.1 incluido en el proyecto (`gradlew.bat` / `./gradlew`).
- **Android SDK:** API 34 (`local.properties` configurado con `sdk.dir`).
- **Entorno iOS:** macOS con Xcode 15+ y simuladores para compilar `iosApp` y firmar framework/IPA. En entornos Windows, el target iOS se mantiene configurado y listo para enlazar sin afirmar compilación nativa simulada.

---

## 3. Comandos de Compilación y Pruebas

### Ejecución de Pruebas Unitarias y de Contrato (commonTest)
```bash
# Ejecutar todas las suites de pruebas unitarias en Android/JVM
./gradlew :composeApp:testDebugUnitTest
```

### Generación de Código SQLDelight
```bash
./gradlew :composeApp:generateCommonMainGymVipDatabaseInterface
```

### Compilación de APK de Android (Debug)
```bash
./gradlew :composeApp:assembleDebug
# El archivo generado se ubica en:
# composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

### Verificación y Build en macOS para iOS
```bash
# Compilar framework para simulador iOS (requiere Mac/Xcode):
./gradlew :composeApp:compileKotlinIosSimulatorArm64

# Abrir proyecto nativo en Xcode:
open iosApp/iosApp.xcodeproj
```

---

## 4. Estado de Integración y Documentación Canónica

Para la trazabilidad detallada por Historias de Usuario, contratos REST, comportamiento offline probado, Firebase y estado de firmas, consulte el documento único:
- [docs/ESTADO_MOVIL.md](docs/ESTADO_MOVIL.md)
