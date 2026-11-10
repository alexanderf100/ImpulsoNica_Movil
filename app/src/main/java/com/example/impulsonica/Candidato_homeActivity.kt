package com.example.impulsonica

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

data class OfertaCandidatoModel(
    val id: Int,
    val titulo: String,
    val empresa: String,
    val empresaId: Int,
    val ubicacion: String,
    val modalidad: String,
    val salario: String
)

data class ResumenHomeCandidato(
    val totalOfertasActivas: Int,
    val misPostulacionesCount: Int,
    val alertasCount: Int,
    val destacadas: List<OfertaCandidatoModel>
)

class Candidato_homeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PantallaHomeCandidatoEjecutiva()
            }
        }
    }
}

@Composable
fun PantallaHomeCandidatoEjecutiva() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs = context.getSharedPreferences("ImpulsoNicaPrefs", Context.MODE_PRIVATE)
    val candidatoId = prefs.getInt("candidato_id", prefs.getInt("usuario_sql_id", 1))
    val nombreCandidato = prefs.getString("nombre_usuario", "Candidato") ?: "Candidato"

    var cargando by remember { mutableStateOf(true) }
    var totalOfertas by remember { mutableStateOf(702) }
    var totalMisPostulaciones by remember { mutableStateOf(0) }
    var totalAlertas by remember { mutableStateOf(0) }
    var destacadas by remember { mutableStateOf<List<OfertaCandidatoModel>>(emptyList()) }

    fun cargarResumenHome() {
        scope.launch {
            cargando = true
            val res = obtenerResumenHomeCandidato(candidatoId)
            totalOfertas = res.totalOfertasActivas
            totalMisPostulaciones = res.misPostulacionesCount
            totalAlertas = res.alertasCount
            destacadas = res.destacadas
            cargando = false
        }
    }

    LaunchedEffect(Unit) {
        cargarResumenHome()
    }

    val azulOscuro = Color(0xFF0A2E5C)
    val fondoApp = Color(0xFFF1F5F9)

    Scaffold(
        containerColor = azulOscuro,
        bottomBar = {
            BarraInferiorCandidato5Iconos(context = context, itemActivo = 0)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Cabecera de Bienvenida del Home
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hola, $nombreCandidato",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Bienvenido a tu panel principal de ImpulsoNica",
                        color = Color(0xFF93C5FD),
                        fontSize = 13.sp
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clickable {
                            context.startActivity(Intent(context, Candidato_perfilActivity::class.java))
                        },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IconoVectorialDelineado(
                            tipo = "profile",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = fondoApp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // 1. Indicadores Rápidos del Candidato
                    item {
                        Text(
                            text = "Resumen de tu Actividad",
                            color = azulOscuro,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        context.startActivity(Intent(context, Candidato_vacantesActivity::class.java))
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Vacantes", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("$totalOfertas", color = azulOscuro, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("Explorar ➔", color = Color(0xFF2563EB), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        context.startActivity(Intent(context, Candidato_postulacionesActivity::class.java))
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Postulaciones", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("$totalMisPostulaciones", color = Color(0xFF16A34A), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("Ver estado ➔", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        context.startActivity(Intent(context, Candidato_notificacionesActivity::class.java))
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Alertas", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("$totalAlertas", color = Color(0xFFF59E0B), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("Revisar ➔", color = Color(0xFFF59E0B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 2. Tarjeta de Acción Rápida para Currículum y Búsqueda
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2E5C))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Impulsa tu Perfil Profesional",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Mantén actualizadas tus habilidades y experiencia en tu CV para destacar ante las empresas reclutadoras.",
                                    color = Color(0xFFBFDBFE),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            context.startActivity(Intent(context, Candidato_vacantesActivity::class.java))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Buscar Vacantes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            context.startActivity(Intent(context, Candidato_perfilActivity::class.java))
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Text("Mi Currículum", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Vacantes Recientes Recomendadas (Vista Previa de 3 ofertas)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Oportunidades Recientes para Ti",
                                color = azulOscuro,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Ver todas ($totalOfertas) ➔",
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable {
                                    context.startActivity(Intent(context, Candidato_vacantesActivity::class.java))
                                }
                            )
                        }
                    }

                    if (cargando) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = azulOscuro)
                            }
                        }
                    } else {
                        items(destacadas, key = { it.id }) { oferta ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = Intent(context, Vista_vacanteActivity::class.java)
                                        intent.putExtra("empleo_id", oferta.id)
                                        intent.putExtra("empleoid", oferta.id)
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
                                                text = oferta.titulo,
                                                color = azulOscuro,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Empresa: ${oferta.empresa}",
                                                color = Color(0xFF475569),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Surface(
                                            color = Color(0xFF2563EB).copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = oferta.modalidad,
                                                color = Color(0xFF2563EB),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                                            text = oferta.salario,
                                            color = Color(0xFF16A34A),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${oferta.ubicacion} • Ver más →",
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

@Composable
fun BarraInferiorCandidato5Iconos(context: Context, itemActivo: Int) {
    val azulBarra = Color(0xFF0A2E5C)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF4F6F9))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(azulBarra),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconos = listOf(
                Triple(0, "home", Candidato_homeActivity::class.java),
                Triple(1, "search", Candidato_vacantesActivity::class.java),
                Triple(2, "postulaciones", Candidato_postulacionesActivity::class.java),
                Triple(3, "bell", Candidato_notificacionesActivity::class.java),
                Triple(4, "profile", Candidato_perfilActivity::class.java)
            )
            iconos.forEach { (idx, tipoIcono, activityClass) ->
                val activo = itemActivo == idx
                IconButton(
                    onClick = {
                        if (!activo) {
                            context.startActivity(Intent(context, activityClass))
                        }
                    }
                ) {
                    IconoVectorialDelineado(
                        tipo = tipoIcono,
                        tint = if (activo) Color(0xFF60A5FA) else Color.White
                    )
                }
            }
        }
    }
}

