package com.gymvip.mobile.features.journal

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.datetime.LocalDate
import com.gymvip.mobile.presentation.component.GymVipLogo

data class JournalShareCard(val date: String, val lines: List<String>, val template: String, val story: Boolean, val photo: String?, val showDate: Boolean = true, val showBrand: Boolean = true)
internal data class JournalRenderedCard(val card: JournalShareCard, val bytes: ByteArray)
private data class JournalPreview(val bytes: ByteArray, val bitmap: ImageBitmap)
internal fun journalExportBytes(request: JournalShareCard, rendered: JournalRenderedCard?): ByteArray? =
    rendered?.takeIf { it.card == request && it.bytes.isNotEmpty() }?.bytes
interface JournalImageService {
    suspend fun render(card: JournalShareCard): ByteArray?
    suspend fun save(bytes: ByteArray): String
    suspend fun share(bytes: ByteArray): String
    fun pickPhoto(onResult: (String?) -> Unit)
    fun takePhoto(onResult: (String?) -> Unit)
}
@Composable
expect fun rememberJournalImageService(onMessage: (String) -> Unit): JournalImageService
expect fun decodeJournalPreview(bytes: ByteArray): ImageBitmap?

/** Lista blanca: ningún campo de identidad/contacto/membresía forma parte del modelo compartible. */
fun journalShareLines(day: JournalDay, days: List<JournalDay>, show: Set<String>, name: String): List<String> = buildList {
    if ("name" in show && name.isNotBlank()) add(name.take(60))
    if ("time" in show) add("${day.seconds / 60} min de entrenamiento")
    if ("type" in show) { add((day.entries.map { it.type } + if (day.sessions.isNotEmpty()) listOf("Rutina") else emptyList()).distinct().joinToString(" · ")); day.entries.flatMap { it.muscles }.distinct().joinToString(" · ").takeIf { it.isNotBlank() }?.let { add(it) } }
    if ("volume" in show) { add("Volumen: ${day.volume.toLong()} kg"); day.entries.flatMap { it.exercises }.map { it.name }.distinct().take(3).forEach { add(it) } }
    if ("feeling" in show && day.entries.isNotEmpty()) { add("Energía ${day.entries.map { it.energy }.average().toInt()}/5 · Esfuerzo ${day.entries.map { it.effort }.average().toInt()}/5"); day.entries.last().howIFelt.takeIf { it.isNotBlank() }?.let { add(it.take(120)) } }
    if ("streak" in show) {
        val count = journalStreak(days.filter { it.date <= day.date }.map { LocalDate.parse(it.date) }.toSet(), LocalDate.parse(day.date))
        add("Racha: $count ${if (count == 1) "día" else "días"}")
    }
    if ("record" in show) {
        val past = days.filter { it.date < day.date }.flatMap { it.entries }
        val record = day.entries.flatMap { it.exercises }.firstOrNull { e ->
            val previous = strengthTimeline(past, e.name).maxOfOrNull { it.estimatedMax }
            previous != null && estimatedOneRepMax(e.kg, e.reps) > previous
        }
        record?.let { add("Mejor registro personal: ${it.name}") }
    }
}

@Composable
internal fun JournalShareScreen(day: JournalDay, days: List<JournalDay>, memberName: String, onMessage: (String) -> Unit) {
    val service = rememberJournalImageService(onMessage)
    val scope = rememberCoroutineScope()
    var shown by remember { mutableStateOf(setOf("date", "brand", "time", "type", "volume", "feeling", "streak", "record")) }
    var story by remember { mutableStateOf(true) }
    var template by remember { mutableStateOf("Minimal") }
    var photo by remember { mutableStateOf<String?>(null) }
    var rendered by remember { mutableStateOf<JournalRenderedCard?>(null) }
    var busy by remember { mutableStateOf(false) }
    var renderError by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    val card = JournalShareCard(day.date, journalShareLines(day, days, shown, memberName), template, story, photo, "date" in shown, "brand" in shown)
    LaunchedEffect(card, retry) {
        rendered = null; renderError = false
        try {
            val bytes = service.render(card)
            ensureActive()
            rendered = bytes?.takeIf { it.isNotEmpty() }?.let { JournalRenderedCard(card, it) }
            renderError = rendered == null
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { renderError = true }
    }
    val bytes = journalExportBytes(card, rendered)
    var previewError by remember(bytes) { mutableStateOf(false) }
    val decoded by produceState<JournalPreview?>(null, bytes) {
        value = null
        try {
            value = bytes?.let { image -> withContext(Dispatchers.Default) { decodeJournalPreview(image)?.let { JournalPreview(image, it) } } }
            previewError = bytes != null && value == null
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { previewError = true }
    }
    val preview = decoded?.takeIf { it.bytes === bytes }?.bitmap
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GymVipLogo(size = 48.dp)
        Text("Tu día, a tu manera", style = MaterialTheme.typography.headlineSmall)
        Text("Revisa el contenido antes de compartir. El nombre está oculto por defecto.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(story, { story = true }, label = { Text("Historia 9:16") }); FilterChip(!story, { story = false }, label = { Text("Publicación 1:1") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Minimal", "Bold", "Estadísticas").forEach { label -> FilterChip(template == label, { template = label }, label = { Text(label) }) } }
        Box(Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
            if (preview != null) Image(preview, contentDescription = "Vista previa exacta de la imagen para compartir", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            else if (renderError || previewError) Column {
                Text("No se pudo generar la imagen. Prueba el fondo de marca o reduce el contenido.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { retry++ }) { Text("Reintentar") }
            }
            else CircularProgressIndicator()
        }
        listOf("date" to "Fecha", "brand" to "Logo GYM VIP", "time" to "Tiempo entrenado", "type" to "Tipo y músculos", "volume" to "Ejercicios y volumen", "record" to "Récord personal", "feeling" to "Cómo me sentí", "streak" to "Racha", "name" to "Mi nombre").forEach { (key, label) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, modifier = Modifier.padding(top = 12.dp)); Switch(key in shown, { checked -> shown = if (checked) shown + key else shown - key }) }
        }
        OutlinedButton(onClick = { photo = null }, modifier = Modifier.fillMaxWidth()) { Text("Usar fondo de marca") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { service.pickPhoto { photo = it ?: photo } }, modifier = Modifier.weight(1f)) { Text("Galería") }
            OutlinedButton(onClick = { service.takePhoto { photo = it ?: photo } }, modifier = Modifier.weight(1f)) { Text("Cámara") }
        }
        Text("Solo se exporta lo que ves en la tarjeta. No se añaden correo, documento, teléfono, QR ni información de pagos.", style = MaterialTheme.typography.bodySmall)
        Button(enabled = bytes != null && preview != null && !busy, onClick = { val image = bytes ?: return@Button; busy = true; scope.launch { try { onMessage(service.save(image)) } catch (e: CancellationException) { throw e } catch (e: Exception) { onMessage("No se pudo guardar la imagen") } finally { busy = false } } }, modifier = Modifier.fillMaxWidth()) { Text("Guardar imagen") }
        OutlinedButton(enabled = bytes != null && preview != null && !busy, onClick = { val image = bytes ?: return@OutlinedButton; busy = true; scope.launch { try { onMessage(service.share(image)) } catch (e: CancellationException) { throw e } catch (e: Exception) { onMessage("No se pudo abrir el menú para compartir") } finally { busy = false } } }, modifier = Modifier.fillMaxWidth()) { Text("Compartir") }
        Spacer(Modifier.height(24.dp))
    }
}
