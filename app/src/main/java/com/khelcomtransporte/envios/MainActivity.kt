package com.khelcomtransporte.envios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.khelcomtransporte.envios.ui.theme.KhelcomTransporteTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    // Estados bilingües EXACTOS
    private val estados = listOf(
        "En preparación (En préparation)",
        "En espera (En attente)",
        "En tránsito (En transit)",
        "Entregado (Livré)",
        "Recogido (Ramassé)",
        "Cancelado (Annulé)",
        "Devuelto (Retourné)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KhelcomTransporteTheme {
                var logged by remember { mutableStateOf(Prefs.hasCreds(this)) }

                if (!logged) {
                    LoginScreen(
                        defaultUrl = Prefs.getBaseUrl(this) ?: "https://khelcomtransporte.com",
                        defaultUser = Prefs.getUser(this) ?: "",
                        onSave = { url, user, pass ->
                            // normalizamos: trim y quitamos espacios de la app password
                            val urlSan = url.trim().trimEnd('/')
                            val userSan = user.trim()
                            val passSan = pass.replace("\\s".toRegex(), "")
                            Prefs.save(this, urlSan, userSan, passSan)
                            ApiClient.configure(urlSan, userSan, passSan)
                            logged = true
                        }
                    )
                } else {
                    // Configuramos cliente con lo guardado
                    ApiClient.configure(
                        Prefs.getBaseUrl(this)!!,
                        Prefs.getUser(this)!!,
                        Prefs.getPass(this)!!
                    )
                    HomeScreen(
                        estados = estados,
                        onLogout = {
                            Prefs.clear(this)
                            logged = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    defaultUrl: String,
    defaultUser: String,
    onSave: (String, String, String) -> Unit
) {
    var url by remember { mutableStateOf(defaultUrl) }
    var user by remember { mutableStateOf(defaultUser) }
    var pass by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Khelcom • Login") }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = url, onValueChange = { url = it },
                label = { Text("URL del sitio (https://...)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = user, onValueChange = { user = it },
                label = { Text("Usuario WordPress") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pass, onValueChange = { pass = it },
                label = { Text("Application Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSave(url, user, pass) },
                enabled = url.isNotBlank() && user.isNotBlank() && pass.isNotBlank()
            ) { Text("Guardar y entrar") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    estados: List<String>,
    onLogout: () -> Unit
) {
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var envios by remember { mutableStateOf<List<Shipment>>(emptyList()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Chips de filtro: "Todos" + estados
    val filtros = remember(estados) { listOf("Todos") + estados }
    var filtroSel by remember { mutableStateOf("Todos") }

    fun cargar() {
        cargando = true; error = null
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                try { ApiClient.getShipmentsParsed() }
                catch (e: Exception) { error = e.message; emptyList() }
            }
            envios = res
            cargando = false
        }
    }

    fun actualizar(id: Long, nuevoEstado: String) {
        cargando = true; error = null
        scope.launch {
            withContext(Dispatchers.IO) {
                try { ApiClient.updateEnvio(id, nuevoEstado) }
                catch (_: Exception) { /* feedback abajo */ }
            }
            cargar()
            snackbarHostState.showSnackbar("Estado actualizado a \"$nuevoEstado\"")
        }
    }

    LaunchedEffect(Unit) { cargar() }

    // Aplicar filtro en memoria
    val enviosFiltrados = remember(envios, filtroSel) {
        if (filtroSel == "Todos") envios
        else envios.filter { (it.status ?: "").normalizeEstado() == filtroSel.normalizeEstado() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Khelcom • Conductor") },
                actions = { TextButton(onClick = onLogout) { Text("Cerrar sesión") } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // CHIPS DE FILTRO (Row con scroll horizontal) — sin LazyRow
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filtros.forEach { f ->
                    val selected = f == filtroSel
                    if (selected) {
                        Button(onClick = { /* ya seleccionado */ }, enabled = false) { Text(f) }
                    } else {
                        OutlinedButton(onClick = { filtroSel = f }) { Text(f) }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { cargar() }, enabled = !cargando) { Text("Cargar envíos") }
                Text(
                    text = "Mostrando: ${enviosFiltrados.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            when {
                cargando -> CircularProgressIndicator()
                error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(enviosFiltrados) { sh ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        sh.title ?: "Sin título",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(6.dp))

                                    EstadoChipDestacado(sh.status)

                                    if (!sh.dest.isNullOrBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text("Destino: ${sh.dest}")
                                    }

                                    Spacer(Modifier.height(10.dp))
                                    Text("Para actualizar el estado pulsa una de las opciones:")
                                    Spacer(Modifier.height(6.dp))

                                    val actualNorm = (sh.status ?: "").normalizeEstado()
                                    val opciones = estados.filter { it.normalizeEstado() != actualNorm }

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        opciones.forEach { e ->
                                            OutlinedButton(
                                                onClick = { actualizar(sh.id, e) },
                                                enabled = !cargando
                                            ) { Text(e) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Badge grande para resaltar el estado actual */
@Composable
fun EstadoChipDestacado(estado: String?) {
    val label = estado?.ifBlank { "—" } ?: "—"
    val esEntregado = label.contains("Entregado", true) || label.contains("Livré", true)
    val bg = if (esEntregado) Color(0xFFDFF6E5) else Color(0xFFDCEFFF)
    val fg = if (esEntregado) Color(0xFF1B5E20) else Color(0xFF1565C0)

    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = "Estado:", fontWeight = FontWeight.SemiBold, color = fg)
        Spacer(Modifier.width(8.dp))
        Text(text = label, fontWeight = FontWeight.Bold, color = fg)
    }
}

/** Normaliza estado para comparar sin problemas de espacios/mayúsculas */
private fun String.normalizeEstado(): String =
    trim().lowercase().replace("\\s+".toRegex(), " ")
