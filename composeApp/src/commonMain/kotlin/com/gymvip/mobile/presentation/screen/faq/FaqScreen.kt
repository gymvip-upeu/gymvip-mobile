package com.gymvip.mobile.presentation.screen.faq

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.domain.model.Faq
import com.gymvip.mobile.domain.usecase.GetFaqsUseCase
import com.gymvip.mobile.presentation.component.ErrorRetryView
import com.gymvip.mobile.presentation.component.LoadingView

@Composable
fun FaqScreen(
    getFaqsUseCase: GetFaqsUseCase,
    receptionWhatsAppNumber: String?,
    onBack: () -> Unit
) {
    val faqs by getFaqsUseCase.observe().collectAsState(initial = emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    LaunchedEffect(getFaqsUseCase, attempt) {
        loading = true
        loadFailed = getFaqsUseCase.refresh().isFailure
        loading = false
    }

    val categories = remember(faqs) { faqs.map { it.category }.filter { it.isNotBlank() }.distinct() }
    val category = selectedCategory?.takeIf { it in categories }
    val results = remember(faqs, query, category) { filterFaqs(faqs, query, category) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
            }
            Text("Ayuda y atención", style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expandedId = null
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            label = { Text("Buscar preguntas frecuentes") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = {
                        query = ""
                        expandedId = null
                    }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Borrar búsqueda")
                    }
                }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() })
        )

        if (categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = category == null,
                        onClick = {
                            selectedCategory = null
                            expandedId = null
                            focus.clearFocus()
                        },
                        label = { Text("Todas") }
                    )
                }
                items(categories, key = { it }) { name ->
                    FilterChip(
                        selected = category == name,
                        onClick = {
                            selectedCategory = name
                            expandedId = null
                            focus.clearFocus()
                        },
                        label = { Text(name) }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when {
                loading && faqs.isEmpty() -> item { LoadingView("Cargando preguntas…") }
                loadFailed && faqs.isEmpty() -> item {
                    ErrorRetryView(
                        message = "No pudimos cargar las preguntas frecuentes.",
                        onRetry = { attempt++ }
                    )
                }
                results.isEmpty() -> item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (faqs.isEmpty()) "Aún no hay preguntas disponibles" else "No encontramos resultados",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (faqs.isNotEmpty()) {
                            Text(
                                "Prueba otra palabra o revisa todas las categorías.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = {
                                query = ""
                                selectedCategory = null
                                expandedId = null
                                focus.clearFocus()
                            }) { Text("Ver todas las preguntas") }
                        }
                    }
                }
                else -> items(results, key = { it.id }) { faq ->
                    FaqAccordionCard(
                        faq = faq,
                        expanded = expandedId == faq.id,
                        onClick = {
                            focus.clearFocus()
                            expandedId = if (expandedId == faq.id) null else faq.id
                        }
                    )
                }
            }
        }

        ReceptionContact(receptionWhatsAppNumber)
    }
}

@Composable
private fun FaqAccordionCard(faq: Faq, expanded: Boolean, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics {
            stateDescription = if (expanded) "Respuesta visible" else "Respuesta cerrada"
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (faq.category.isNotBlank()) {
                Text(faq.category, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(visible = expanded) {
                Text(
                    text = faq.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReceptionContact(whatsAppNumber: String?) {
    val uriHandler = LocalUriHandler.current
    val focus = LocalFocusManager.current
    val whatsAppUrl = remember(whatsAppNumber) { receptionWhatsAppUrl(whatsAppNumber) }
    var openFailed by remember(whatsAppUrl) { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("¿No encontraste tu respuesta?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (whatsAppUrl == null) {
                Text(
                    "WhatsApp de Recepción aún no está disponible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Button(onClick = {
                    focus.clearFocus()
                    openFailed = runCatching { uriHandler.openUri(whatsAppUrl) }.isFailure
                }) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null)
                    Text("WhatsApp de Recepción", modifier = Modifier.padding(start = 8.dp))
                }
                if (openFailed) {
                    Text("No pudimos abrir WhatsApp. Inténtalo de nuevo.", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

internal fun filterFaqs(faqs: List<Faq>, query: String, category: String?): List<Faq> {
    val terms = query.searchText().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return faqs.filter { faq ->
        if (category != null && faq.category != category) return@filter false
        if (terms.isEmpty()) return@filter true
        val searchable = "${faq.question} ${faq.answer} ${faq.category}".searchText()
        terms.all(searchable::contains)
    }
}

internal fun receptionWhatsAppUrl(number: String?): String? {
    val digits = number?.filterNot { it in "+ -()" } ?: return null
    return if (digits.matches(Regex("[1-9][0-9]{7,14}"))) "https://wa.me/$digits" else null
}

private fun String.searchText(): String =
    lowercase().replace('á', 'a').replace('é', 'e').replace('í', 'i')
        .replace('ó', 'o').replace('ú', 'u').replace('ü', 'u')
