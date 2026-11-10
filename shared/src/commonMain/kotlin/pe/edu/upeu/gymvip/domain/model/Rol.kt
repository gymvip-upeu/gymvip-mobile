package pe.edu.upeu.gymvip.domain.model

enum class Rol(val etiqueta: String) {
    ADMIN("Administrador"), RECEPCION("Recepción"), CLIENTE("Socio");
    companion object {
        fun reconocidos(valores: Iterable<String>): Set<Rol> = valores.mapNotNull { valor ->
            entries.firstOrNull { it.name == valor.removePrefix("ROLE_") }
        }.toSet()
    }
}

data class UsuarioPersonal(val id: String, val correo: String, val roles: Set<Rol>) {
    val esAdministrador get() = Rol.ADMIN in roles
    val etiqueta get() = if (esAdministrador) Rol.ADMIN.etiqueta else Rol.RECEPCION.etiqueta
}
