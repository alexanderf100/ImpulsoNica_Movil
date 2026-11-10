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

data class VacanteEmpresaHomeItem(
    val id: Int,
    val titulo: String,
    val modalidad: String,
    val ubicacion: String,
    val salario: String
)

class Empresa_homeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PantallaHomeEmpresaSeparada()
            }
        }
    }
}

@Composable
fun PantallaHomeEmpresaSeparada() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs = context.getSharedPreferences("ImpulsoNicaPrefs", Context.MODE_PRIVATE)
    val empresaId = prefs.getInt("empresa_id", prefs.getInt("usuario_sql_id", 2))
    val token = prefs.getString("access_token", prefs.getString("jwt_token", "")) ?: ""
    val guardado = prefs.getString("nombre_empresa", "Casa Pellas") ?: "Casa Pellas"
    var nombreEmpresa by remember {
        mutableStateOf(if (guardado == "Empresa Corporativa") "Casa Pellas" else guardado)
    }

    var totalVacantesEmpresa by remember { mutableStateOf(0) }
    var vacantesRecientes by remember { mutableStateOf<List<VacanteEmpresaHomeItem>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    fun cargarResumenEmpresa() {
        scope.launch {
            cargando = true
            withContext(Dispatchers.IO) {
                try {
                    val urlEmp = URL("http://20.51.194.8:8000/api/empresas/$empresaId/")
                    val connE = (urlEmp.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 5000
                        readTimeout = 5000
                        if (token.isNotBlank()) {
                            setRequestProperty("Authorization", "Bearer $token")
                        }
                    }
                    if (connE.responseCode in 200..299) {
                        val objE = JSONObject(connE.inputStream.bufferedReader().use { it.readText() })
                        val realName = objE.optString("nombreempresa", objE.optString("NombreEmpresa", ""))
                        if (realName.isNotBlank() && realName != "null") {
                            nombreEmpresa = realName
                            prefs.edit().putString("nombre_empresa", realName).apply()
                        }
                    }
                    connE.disconnect()
                } catch (_: Exception) {}
            }
            val res = consultarVacantesPorEmpresaPaginadas(empresaId, 1, 3)
            vacantesRecientes = res.first
            totalVacantesEmpresa = res.second
            cargando = false
        }
    }

    LaunchedEffect(Unit) {
        cargarResumenEmpresa()
    }

    val azulOscuro = Color(0xFF0A2E5C)
    val fondoApp = Color(0xFFF1F5F9)

    Scaffold(
        containerColor = azulOscuro,
        bottomBar = {
            BarraInferiorEmpresa(context = context, itemActivo = 0)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "Hola, $nombreEmpresa",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Panel Principal de Reclutamiento Corporativo",
                    color = Color(0xFF93C5FD),
                    fontSize = 13.sp
                )
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = fondoApp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Accesos Rápidos Corporativos",
                        color = azulOscuro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
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
                                    context.startActivity(Intent(context, Empresa_vacantesActivity::class.java))
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconoVectorialDelineado(tipo = "work", tint = azulOscuro, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mis Vacantes", color = azulOscuro, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                }
                                Text("$totalVacantesEmpresa ofertas activas", color = Color(0xFF16A34A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Publicar con requisitos →", color = Color.Gray, fontSize = 11.sp)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    context.startActivity(Intent(context, Empresa_notificacionesActivity::class.java))
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconoVectorialDelineado(tipo = "bell", tint = azulOscuro, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Postulantes", color = azulOscuro, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                }
                                Text("Revisar CVs", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Aceptar o rechazar →", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Últimas Plazas Publicadas",
                            color = azulOscuro,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Gestionar todas ➔",
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(context, Empresa_vacantesActivity::class.java))
                            }
                        )
                    }

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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(vacantesRecientes, key = { it.id }) { vac ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            context.startActivity(Intent(context, Empresa_vacantesActivity::class.java))
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(vac.titulo, color = azulOscuro, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${vac.ubicacion} • ${vac.modalidad} • ${vac.salario}",
                                            color = Color(0xFF475569),
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

private suspend fun consultarVacantesPorEmpresaPaginadas(
    empresaId: Int,
    pagina: Int,
    pageSize: Int = 10
): Triple<List<VacanteEmpresaHomeItem>, Int, Boolean> = withContext(Dispatchers.IO) {
    val lista = mutableListOf<VacanteEmpresaHomeItem>()
    var total = 0
    var hayMas = false
    try {
        val url = URL("http://20.51.194.8:8000/api/empleos/?empresaid=$empresaId&page=$pagina&page_size=$pageSize")
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
                hayMas = !obj.isNull("next") && obj.optString("next", "").isNotBlank()
                obj.optJSONArray("results") ?: JSONArray()
            } else {
                val a = JSONArray(raw)
                total = a.length()
                a
            }
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                lista.add(
                    VacanteEmpresaHomeItem(
                        id = o.optInt("empleoid", i + 1),
                        titulo = extraerTituloRealVacante(o),
                        modalidad = o.optString("modalidad_nombre", "Presencial"),
                        ubicacion = o.optString("municipio_nombre", "Nicaragua"),
                        salario = "C$ ${o.optString("salario", o.optString("salariomax", "A convenir"))}"
                    )
                )
            }
        }
        conn.disconnect()
    } catch (_: Exception) {}
    Triple(lista, total, hayMas)
}