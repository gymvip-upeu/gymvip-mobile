package com.gymvip.mobile.features.journal

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.gymvip.mobile.presentation.permission.DevicePermissionStatus
import com.gymvip.mobile.presentation.permission.rememberCameraPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
actual fun rememberJournalImageService(onMessage: (String) -> Unit): JournalImageService {
    val context = LocalContext.current
    val message by rememberUpdatedState(onMessage)
    var callback by remember { mutableStateOf<((String?) -> Unit)?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoFile by remember { mutableStateOf<File?>(null) }
    var awaitingPermission by remember { mutableStateOf(false) }
    val permission = rememberCameraPermission()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        callback?.invoke(uri?.toString()); callback = null
        if (uri == null) message("Selección cancelada")
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        callback?.invoke(if (success) photoUri?.toString() else null); callback = null
        if (!success) { photoFile?.delete(); message("Foto cancelada") }
    }
    fun launchCamera() {
        try {
            val dir = File(context.cacheDir, "journal").apply { mkdirs() }
            dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 24 * 60 * 60 * 1000L }?.forEach { it.delete() }
            val file = File.createTempFile("photo-", ".jpg", dir)
            photoFile = file
            photoUri = FileProvider.getUriForFile(context, context.packageName + ".journal.files", file)
            camera.launch(photoUri!!)
        } catch (e: Exception) { callback?.invoke(null); callback = null; message("No hay una cámara disponible") }
    }
    LaunchedEffect(permission.status) {
        if (awaitingPermission && permission.status == DevicePermissionStatus.ENABLED) { awaitingPermission = false; launchCamera() }
        else if (awaitingPermission && permission.status == DevicePermissionStatus.DISABLED) { awaitingPermission = false; callback?.invoke(null); callback = null; message("Permiso de cámara denegado. Puedes usar una foto de la galería.") }
    }
    return object : JournalImageService {
        override suspend fun render(card: JournalShareCard): ByteArray = withContext(Dispatchers.Default) {
            val jobContext = currentCoroutineContext()
            renderJournal(context, card) { jobContext.ensureActive() }
        }
        override suspend fun save(bytes: ByteArray): String = withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < 29) return@withContext share(bytes)
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "GymVIP-${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GymVIP")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("Sin espacio disponible")
            try {
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("No se pudo abrir el archivo")
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
                "Imagen guardada en Pictures/GymVIP"
            } catch (e: Exception) { resolver.delete(uri, null, null); throw e }
        }
        override suspend fun share(bytes: ByteArray): String {
            val uri = withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "journal").apply { mkdirs() }
                dir.listFiles()?.filter { it.name.startsWith("GymVIP-") && System.currentTimeMillis() - it.lastModified() > 24 * 60 * 60 * 1000L }?.forEach { it.delete() }
                val file = File.createTempFile("GymVIP-", ".png", dir).apply { writeBytes(bytes) }
                FileProvider.getUriForFile(context, context.packageName + ".journal.files", file)
            }
            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newRawUri("Mi día GYM VIP", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir mi día").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            return if (Build.VERSION.SDK_INT < 29) "Elige una app para guardar o compartir la imagen" else "Elige dónde compartir tu imagen"
        }
        override fun pickPhoto(onResult: (String?) -> Unit) {
            callback = onResult
            try { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            catch (e: Exception) { callback = null; message("No se pudo abrir la galería") }
        }
        override fun takePhoto(onResult: (String?) -> Unit) {
            callback = onResult
            when (permission.status) {
                DevicePermissionStatus.ENABLED -> launchCamera()
                DevicePermissionStatus.CAN_REQUEST -> { awaitingPermission = true; permission.request() }
                else -> { callback = null; message("Activa el permiso de cámara en Ajustes o utiliza Galería.") }
            }
        }
    }
}

actual fun decodeJournalPreview(bytes: ByteArray): ImageBitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()

