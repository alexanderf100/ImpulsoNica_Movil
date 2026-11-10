package com.example.impulsonica

import android.content.Context
import android.os.Bundle
import android.widget.Toast
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

data class NotificacionCandidatoModel(
    val id: String,
    val titulo: String,
    val mensaje: String,
    val fecha: String,
    val tipoAlerta: String,
    val estadoAccion: String,
    val leida: Boolean
)

class Candidato_notificacionesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PantallaNotificacionesCandidatoInteractiva()
            }
        }
    }
}

@Composable
fun PantallaNotificacionesCandidatoInteractiva() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs = context.getSharedPreferences("ImpulsoNicaPrefs", Context.MODE_PRIVATE)
    val candidatoId = prefs.getInt("candidato_id", prefs.getInt("usuario_sql_id", 1))

    var notificaciones by remember { mutableStateOf<List<NotificacionCandidatoModel>>(emptyList()) }
    var soloNoLeidas by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(true) }
    var notificacionAbierta by remember { mutableStateOf<NotificacionCandidatoModel?>(null) }

    fun cargarAlertas() {
        scope.launch {
            cargando = true
            notificaciones = obtenerNotificacionesCandidato(candidatoId)
            cargando = false
        }
    }

    LaunchedEffect(Unit) {
        cargarAlertas()
    }

    val azulOscuro = Color(0xFF0A2E5C)
    val fondoGris = Color(0xFFF4F6F9)
    val listaMostrada = if (soloNoLeidas) notificaciones.filter { !it.leida } else notificaciones
    val noLeidasCount = notificaciones.count { !it.leida }

    Scaffold(
        containerColor = azulOscuro,
        bottomBar = {
            BarraInferiorCandidato5Iconos(context = context, itemActivo = 3)
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
                    text = "Notificaciones",
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = if (!soloNoLeidas) Color(0xFFE0E7FF) else Color.White,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable { soloNoLeidas = false }
                            ) {
                                Text(
                                    text = "Todas (${notificaciones.size})",
                                    color = azulOscuro,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }

                            Surface(
                                color = if (soloNoLeidas) Color(0xFFE0E7FF) else Color.White,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable { soloNoLeidas = true }
                            ) {
                                Text(
                                    text = "No leídas ($noLeidasCount)",
                                    color = azulOscuro,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }

                        Text(
                            text = "↻ Actualizar",
                            color = Color(0xFF0066CC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { cargarAlertas() }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (cargando) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = azulOscuro)
                        }
                    } else if (listaMostrada.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No tienes notificaciones en esta bandeja.", color = Color.Gray, fontSize = 15.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(listaMostrada, key = { it.id }) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            notificacionAbierta = item
                                            if (!item.leida) {
                                                scope.launch {
                                                    marcarNotificacionLeidaCandidato(item.id)
                                                    cargarAlertas()
                                                }
                                            }
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (item.leida) Color.White else Color(0xFFEFF6FF)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = item.titulo,
                                                color = azulOscuro,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = item.fecha.take(10),
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = item.mensaje,
                                            color = Color(0xFF475569),
                                            fontSize = 14.sp,
                                            maxLines = 2
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Toca para abrir detalle completo ➔",
                                                color = Color(0xFF2563EB),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )

                                            TextButton(
                                                onClick = {
                                                    scope.launch {
                                                        val ok = borrarNotificacionDeCosmosDB(item.id)
                                                        if (ok) {
                                                            notificaciones = notificaciones.filterNot { it.id == item.id }
                                                            Toast.makeText(context, "Notificación eliminada", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("🗑️ Borrar", color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

    // MODAL DE VISTA AMPLIA DE LA NOTIFICACIÓN AL TOCARLA
    notificacionAbierta?.let { notif ->
        AlertDialog(
            onDismissRequest = { notificacionAbierta = null },
            title = {
                Column {
                    Surface(
                        color = Color(0xFF2563EB).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (notif.tipoAlerta.isNotBlank()) notif.tipoAlerta.replace("_", " ") else "Notificación Oficial",
                            color = Color(0xFF2563EB),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = notif.titulo,
                        color = azulOscuro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "📅 Fecha de recepción: ${notif.fecha}",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = notif.mensaje,
                            color = Color(0xFF1E293B),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    if (notif.estadoAccion.isNotBlank()) {
                        Text(
                            text = "Estado registrado: ${notif.estadoAccion}",
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val ok = borrarNotificacionDeCosmosDB(notif.id)
                                if (ok) {
                                    notificaciones = notificaciones.filterNot { it.id == notif.id }
                                    notificacionAbierta = null
                                    Toast.makeText(context, "Notificación borrada de tu perfil", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                    ) {
                        Text("🗑️ Eliminar Notificación", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { notificacionAbierta = null },
                        colors = ButtonDefaults.buttonColors(containerColor = azulOscuro)
                    ) {
                        Text("Cerrar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}

private suspend fun obtenerNotificacionesCandidato(candidatoId: Int): List<NotificacionCandidatoModel> =
    withContext(Dispatchers.IO) {
        val lista = mutableListOf<NotificacionCandidatoModel>()
        try {
            val url = URL("http://20.51.194.8:8000/api/v1/notificaciones/?usuario_sql_id=$candidatoId&rol=candidato")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
            }
            if (conn.responseCode in 200..299) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    lista.add(
                        NotificacionCandidatoModel(
                            id = o.optString("_id", "$i"),
                            titulo = o.optString("titulo", "Notificación"),
                            mensaje = o.optString("mensaje", ""),
                            fecha = o.optString("fecha_creacion", "Reciente"),
                            tipoAlerta = o.optString("tipo_alerta", "Postulación"),
                            estadoAccion = o.optString("estado_accion", ""),
                            leida = o.optBoolean("leida", false)
                        )
                    )
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}
        lista
    }

private suspend fun marcarNotificacionLeidaCandidato(notifId: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val url = URL("http://20.51.194.8:8000/api/v1/notificaciones/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                connectTimeout = 6000
                readTimeout = 6000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val payload = JSONObject().apply {
                put("id", notifId)
                put("notificacion_id", notifId)
                put("accion", "leida")
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val ok = conn.responseCode in 200..299
            conn.disconnect()
            ok
        } catch (_: Exception) {
            false
        }
    }

private suspend fun borrarNotificacionDeCosmosDB(notifId: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val url = URL("http://20.51.194.8:8000/api/v1/notificaciones/?id=$notifId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 6000
                readTimeout = 6000
            }
            val ok = conn.responseCode in 200..299
            conn.disconnect()
            ok
        } catch (_: Exception) {
            false
        }
    }