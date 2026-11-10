package com.example.impulsonica

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

data class PostulacionEnriquecidaItem(
    val postulacionId: Int,
    val empleoId: Int,
    val tituloVacante: String,
    val nombreEmpresa: String,
    val modalidad: String,
    val salario: String,
    val fechaPostulacion: String,
    val estado: String
)

class Candidato_postulacionesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PantallaMisPostulacionesConNombreReal()
            }
        }
    }
}

@Composable
fun PantallaMisPostulacionesConNombreReal() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs = context.getSharedPreferences("ImpulsoNicaPrefs", Context.MODE_PRIVATE)
    val candidatoId = prefs.getInt("candidato_id", prefs.getInt("usuario_sql_id", 1))

    var postulaciones by remember { mutableStateOf<List<PostulacionEnriquecidaItem>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    fun cargarPostulaciones() {
        scope.launch {
            cargando = true
            postulaciones = obtenerPostulacionesConNombreDeVacante(candidatoId)
            cargando = false
        }
    }

    LaunchedEffect(Unit) {
        cargarPostulaciones()
    }

    val azulOscuro = Color(0xFF0A2E5C)
    val fondoGris = Color(0xFFF4F6F9)

    Scaffold(
        containerColor = azulOscuro,
        bottomBar = {
            BarraInferiorCandidato5Iconos(context = context, itemActivo = 2)
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
                    .padding(vertical = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mis Postulaciones",
                    color = Color.White,
                    fontSize = 22.sp,
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
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vacantes a las que te has postulado (${postulaciones.size})",
                            color = azulOscuro,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "↻ Actualizar",
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { cargarPostulaciones() }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (cargando) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = azulOscuro)
                        }
                    } else if (postulaciones.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Aún no tienes postulaciones registradas.", color = Color.Gray, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        context.startActivity(Intent(context, Candidato_vacantesActivity::class.java))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = azulOscuro)
                                ) {
                                    Text("Explorar Vacantes", color = Color.White)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(postulaciones, key = { it.postulacionId }) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // Al tocar la postulación te lleva directo a la vacante a la que aplicaste
                                            val intent = Intent(context, Vista_vacanteActivity::class.java)
                                            intent.putExtra("empleo_id", item.empleoId)
                                            intent.putExtra("empleoid", item.empleoId)
                                            intent.putExtra("id", item.empleoId)
                                            context.startActivity(intent)
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.tituloVacante,
                                                    color = azulOscuro,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 16.sp
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = "🏢 ${item.nombreEmpresa} • ${item.modalidad}",
                                                    color = Color(0xFF475569),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            val colorEstado = when {
                                                item.estado.contains("Acept", ignoreCase = true) -> Color(0xFF16A34A)
                                                item.estado.contains("Rechaz", ignoreCase = true) -> Color(0xFFDC2626)
                                                else -> Color(0xFF0284C7)
                                            }

                                            Surface(
                                                color = colorEstado.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    text = item.estado,
                                                    color = colorEstado,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "📅 Fecha: ${item.fechaPostulacion}  •  ${item.salario}",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "Ver vacante ➔",
                                                color = Color(0xFF2563EB),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
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

private suspend fun obtenerPostulacionesConNombreDeVacante(candidatoId: Int): List<PostulacionEnriquecidaItem> =
    withContext(Dispatchers.IO) {
        val resultado = mutableListOf<PostulacionEnriquecidaItem>()
        val cacheEmpleos = mutableMapOf<Int, JSONObject>()

        try {
            val url = URL("http://20.51.194.8:8000/api/postulaciones/?candidatoid=$candidatoId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode in 200..299) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                val arr: JSONArray = if (raw.trim().startsWith("{")) {
                    JSONObject(raw).optJSONArray("results") ?: JSONArray()
                } else {
                    JSONArray(raw)
                }

                for (i in 0 until arr.length()) {
                    val p = arr.getJSONObject(i)
                    val postId = p.optInt("postulacionid", p.optInt("PostulacionId", p.optInt("id", i + 1)))
                    val empleoId = p.optInt("empleoid", p.optInt("EmpleoId", p.optInt("empleo_id", 0)))

                    // Intentar extraer el nombre si ya viene anidado, o consultarlo por su ID en /api/empleos/{empleoId}/
                    var titulo = p.optString("titulo_empleo", p.optString("titulo", p.optString("tituloempleo", "")))
                    var empresa = p.optString("nombre_empresa", p.optString("nombreempresa", ""))
                    var modalidad = p.optString("modalidad_nombre", "Presencial")
                    var salario = p.optString("salario", "")

                    if ((titulo.isBlank() || titulo == "null" || titulo.startsWith("Oferta Laboral #")) && empleoId > 0) {
                        val objEmpleo = cacheEmpleos.getOrPut(empleoId) {
                            consultarDetalleEmpleoPorId(empleoId) ?: JSONObject()
                        }
                        if (objEmpleo.length() > 0) {
                            titulo = extraerTituloRealVacante(objEmpleo)
                            empresa = extraerNombreEmpresaReal(objEmpleo)
                            modalidad = objEmpleo.optString("modalidad_nombre", objEmpleo.optString("modalidad", "Presencial"))
                            val salRaw = objEmpleo.optString("salario", objEmpleo.optString("salariomax", objEmpleo.optString("SalarioMax", "")))
                            salario = if (salRaw.isBlank() || salRaw == "null") "C$ A convenir" else "C$ $salRaw"
                        }
                    }

                    if (titulo.isBlank() || titulo == "null") {
                        titulo = "Especialista Corporativo (Vacante #$empleoId)"
                    }
                    if (empresa.isBlank() || empresa == "null") {
                        empresa = "Empresa Verificada"
                    }

                    val fechaRaw = p.optString("fechapostulacion", p.optString("FechaPostulacion", p.optString("fecha", "")))
                    val fechaLimpia = if (fechaRaw.isBlank() || fechaRaw == "null") "Reciente" else fechaRaw.take(10)
                    val estado = p.optString("estado_nombre", p.optString("estado", "Activo"))

                    resultado.add(
                        PostulacionEnriquecidaItem(
                            postulacionId = postId,
                            empleoId = empleoId,
                            tituloVacante = titulo,
                            nombreEmpresa = empresa,
                            modalidad = modalidad,
                            salario = if (salario.isBlank()) "Ver detalles" else salario,
                            fechaPostulacion = fechaLimpia,
                            estado = if (estado == "null" || estado.isBlank()) "Activo" else estado
                        )
                    )
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        resultado
    }

private fun consultarDetalleEmpleoPorId(empleoId: Int): JSONObject? {
    return try {
        val url = URL("http://20.51.194.8:8000/api/empleos/$empleoId/")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
        }
        val obj = if (conn.responseCode in 200..299) {
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(raw)
        } else null
        conn.disconnect()
        obj
    } catch (_: Exception) {
        null
    }
}