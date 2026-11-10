package com.example.impulsonica

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.ceil

class Candidato_vacantesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PantallaBusquedaVacantes10En10()
            }
        }
    }
}

@Composable
fun PantallaBusquedaVacantes10En10() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var textoBusqueda by remember { mutableStateOf("") }
    var paginaActual by remember { mutableStateOf(1) }
    var totalPaginas by remember { mutableStateOf(1) }
    var totalOfertas by remember { mutableStateOf(0) }
    var listaVacantes by remember { mutableStateOf<List<OfertaCandidatoModel>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    fun cargarPagina(pagina: Int) {
        scope.launch {
            cargando = true
            val res = consultarVacantesPaginadas10En10(pagina, textoBusqueda.trim())
            listaVacantes = res.first
            totalOfertas = res.second
            totalPaginas = ceil(totalOfertas.toDouble() / 10.0).toInt().coerceAtLeast(1)
            paginaActual = pagina
            cargando = false
        }
    }

    LaunchedEffect(Unit) {
        cargarPagina(1)
    }

    val azulOscuro = Color(0xFF0A2E5C)
    val fondoGris = Color(0xFFF4F6F9)

    Scaffold(
        containerColor = azulOscuro,
        bottomBar = {
            // Misma barra inferior unificada que Home, Postulaciones, Notificaciones y Perfil
            BarraInferiorCandidato5Iconos(context = context, itemActivo = 1)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Vacantes",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = fondoGris,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    // Buscador con texto negro nítido
                    OutlinedTextField(
                        value = textoBusqueda,
                        onValueChange = { textoBusqueda = it },
                        placeholder = { Text("🔍 Buscar cargo o empresa...", color = Color(0xFF64748B)) },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF0066CC),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { cargarPagina(1) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066CC)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("Buscar Vacantes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Página $paginaActual de $totalPaginas ($totalOfertas ofertas disponibles)",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (cargando) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = azulOscuro)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(listaVacantes, key = { it.id }) { vac ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val intent = Intent(context, Vista_vacanteActivity::class.java)
                                            intent.putExtra("empleo_id", vac.id)
                                            intent.putExtra("empleoid", vac.id)
                                            intent.putExtra("id", vac.id)
                                            context.startActivity(intent)
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = vac.titulo,
                                            color = azulOscuro,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Empresa: ${vac.empresa}",
                                            color = Color(0xFF475569),
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = vac.salario,
                                                color = Color(0xFF16A34A),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${vac.modalidad} • Ver más →",
                                                color = Color(0xFF0066CC),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Controles de Paginación de 10 en 10 al final de la lista
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { if (paginaActual > 1) cargarPagina(paginaActual - 1) },
                                        enabled = paginaActual > 1,
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("« Anterior", fontWeight = FontWeight.Bold)
                                    }

                                    Text(
                                        text = "Pág. $paginaActual / $totalPaginas",
                                        color = azulOscuro,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )

                                    OutlinedButton(
                                        onClick = { if (paginaActual < totalPaginas) cargarPagina(paginaActual + 1) },
                                        enabled = paginaActual < totalPaginas,
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("Siguiente »", fontWeight = FontWeight.Bold)
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

private suspend fun consultarVacantesPaginadas10En10(
    pagina: Int,
    busqueda: String
): Pair<List<OfertaCandidatoModel>, Int> = withContext(Dispatchers.IO) {
    val lista = mutableListOf<OfertaCandidatoModel>()
    var total = 0
    try {
        val qParam = if (busqueda.isNotBlank()) "&search=${URLEncoder.encode(busqueda, "UTF-8")}" else ""
        val url = URL("http://20.51.194.8:8000/api/empleos/?page=$pagina&page_size=10$qParam")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 7000
        }
        if (conn.responseCode in 200..299) {
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = if (raw.trim().startsWith("{")) {
                val obj = JSONObject(raw)
                total = obj.optInt("count", 0)
                obj.optJSONArray("results") ?: JSONArray()
            } else {
                val a = JSONArray(raw)
                total = a.length()
                a
            }
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val salRaw = o.optString("salario", o.optString("salariomax", o.optString("SalarioMax", "")))
                lista.add(
                    OfertaCandidatoModel(
                        id = o.optInt("empleoid", o.optInt("EmpleoId", o.optInt("id", i + 1))),
                        titulo = extraerTituloRealVacante(o),
                        empresa = extraerNombreEmpresaReal(o),
                        empresaId = o.optInt("empresaid", 1),
                        ubicacion = o.optString("municipio_nombre", "Nicaragua"),
                        modalidad = o.optString("modalidad_nombre", "Presencial"),
                        salario = if (salRaw.isBlank() || salRaw == "null") "C$ A convenir" else "C$ $salRaw"
                    )
                )
            }
        }
        conn.disconnect()
    } catch (_: Exception) {}
    Pair(lista, total)
}