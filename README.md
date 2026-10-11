# GYM VIP — Aplicación Móvil del Socio (Kotlin Multiplatform)

**Estado:** Repositorio y módulo móvil del socio.  
**Plataforma tecnológica:** Kotlin Multiplatform (KMP) con Compose Multiplatform / UI Nativa para Android e iOS.  
**Propósito:** Proporcionar al socio un canal digital propio de consulta y monitoreo de su membresía y pagos, reduciendo la necesidad de hacer filas en la recepción del gimnasio.

---

## 1. Alcance y Límites del Módulo Móvil

### Funcionalidades Incluidas (Modo Consulta):
- **HU-28: Inicio de sesión móvil:** Autenticación segura vinculada a la cuenta única de socio generada tras el alta presencial.
- **HU-29: Consultar membresía:** Visualización del plan contratado, fecha de inicio, fecha de vencimiento y estado de vigencia (`VIGENTE`, `PRÓXIMA A VENCER`, `VENCIDA`).
  - *Regla clave:* La aplicación permite la consulta de información personal e historial **incluso si la membresía se encuentra vencida**.
- **HU-30: Consultar historial de pagos:** Lista de mensualidades y pagos presenciales registrados con fecha, monto en PEN y medio de pago.
- **HU-33: Notificaciones y avisos:** Visualización de avisos transaccionales y recordatorios enviados por el gimnasio.

### Exclusiones Explícitas:
- **NO existe pasarela de pagos online ni cobros dentro de la app.** Las renovaciones se realizan presencialmente en el mostrador del gimnasio.
- **NO incluye control de acceso biométrico ni apertura de torniquetes.**
- **NO realiza sincronización bidireccional compleja sin conexión.**

---

## 2. Enlaces a Documentación Canónica

- [PRD Canónico](../gymvip-backend/docs/producto/PRD.md)
- [Arquitectura del Sistema](../gymvip-backend/docs/arquitectura/ARCHITECTURE.md)
- [Contratos de API REST](../gymvip-backend/docs/arquitectura/API_CONTRACTS.md)
- [Design System y Tokens Visuales](../gymvip-frontend/docs/ux/DESIGN_SYSTEM.md)

---

## 3. Instrucciones de Arranque y Compilación

### Requisitos:
- Android Studio Ladybug / Meerkat o IntelliJ IDEA con plugin KMP.
- JDK 17 o 21.
- Android SDK 34+.

### Comandos:
```bash
# Compilar módulo común
./gradlew :shared:assemble

# Ejecutar aplicación Android en emulador o dispositivo
./gradlew :composeApp:assembleDebug
```