internal fun renderJournal(context: Context, card: JournalShareCard, checkActive: () -> Unit = {}): ByteArray {
    val width = 1080; val height = if (card.story) 1920 else 1080
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 1080f, height.toFloat(), intArrayOf(Color.rgb(11,19,32), Color.rgb(20,32,51)), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 1080f, height.toFloat(), paint); paint.shader = null
        card.photo?.let { raw ->
            checkActive()
            val uri = Uri.parse(raw)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            var sample = 1
            while (options.outWidth / sample > 2160 || options.outHeight / sample > 3840) sample *= 2
            val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
                ?: error("No se pudo leer la foto")
            val orientation = runCatching { context.contentResolver.openInputStream(uri)?.use { android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) } }.getOrNull() ?: 1
            val matrix = Matrix().apply {
                when (orientation) {
                    2 -> setScale(-1f, 1f)
                    3 -> setRotate(180f)
                    4 -> setScale(1f, -1f)
                    5 -> { setRotate(90f); postScale(-1f, 1f) }
                    6 -> setRotate(90f)
                    7 -> { setRotate(270f); postScale(-1f, 1f) }
                    8 -> setRotate(270f)
                }
            }
            val photo = if (orientation in 2..8) Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also { if (it !== decoded) decoded.recycle() } else decoded
            try {
                val it = photo
                val scale = maxOf(width.toFloat() / it.width, height.toFloat() / it.height)
                val w = it.width * scale; val h = it.height * scale
                canvas.drawBitmap(it, null, RectF((width-w)/2, (height-h)/2, (width+w)/2, (height+h)/2), paint)
            } finally { photo.recycle() }
            paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), intArrayOf(0xC90B1320.toInt(), 0xEB0B1320.toInt()), null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f,0f,1080f,height.toFloat(),paint); paint.shader = null
        }
        val blue = Color.rgb(0,141,218); val gold = Color.rgb(231,197,121)
        paint.color = blue; paint.alpha = 38; paint.strokeWidth = 5f
        if (card.template == "Bold") canvas.drawCircle(1040f, 380f, 380f, paint)
        else for (i in 0..8) canvas.drawLine(780f+i*60f, 0f, 100f+i*60f, height.toFloat(), paint)
        paint.alpha = 255
        // Identidad vectorial GYM VIP: monograma y aros, sin assets externos.
        fun text(value: String, x: Float, y: Float, size: Float, color: Int = Color.WHITE, bold: Boolean = false) {
            paint.color = color; paint.textSize = size; paint.typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
            canvas.drawText(value, x, y, paint)
        }
        if (card.showBrand) {
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 7f; paint.color = blue; canvas.drawCircle(126f, 136f, 58f, paint)
            paint.strokeWidth = 3f; paint.color = gold; canvas.drawCircle(126f,136f,49f,paint); paint.style = Paint.Style.FILL
            text("GV", 86f, 151f, 42f, Color.WHITE, true); text("GYM VIP", 212f, 148f, 42f, Color.WHITE, true)
        }
        if (card.showDate) text(card.date, 72f, 260f, 32f, gold)
        text(if (card.template == "Bold") "HOY ME SUPERÉ" else "MI DÍA DE ENTRENO", 72f, 342f, if (card.story) 56f else 48f, Color.WHITE, true)
        var y = if (card.story) 520f else 420f
        var fontSize = if (card.story) 40f else 30f
        fun wrappedLines(): List<Triple<String, Boolean, Boolean>> {
            paint.textSize = fontSize
            paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            return card.lines.filter { it.isNotBlank() }.flatMap { line ->
                checkActive()
                var remaining = line.replace(Regex("\\s+"), " ").trim()
                val result = mutableListOf<String>()
                while (remaining.isNotEmpty()) {
                    var count = paint.breakText(remaining, true, 920f, null)
                    require(count > 0) { "No se pudo ajustar el texto" }
                    if (count < remaining.length) {
                        val split = remaining.lastIndexOf(' ', count - 1)
                        if (split > 0) count = split
                    }
                    result.add(remaining.take(count))
                    remaining = remaining.drop(count).trimStart()
                }
                result.mapIndexed { index, text -> Triple(text, line.startsWith("Racha") || line.startsWith("Mejor"), index == result.lastIndex) }
            }
        }
        var lines = wrappedLines()
        fun requiredHeight() = lines.size * fontSize * 1.5f + if (card.template == "Estadísticas") lines.count { it.third } * 12f else 0f
        while (requiredHeight() > height - y - 110 && fontSize > 16f) {
            fontSize -= 1f; lines = wrappedLines()
        }
        require(requiredHeight() <= height - y - 110) { "Reduce el contenido de la tarjeta para que todos los datos sean legibles." }
        for ((line, highlighted, lastInGroup) in lines) {
            checkActive()
            text(line, 72f, y, fontSize, if (highlighted) gold else Color.WHITE)
            y += fontSize * 1.5f
            if (card.template == "Estadísticas" && lastInGroup) {
                paint.color = blue; canvas.drawRect(72f,y-14f,1008f,y-11f,paint); y += 12f
            }
        }
        checkActive()
        text("MI CONSTANCIA CUENTA",72f,height-62f,26f,gold,true)
        return ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it); it.toByteArray() }
    } finally { bitmap.recycle() }
}
