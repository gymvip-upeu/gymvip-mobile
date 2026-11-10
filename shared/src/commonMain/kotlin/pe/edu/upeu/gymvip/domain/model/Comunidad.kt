package pe.edu.upeu.gymvip.domain.model

data class Suplemento(val id: String, val nombre: String, val marca: String, val categoria: String,
    val presentacion: String, val contenido: String, val sabor: String, val descripcion: String,
    val ingredientes: String, val precio: MoneyPen, val disponible: Boolean, val publicado: Boolean, val imagenes: List<String>)
data class Oferta(val id: String, val producto: Suplemento, val nombre: String, val descripcion: String,
    val condiciones: String, val precioReferencia: MoneyPen, val precioOferta: MoneyPen,
    val inicio: String, val fin: String, val estado: String) { val vigente get() = estado == "VIGENTE" }
data class FavoritoSocio(val id: String, val productoId: String, val nombre: String, val categoria: String,
    val nota: String, val actualizado: String)
data class PaginaComercial<T>(val contenido: List<T>, val pagina: Int, val totalPaginas: Int, val total: Long)
data class FiltroCatalogo(val consulta: String = "", val categoria: String = "", val disponibilidad: String = "",
    val minimo: String = "", val maximo: String = "", val enPromocion: Boolean = false)
data class DatosComunidad(val favoritos: List<FavoritoSocio> = emptyList(), val avisos: List<InboxItem> = emptyList(),
    val preguntas: List<Faq> = emptyList())

fun correoEnmascarado(valor: String): String {
    if (valor.isBlank()) return "Sin registrar"
    if ('•' in valor || '*' in valor) return valor
    if ('@' !in valor) return "••••••"
    return valor.substringBefore('@').take(2) + "••••••@" + valor.substringAfter('@')
}
fun celularEnmascarado(valor: String?): String {
    if (valor.isNullOrBlank()) return "Sin registrar"
    if ('•' in valor || '*' in valor) return valor
    val digitos = valor.filter(Char::isDigit)
    if (digitos.length < 7) return "••••••"
    val local = if (digitos.startsWith("51") && digitos.length > 9) digitos.drop(2) else digitos
    return (if (local != digitos) "+51 " else "") + local.first() + "•• ••• " + local.takeLast(3)
}
