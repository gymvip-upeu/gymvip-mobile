package pe.edu.upeu.gymvip

enum class IdentificadorLogin { CORREO, CODIGO_SOCIO }

/** Configuración central. Las rutas REST conservan /api/v1 en los adaptadores. */
object AppConfig {
    const val API_ANDROID_LOCAL = "http://10.0.2.2:8081"
    const val API_IOS_LOCAL = "http://localhost:8081"
    const val API_BASE_URL_PROD = "https://gymvip-backend.onrender.com"
    var apiBaseUrl = API_BASE_URL_PROD
    var debug = false
    var versionName = ""
    const val MODO_DEMO = false
    const val MOSTRAR_ESTADOS_DEMO = false
    const val FEATURE_QR = false
    const val FEATURE_FAVORITOS = true
    const val FEATURE_SERVICIOS = false
    const val DIAS_PROXIMA_A_VENCER = 7
    val IDENTIFICADOR_LOGIN = IdentificadorLogin.CORREO
    // Pendiente: número oficial del gimnasio; no abrir WhatsApp hasta configurarlo.
    const val WHATSAPP_GYM = ""
    const val ZONA_HORARIA = "America/Lima"
}