fun extraerTituloRealVacante(o: JSONObject): String {
    val camposTitulo = listOf(
        "titulo", "Titulo", "tituloempleo", "TituloEmpleo",
        "cargo", "Cargo", "nombre_vacante", "puesto", "nombreempleo"
    )
    for (c in camposTitulo) {
        val v = o.optString(c, "")
        if (v.isNotBlank() && v != "null") return v
    }
    return "Especialista Profesional"
}

fun extraerNombreEmpresaReal(o: JSONObject): String {
    val posiblesCampos = listOf("nombreempresa", "nombre_empresa", "empresa_nombre", "NombreEmpresa", "empresa")
    for (campo in posiblesCampos) {
        val valor = o.opt(campo)
        if (valor is JSONObject) {
            val nomAnidado = valor.optString("nombreempresa", valor.optString("NombreEmpresa", ""))
            if (nomAnidado.isNotBlank()) return nomAnidado
        } else if (valor is String && valor.isNotBlank() && !valor.all { it.isDigit() }) {
            return valor
        }
    }
    val empObj = o.optJSONObject("empresaid")
    if (empObj != null) {
        val nom = empObj.optString("nombreempresa", empObj.optString("NombreEmpresa", ""))
        if (nom.isNotBlank()) return nom
    }
    val empId = o.optInt("empresaid", o.optInt("EmpresaId", 1))
    return "Empresa #$empId"
}

private suspend fun obtenerResumenHomeCandidato(candidatoId: Int): ResumenHomeCandidato =
    withContext(Dispatchers.IO) {
        val destacadas = mutableListOf<OfertaCandidatoModel>()
        var totalOfertas = 702
        var misPostCount = 0
        var alertasCount = 0

        // 1. Obtener las 3 ofertas más recientes
        try {
            val url = URL("http://20.51.194.8:8000/api/empleos/?page=1&page_size=4")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
            }
            if (conn.responseCode in 200..299) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                val arr: JSONArray = if (raw.trim().startsWith("{")) {
                    val obj = JSONObject(raw)
                    totalOfertas = obj.optInt("count", 702)
                    obj.optJSONArray("results") ?: JSONArray()
                } else {
                    JSONArray(raw)
                }
                for (i in 0 until minOf(arr.length(), 4)) {
                    val o = arr.getJSONObject(i)
                    val salarioRaw = o.optString(
                        "salario",
                        o.optString("salariomax", o.optString("SalarioMax", o.optString("salariomin", "")))
                    )
                    destacadas.add(
                        OfertaCandidatoModel(
                            id = o.optInt("empleoid", o.optInt("EmpleoId", o.optInt("id", i + 1))),
                            titulo = extraerTituloRealVacante(o),
                            empresa = extraerNombreEmpresaReal(o),
                            empresaId = o.optInt("empresaid", 1),
                            ubicacion = o.optString("municipio_nombre", o.optString("ubicacion", "Nicaragua")),
                            modalidad = o.optString("modalidad_nombre", o.optString("modalidad", "Presencial")),
                            salario = if (salarioRaw.isBlank() || salarioRaw == "null") "C$ A convenir" else "C$ $salarioRaw"
                        )
                    )
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}

        // 2. Contar postulaciones del candidato
        try {
            val urlP = URL("http://20.51.194.8:8000/api/postulaciones/?candidatoid=$candidatoId")
            val connP = (urlP.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }
            if (connP.responseCode in 200..299) {
                val rawP = connP.inputStream.bufferedReader().use { it.readText() }
                misPostCount = if (rawP.trim().startsWith("{")) {
                    JSONObject(rawP).optInt("count", 0)
                } else {
                    JSONArray(rawP).length()
                }
            }
            connP.disconnect()
        } catch (_: Exception) {}

        // 3. Contar notificaciones del candidato
        try {
            val urlN = URL("http://20.51.194.8:8000/api/v1/notificaciones/?usuario_sql_id=$candidatoId&rol=candidato")
            val connN = (urlN.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }
            if (connN.responseCode in 200..299) {
                val rawN = connN.inputStream.bufferedReader().use { it.readText() }
                alertasCount = JSONArray(rawN).length()
            }
            connN.disconnect()
        } catch (_: Exception) {}

        ResumenHomeCandidato(
            totalOfertasActivas = totalOfertas,
            misPostulacionesCount = misPostCount,
            alertasCount = alertasCount,
            destacadas = destacadas
        )
    }