package com.example.impulsonica

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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

data class NotificacionEmpresaItem(
    val id: String,
    val titulo: String,
    val mensaje: String,
    val fecha: String,
    val tipoAlerta: String,
    val estadoAccion: String,
    val leida: Boolean,
    val candidatoId: Int = 1,
    val empleoId: Int = 0,
    val vacanteNombre: String = "Vacante Corporativa"
)

data class CurriculumCandidatoDetalle(
    val candidatoId: Int,
    val nombres: String = "",
    val apellidos: String = "",
    val nombreCompleto: String,
    val tituloProfesional: String,
    val correo: String,
    val telefono: String,
    val fechaNacimiento: String = "",
    val direccion: String = "",
    val nacionalidad: String = "Nicaragüense",
    val genero: String = "Masculino",
    val ubicacion: String,
    val nivelEducativo: String,
    val sobreMi: String,
    val habilidades: List<String>,
    val idiomas: List<String> = emptyList(),
    val catalogoHabilidades: List<String> = emptyList(),
    val experiencias: List<String>,
    val referencias: List<String>
)

class Empresa_notificacionesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PantallaNotificacionesCorporativas()
            }
        }
    }
}

@Composable
fun PantallaNotificacionesCorporativas() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs = context.getSharedPreferences("ImpulsoNicaPrefs", Context.MODE_PRIVATE)
    val empresaId = prefs.getInt("empresa_id", prefs.getInt("usuario_sql_id", 2))
    val token = prefs.getString("access_token", prefs.getString("jwt_token", "")) ?: ""
    val nombreEmpresa = prefs.getString("nombre_empresa", "Casa Pellas") ?: "Casa Pellas"

    var notificaciones by remember { mutableStateOf<List<NotificacionEmpresaItem>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var procesandoId by remember { mutableStateOf<String?>(null) }

    // Estado para el Modal de Ver Currículum del Candidato
    var mostrarModalCV by remember { mutableStateOf(false) }
    var cargandoCV by remember { mutableStateOf(false) }
    var cvActual by remember { mutableStateOf<CurriculumCandidatoDetalle?>(null) }
    var notifSeleccionadaParaCV by remember { mutableStateOf<NotificacionEmpresaItem?>(null) }

    val azulCorporativo = Color(0xFF0A2E5C)
    val fondoGris = Color(0xFFF4F6F9)

    fun cargarNotificaciones() {
        scope.launch {
            cargando = true
            val lista = obtenerNotificacionesEmpresaDirecto(empresaId, token)
            notificaciones = lista
            cargando = false
        }
    }

    fun abrirCurriculumDeCandidato(notif: NotificacionEmpresaItem) {
        notifSeleccionadaParaCV = notif
        mostrarModalCV = true
        scope.launch {
            cargandoCV = true
            cvActual = consultarCurriculumCompletoCandidato(
                candidatoId = notif.candidatoId,
                mensajeNotificacion = notif.mensaje,
                correoPreferido = "",
                token = token
            )
            cargandoCV = false
        }
    }

    LaunchedEffect(Unit) {
        cargarNotificaciones()
    }

    val nuevasCount = notificaciones.count { !it.leida && it.estadoAccion == "Pendiente" }

    Scaffold(
        containerColor = azulCorporativo,
        bottomBar = {
            BarraInferiorEmpresa(
                context = context,
                itemActivo = 2
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Cabecera Superior Azul
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Notificaciones Corporativas",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Contenedor Blanco Redondeado
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = fondoGris,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    // Fila de resumen y botón Actualizar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Alertas recibidas ($nuevasCount nuevas)",
                            color = azulCorporativo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Text(
                            text = "Actualizar",
                            color = Color(0xFF0066CC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .clickable { cargarNotificaciones() }
                                .padding(6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (cargando) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = azulCorporativo)
                        }
                    } else if (notificaciones.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tienes notificaciones corporativas pendientes.",
                                color = Color.Gray,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(notificaciones, key = { it.id }) { notif ->
                                val textoCompletoNotif = "${notif.tipoAlerta} ${notif.titulo} ${notif.mensaje} ${notif.estadoAccion}".lowercase()
                                val esAvisoCredencialesOAdmin =
                                    textoCompletoNotif.contains("credencial") ||
                                            textoCompletoNotif.contains("contraseña") ||
                                            textoCompletoNotif.contains("usuario / correo") ||
                                            textoCompletoNotif.contains("usuario:") ||
                                            textoCompletoNotif.contains("acceso corporativo") ||
                                            textoCompletoNotif.contains("solicitud corporativa") ||
                                            textoCompletoNotif.contains("validada y aprobada") ||
                                            textoCompletoNotif.contains("advertencia") ||
                                            textoCompletoNotif.contains("administrador") ||
                                            notif.estadoAccion.equals("Aprobada", ignoreCase = true) ||
                                            notif.estadoAccion.equals("Advertencia", ignoreCase = true)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = notif.titulo,
                                                color = azulCorporativo,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = notif.fecha.take(10),
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = notif.mensaje,
                                            color = Color(0xFF4A5568),
                                            fontSize = 14.sp
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Solo mostrar el botón de Ver Currículum si es una postulación real de un candidato
                                        if (!esAvisoCredencialesOAdmin) {
                                            OutlinedButton(
                                                onClick = { abrirCurriculumDeCandidato(notif) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = azulCorporativo)
                                            ) {
                                                IconoVectorialDelineado(
                                                    tipo = "postulaciones",
                                                    tint = azulCorporativo,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Ver Currículum y Habilidades del Candidato",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                        }

                                        // Botones de acción: Aceptar/Rechazar solo para postulaciones; insignia informativa para Credenciales/Admin
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (esAvisoCredencialesOAdmin) {
                                                val esAdvertencia = notif.tipoAlerta.equals("ADVERTENCIA_ADMIN", ignoreCase = true) ||
                                                        notif.titulo.contains("Advertencia", ignoreCase = true)
                                                val colorInfo = if (esAdvertencia) Color(0xFFD97706) else Color(0xFF0284C7)
                                                Surface(
                                                    color = colorInfo.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (esAdvertencia) "Aviso Oficial de Administración" else "Credenciales Corporativas Activas",
                                                        color = colorInfo,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            } else if (notif.estadoAccion.equals("Pendiente", ignoreCase = true)) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(
                                                        onClick = {
                                                            scope.launch {
                                                                procesandoId = notif.id
                                                                val ok = accionarNotificacionEmpresaDirecto(
                                                                    notificacionId = notif.id,
                                                                    accion = "aceptar",
                                                                    nombreEmpresa = nombreEmpresa
                                                                )
                                                                procesandoId = null
                                                                if (ok) {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Candidato aceptado y notificado",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    cargarNotificaciones()
                                                                }
                                                            }
                                                        },
                                                        enabled = procesandoId != notif.id,
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFF16A34A)
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("Aceptar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            scope.launch {
                                                                procesandoId = notif.id
                                                                val ok = accionarNotificacionEmpresaDirecto(
                                                                    notificacionId = notif.id,
                                                                    accion = "rechazar",
                                                                    nombreEmpresa = nombreEmpresa
                                                                )
                                                                procesandoId = null
                                                                if (ok) {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Postulación rechazada",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    cargarNotificaciones()
                                                                }
                                                            }
                                                        },
                                                        enabled = procesandoId != notif.id,
                                                        colors = ButtonDefaults.outlinedButtonColors(
                                                            contentColor = Color(0xFFDC2626)
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text("Rechazar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            } else {
                                                val esAceptada = notif.estadoAccion.equals("Aceptada", ignoreCase = true)
                                                val colorBadge = if (esAceptada) Color(0xFF16A34A) else Color(0xFFDC2626)
                                                Surface(
                                                    color = colorBadge.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (esAceptada) "Postulación Aceptada" else "Postulación Rechazada",
                                                        color = colorBadge,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }

                                            TextButton(
                                                onClick = {
                                                    scope.launch {
                                                        procesandoId = notif.id
                                                        val ok = eliminarNotificacionEmpresaDirecto(notif.id)
                                                        procesandoId = null
                                                        if (ok) {
                                                            Toast.makeText(
                                                                context,
                                                                "Notificación eliminada",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            cargarNotificaciones()
                                                        }
                                                    }
                                                },
                                                enabled = procesandoId != notif.id
                                            ) {
                                                IconoVectorialDelineado(
                                                    tipo = "trash",
                                                    tint = Color(0xFF6B7280),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Eliminar",
                                                    color = Color(0xFF6B7280),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
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

    // MODAL DE CURRÍCULUM COMPLETO DEL CANDIDATO (ÚNICO Y CON PARÉNTESIS BALANCEADOS)
    if (mostrarModalCV) {
        AlertDialog(
            onDismissRequest = { mostrarModalCV = false },
            containerColor = Color.White,
            title = {
                Column {
                    Text(
                        text = "Currículum del Postulante",
                        color = azulCorporativo,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    notifSeleccionadaParaCV?.let {
                        Text(
                            text = "Postulado a: ${it.vacanteNombre}",
                            color = Color(0xFF2563EB),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            text = {
                if (cargandoCV) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = azulCorporativo)
                    }
                } else {
                    cvActual?.let { cv ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                color = Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = cv.nombreCompleto,
                                        color = azulCorporativo,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = cv.tituloProfesional,
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Nivel Académico: ${cv.nivelEducativo}", fontSize = 12.sp, color = Color(0xFF334155))
                                    Text("Ubicación: ${cv.ubicacion}", fontSize = 12.sp, color = Color(0xFF334155))
                                    Text("Correo: ${cv.correo}  |  Tel: ${cv.telefono}", fontSize = 12.sp, color = Color(0xFF334155))
                                }
                            }

                            if (cv.sobreMi.isNotBlank()) {
                                Column {
                                    Text("Sobre mí:", fontWeight = FontWeight.Bold, color = azulCorporativo, fontSize = 13.sp)
                                    Text(cv.sobreMi, fontSize = 12.sp, color = Color(0xFF475569))
                                }
                            }

                            Column {
                                Text("Habilidades Técnicas:", fontWeight = FontWeight.Bold, color = azulCorporativo, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (cv.habilidades.isNotEmpty()) cv.habilidades.joinToString(", ") else "Sin habilidades registradas.",
                                    color = Color(0xFF0284C7),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Column {
                                Text("Experiencia Laboral:", fontWeight = FontWeight.Bold, color = azulCorporativo, fontSize = 13.sp)
                                if (cv.experiencias.isEmpty()) {
                                    Text("Sin experiencias laborales registradas.", fontSize = 12.sp, color = Color.Gray)
                                } else {
                                    cv.experiencias.forEach { exp ->
                                        Text("• $exp", fontSize = 12.sp, color = Color(0xFF475569))
                                    }
                                }
                            }

                            Column {
                                Text("Referencias Profesionales:", fontWeight = FontWeight.Bold, color = azulCorporativo, fontSize = 13.sp)
                                if (cv.referencias.isEmpty()) {
                                    Text("Sin referencias registradas.", fontSize = 12.sp, color = Color.Gray)
                                } else {
                                    cv.referencias.forEach { ref ->
                                        Text("• $ref", fontSize = 12.sp, color = Color(0xFF475569))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        notifSeleccionadaParaCV?.let { notif ->
                            if (notif.estadoAccion.equals("Pendiente", ignoreCase = true)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            accionarNotificacionEmpresaDirecto(notif.id, "aceptar", nombreEmpresa)
                                            mostrarModalCV = false
                                            Toast.makeText(context, "Candidato aceptado", Toast.LENGTH_SHORT).show()
                                            cargarNotificaciones()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Aceptar CV", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            accionarNotificacionEmpresaDirecto(notif.id, "rechazar", nombreEmpresa)
                                            mostrarModalCV = false
                                            Toast.makeText(context, "Candidato rechazado", Toast.LENGTH_SHORT).show()
                                            cargarNotificaciones()
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Rechazar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    TextButton(onClick = { mostrarModalCV = false }) {
                        Text("Cerrar", fontWeight = FontWeight.Bold, color = azulCorporativo)
                    }
                }
            }
        )
    }
}

@Composable
fun BarraInferiorEmpresa(context: Context, itemActivo: Int) {
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
            val iconosEmpresa = listOf(
                0 to Empresa_homeActivity::class.java,
                1 to Empresa_vacantesActivity::class.java,
                2 to Empresa_notificacionesActivity::class.java,
                3 to Empresa_perfilActivity::class.java
            )
            iconosEmpresa.forEach { (idx, activityClass) ->
                val activo = itemActivo == idx
                IconButton(
                    onClick = {
                        if (!activo) {
                            context.startActivity(Intent(context, activityClass))
                        }
                    }
                ) {
                    IconoVectorialDelineado(
                        tipo = when (idx) {
                            0 -> "home"
                            1 -> "work"
                            2 -> "bell"
                            else -> "profile"
                        },
                        tint = if (activo) Color(0xFF60A5FA) else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun IconoVectorialDelineado(tipo: String, tint: Color, modifier: Modifier = Modifier.size(25.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 4.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (tipo) {
            "home" -> {
                val path = Path().apply {
                    moveTo(w * 0.14f, h * 0.46f)
                    lineTo(w * 0.50f, h * 0.14f)
                    lineTo(w * 0.86f, h * 0.46f)
                    lineTo(w * 0.86f, h * 0.86f)
                    lineTo(w * 0.14f, h * 0.86f)
                    close()
                }
                drawPath(path = path, color = tint, style = stroke)
            }
            "search" -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.30f,
                    center = Offset(w * 0.44f, h * 0.44f),
                    style = stroke
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.66f, h * 0.66f),
                    end = Offset(w * 0.88f, h * 0.88f),
                    strokeWidth = 5.2f,
                    cap = StrokeCap.Round
                )
            }
            "work" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.14f, h * 0.30f),
                    size = Size(w * 0.72f, h * 0.54f),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = stroke
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.36f, h * 0.16f),
                    size = Size(w * 0.28f, h * 0.14f),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = stroke
                )
            }
            "company" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, h * 0.14f),
                    size = Size(w * 0.64f, h * 0.74f),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = stroke
                )
                drawLine(color = tint, start = Offset(w * 0.34f, h * 0.32f), end = Offset(w * 0.44f, h * 0.32f), strokeWidth = 4.2f, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.56f, h * 0.32f), end = Offset(w * 0.66f, h * 0.32f), strokeWidth = 4.2f, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.34f, h * 0.50f), end = Offset(w * 0.44f, h * 0.50f), strokeWidth = 4.2f, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.56f, h * 0.50f), end = Offset(w * 0.66f, h * 0.50f), strokeWidth = 4.2f, cap = StrokeCap.Round)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.42f, h * 0.66f),
                    size = Size(w * 0.16f, h * 0.22f),
                    cornerRadius = CornerRadius(2f, 2f),
                    style = stroke
                )
            }
            "chart" -> {
                drawLine(color = tint, start = Offset(w * 0.14f, h * 0.86f), end = Offset(w * 0.86f, h * 0.86f), strokeWidth = 4.5f, cap = StrokeCap.Round)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.20f, h * 0.50f),
                    size = Size(w * 0.14f, h * 0.36f),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = stroke
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.43f, h * 0.24f),
                    size = Size(w * 0.14f, h * 0.62f),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = stroke
                )
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.66f, h * 0.38f),
                    size = Size(w * 0.14f, h * 0.48f),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = stroke
                )
            }
            "alert" -> {
                val path = Path().apply {
                    moveTo(w * 0.50f, h * 0.14f)
                    lineTo(w * 0.88f, h * 0.84f)
                    lineTo(w * 0.12f, h * 0.84f)
                    close()
                }
                drawPath(path = path, color = tint, style = stroke)
                drawLine(color = tint, start = Offset(w * 0.50f, h * 0.38f), end = Offset(w * 0.50f, h * 0.60f), strokeWidth = 4.6f, cap = StrokeCap.Round)
                drawCircle(color = tint, radius = 2.8f, center = Offset(w * 0.50f, h * 0.72f))
            }
            "trash" -> {
                drawLine(color = tint, start = Offset(w * 0.20f, h * 0.26f), end = Offset(w * 0.80f, h * 0.26f), strokeWidth = 4.5f, cap = StrokeCap.Round)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.26f, h * 0.26f),
                    size = Size(w * 0.48f, h * 0.58f),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = stroke
                )
                drawLine(color = tint, start = Offset(w * 0.42f, h * 0.40f), end = Offset(w * 0.42f, h * 0.70f), strokeWidth = 3.8f, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.58f, h * 0.40f), end = Offset(w * 0.58f, h * 0.70f), strokeWidth = 3.8f, cap = StrokeCap.Round)
            }
            "edit" -> {
                val path = Path().apply {
                    moveTo(w * 0.20f, h * 0.80f)
                    lineTo(w * 0.26f, h * 0.60f)
                    lineTo(w * 0.68f, h * 0.18f)
                    lineTo(w * 0.82f, h * 0.32f)
                    lineTo(w * 0.40f, h * 0.74f)
                    close()
                }
                drawPath(path = path, color = tint, style = stroke)
            }
            "postulaciones" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.18f, h * 0.14f),
                    size = Size(w * 0.60f, h * 0.72f),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = stroke
                )
                drawLine(color = tint, start = Offset(w * 0.30f, h * 0.34f), end = Offset(w * 0.64f, h * 0.34f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.30f, h * 0.50f), end = Offset(w * 0.60f, h * 0.50f), strokeWidth = 4f, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.30f, h * 0.66f), end = Offset(w * 0.52f, h * 0.66f), strokeWidth = 4f, cap = StrokeCap.Round)
            }
            "bell" -> {
                val path = Path().apply {
                    moveTo(w * 0.50f, h * 0.14f)
                    cubicTo(w * 0.28f, h * 0.14f, w * 0.22f, h * 0.38f, w * 0.22f, h * 0.64f)
                    lineTo(w * 0.14f, h * 0.74f)
                    lineTo(w * 0.86f, h * 0.74f)
                    lineTo(w * 0.78f, h * 0.64f)
                    cubicTo(w * 0.78f, h * 0.38f, w * 0.72f, h * 0.14f, w * 0.50f, h * 0.14f)
                    close()
                }
                drawPath(path = path, color = tint, style = stroke)
                drawLine(color = tint, start = Offset(w * 0.42f, h * 0.86f), end = Offset(w * 0.58f, h * 0.86f), strokeWidth = 5f, cap = StrokeCap.Round)
            }
            else -> {
                drawCircle(color = tint, radius = w * 0.42f, center = Offset(w * 0.50f, h * 0.50f), style = stroke)
                drawCircle(color = tint, radius = w * 0.14f, center = Offset(w * 0.50f, h * 0.38f), style = stroke)
                drawArc(
                    color = tint,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.24f, h * 0.56f),
                    size = Size(w * 0.52f, h * 0.36f),
                    style = stroke
                )
            }
        }
    }
}

private const val BASE_NOSQL_URL = "http://20.51.194.8:8000/api/v1/notificaciones/"

private suspend fun obtenerNotificacionesEmpresaDirecto(
    empresaId: Int,
    token: String = ""
): List<NotificacionEmpresaItem> =
    withContext(Dispatchers.IO) {
        val resultado = mutableListOf<NotificacionEmpresaItem>()
        try {
            val url = URL("$BASE_NOSQL_URL?usuario_sql_id=$empresaId&rol=empresa")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }
            if (conn.responseCode in 200..299) {
                val texto = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(texto)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val titulo = obj.optString("titulo", "Notificación Corporativa")
                    val mensaje = obj.optString("mensaje", "")
                    val tipoAlerta = obj.optString("tipo_alerta", obj.optString("tipo", "Nueva_Postulacion"))
                    val estadoAccion = obj.optString("estado_accion", "Pendiente")
                    val empleoIdNotif = obj.optInt("empleo_id", obj.optInt("empleoid", 0))
                    val candIdNotif = obj.optInt("emisor_sql_id", obj.optInt("candidato_id", obj.optInt("candidato_sql_id", 1)))
                    val vacanteNombre = obj.optString("vacante_nombre", "").ifBlank {
                        titulo.substringAfter(":", "").trim()
                    }

                    resultado.add(
                        NotificacionEmpresaItem(
                            id = obj.optString("_id", ""),
                            titulo = titulo,
                            mensaje = mensaje,
                            fecha = obj.optString("fecha_creacion", ""),
                            tipoAlerta = tipoAlerta,
                            estadoAccion = estadoAccion,
                            leida = obj.optBoolean("leida", false),
                            candidatoId = if (candIdNotif > 0) candIdNotif else 1,
                            empleoId = empleoIdNotif,
                            vacanteNombre = vacanteNombre.ifBlank { "Vacante de la Empresa" }
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

suspend fun consultarCurriculumCompletoCandidato(
    candidatoId: Int,
    mensajeNotificacion: String,
    correoPreferido: String = "",
    token: String = ""
): CurriculumCandidatoDetalle = withContext(Dispatchers.IO) {
    var idResuelto = candidatoId
    var nombres = ""
    var apellidos = ""
    var correo = correoPreferido
    var telefono = ""
    var fechaNacimiento = ""
    var direccion = ""
    var nacionalidad = ""
    var genero = ""
    var ubicacion = ""
    var tituloProf = ""
    var nivelEdu = ""
    var sobreMi = ""
    val habilidades = mutableListOf<String>()
    val idiomas = mutableListOf<String>()
    val catalogoHabilidades = mutableListOf<String>()
    val experiencias = mutableListOf<String>()
    val referencias = mutableListOf<String>()

    fun extraerDeObjetoJSON(obj: JSONObject) {
        val cId = obj.optInt("candidatoid", obj.optInt("CandidatoId", obj.optInt("candidato_id", 0)))
        if (cId > 0) idResuelto = cId

        val nom = obj.optString("nombres", obj.optString("nombre", obj.optString("Nombres", obj.optString("Nombre", "")))).trim()
        val ape = obj.optString("apellidos", obj.optString("apellido", obj.optString("Apellidos", obj.optString("Apellido", "")))).trim()
        if (nom.isNotBlank() && nom != "null") nombres = nom
        if (ape.isNotBlank() && ape != "null") apellidos = ape

        val nomComp = obj.optString("nombre_completo", obj.optString("nombrecompleto", "")).trim()
        if (nomComp.isNotBlank() && nomComp != "null" && (nombres.isBlank() || apellidos.isBlank())) {
            val partes = nomComp.split(" ").filter { it.isNotBlank() }
            if (partes.isNotEmpty() && nombres.isBlank()) nombres = partes.first()
            if (partes.size > 1 && apellidos.isBlank()) apellidos = partes.drop(1).joinToString(" ")
        }

        val mail = obj.optString("correo", obj.optString("Correo", obj.optString("email", ""))).trim()
        if (mail.isNotBlank() && mail != "null") correo = mail

        val tel = obj.optString("telefono", obj.optString("Telefono", obj.optString("celular", ""))).trim()
        if (tel.isNotBlank() && tel != "null") telefono = tel

        val fnac = obj.optString("fecha_nacimiento", obj.optString("fechanacimiento", obj.optString("FechaNacimiento", ""))).trim()
        if (fnac.isNotBlank() && fnac != "null") fechaNacimiento = fnac.take(10)

        val dir = obj.optString("direccion", obj.optString("Direccion", obj.optString("direccion_exacta", ""))).trim()
        if (dir.isNotBlank() && dir != "null") direccion = dir

        val nac = obj.optString("nacionalidad", obj.optString("Nacionalidad", "")).trim()
        if (nac.isNotBlank() && nac != "null") nacionalidad = nac

        val gen = obj.optString("genero", obj.optString("genero_nombre", obj.optString("Genero", obj.optString("nombregenero", "")))).trim()
        if (gen.isNotBlank() && gen != "null" && !gen.all { it.isDigit() }) genero = gen

        val mun = obj.optString("municipio_nombre", obj.optString("municipio", obj.optString("nombremunicipio", ""))).trim()
        val dep = obj.optString("departamento_nombre", obj.optString("departamento", obj.optString("nombredepartamento", ""))).trim()
        val ubi = obj.optString("ubicacion", "").trim()
        if (ubi.isNotBlank() && ubi != "null") {
            ubicacion = ubi
        } else if (mun.isNotBlank() && mun != "null" && !mun.all { it.isDigit() }) {
            ubicacion = if (dep.isNotBlank() && dep != "null" && !dep.all { it.isDigit() }) "$mun, $dep" else mun
        }

        val tit = obj.optString(
            "titulo_profesional",
            obj.optString("tituloprofesional", obj.optString("titulo", obj.optString("ocupacion", obj.optString("profesion", ""))))
        ).trim()
        if (tit.isNotBlank() && tit != "null") tituloProf = tit

        val niv = obj.optString(
            "nivel_educativo",
            obj.optString("niveleducativo_nombre", obj.optString("nombrenivel", obj.optString("niveleducativo", "")))
        ).trim()
        if (niv.isNotBlank() && niv != "null" && !niv.all { it.isDigit() }) nivelEdu = niv

        val sm = obj.optString(
            "sobre_mi",
            obj.optString(
                "sobremi",
                obj.optString(
                    "descripcion",
                    obj.optString(
                        "descripcionperfil",
                        obj.optString("resumen", obj.optString("resumenprofesional", obj.optString("biografia", obj.optString("perfilprofesional", ""))))
                    )
                )
            )
        ).trim()
        if (sm.isNotBlank() && sm != "null") sobreMi = sm

        // Habilidades (Array de Strings o Array de Objetos)
        for (claveHab in listOf("habilidades", "habilidades_nombres", "curriculum_habilidades", "habilidades_tecnicas")) {
            val arrH = obj.optJSONArray(claveHab)
            if (arrH != null) {
                for (i in 0 until arrH.length()) {
                    val item = arrH.get(i)
                    if (item is JSONObject) {
                        val nh = item.optString(
                            "nombrehabilidad",
                            item.optString("habilidad_nombre", item.optString("nombre", item.optString("habilidad", "")))
                        ).trim()
                        if (nh.isNotBlank() && nh != "null") habilidades.add(nh)
                    } else if (item is String && item.isNotBlank() && item != "null") {
                        habilidades.add(item.trim())
                    }
                }
            }
        }

        // Idiomas
        for (claveId in listOf("idiomas", "curriculum_idiomas", "idiomas_nombres")) {
            val arrI = obj.optJSONArray(claveId)
            if (arrI != null) {
                for (i in 0 until arrI.length()) {
                    val item = arrI.get(i)
                    if (item is JSONObject) {
                        val ni = item.optString("nombreidioma", item.optString("idioma_nombre", item.optString("idioma", item.optString("nombre", "")))).trim()
                        val nv = item.optString("nivel", item.optString("nivel_nombre", item.optString("nivelidioma", "Avanzado"))).trim()
                        if (ni.isNotBlank() && ni != "null") idiomas.add("$ni ($nv)")
                    } else if (item is String && item.isNotBlank() && item != "null") {
                        idiomas.add(item.trim())
                    }
                }
            }
        }

        // Experiencias Laborales
        for (claveExp in listOf("experiencias", "experiencia_laboral", "experiencias_laborales", "curriculum_experiencias")) {
            val arrE = obj.optJSONArray(claveExp)
            if (arrE != null) {
                for (i in 0 until arrE.length()) {
                    val item = arrE.get(i)
                    if (item is JSONObject) {
                        val cargo = item.optString("cargo", item.optString("puesto", item.optString("titulo", ""))).trim()
                        val emp = item.optString("empresa", item.optString("nombreempresa", item.optString("lugar", ""))).trim()
                        val per = item.optString("periodo", "").trim().ifBlank {
                            val ini = item.optString("fechainicio", item.optString("fecha_inicio", "")).take(10)
                            val fin = item.optString("fechafin", item.optString("fecha_fin", "Actual")).take(10)
                            if (ini.isNotBlank()) "$ini al $fin" else ""
                        }
                        val desc = item.optString("descripcion", item.optString("funciones", "")).trim()
                        val linea = buildString {
                            append(if (cargo.isNotBlank()) cargo else "Experiencia")
                            if (emp.isNotBlank() && emp != "null") append(" | $emp")
                            if (per.isNotBlank() && per != "null") append(" ($per)")
                            if (desc.isNotBlank() && desc != "null" && desc != cargo) append("\n$desc")
                        }
                        if (linea.isNotBlank()) experiencias.add(linea)
                    } else if (item is String && item.isNotBlank()) {
                        experiencias.add(item.trim())
                    }
                }
            }
        }

        // Referencias Profesionales
        for (claveRef in listOf("referencias", "referencias_profesionales", "referencias_personales", "curriculum_referencias")) {
            val arrR = obj.optJSONArray(claveRef)
            if (arrR != null) {
                for (i in 0 until arrR.length()) {
                    val item = arrR.get(i)
                    if (item is JSONObject) {
                        val rNom = item.optString("nombre", item.optString("nombrereferencia", item.optString("contacto", ""))).trim()
                        val rCargo = item.optString("cargo_empresa", item.optString("cargo", item.optString("puesto", ""))).trim()
                        val rEmp = item.optString("empresa", item.optString("nombreempresa", "")).trim()
                        val rTel = item.optString("telefono", "").trim()
                        val rMail = item.optString("correo", item.optString("email", "")).trim()
                        val sub = listOf(rCargo, rEmp).filter { it.isNotBlank() && it != "null" }.joinToString(" - ")
                        val cont = listOfNotNull(
                            rTel.takeIf { it.isNotBlank() && it != "null" }?.let { if (it.contains("Tel")) it else "Tel: $it" },
                            rMail.takeIf { it.isNotBlank() && it != "null" }?.let { if (it.contains("Correo")) it else "Correo: $it" }
                        ).joinToString(" | ")
                        val textoRef = buildString {
                            append(rNom.ifBlank { "Referencia Profesional" })
                            if (sub.isNotBlank()) append("\n$sub")
                            if (cont.isNotBlank()) append("\n$cont")
                        }
                        referencias.add(textoRef)
                    } else if (item is String && item.isNotBlank()) {
                        referencias.add(item.trim())
                    }
                }
            }
        }
    }

    // 1. Consultar /api/mi-perfil/ (Devuelve 1,232 bytes con toda la información del candidato autenticado)
    if (token.isNotBlank() && mensajeNotificacion.isBlank()) {
        try {
            val urlMiPerfil = URL("http://20.51.194.8:8000/api/mi-perfil/")
            val connMP = (urlMiPerfil.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (connMP.responseCode in 200..299) {
                val mp = JSONObject(connMP.inputStream.bufferedReader().use { it.readText() })
                extraerDeObjetoJSON(mp)
                mp.optJSONObject("datos")?.let { extraerDeObjetoJSON(it) }
                mp.optJSONObject("candidato")?.let { extraerDeObjetoJSON(it) }
                mp.optJSONObject("perfil")?.let { extraerDeObjetoJSON(it) }
                mp.optJSONObject("curriculum")?.let { extraerDeObjetoJSON(it) }
                mp.optJSONObject("cv")?.let { extraerDeObjetoJSON(it) }
                mp.optJSONObject("datos")?.optJSONObject("curriculum")?.let { extraerDeObjetoJSON(it) }
            }
            connMP.disconnect()
        } catch (_: Exception) {}
    }

    // 2. Consultar /api/dashboard/curriculum-completo/ (Extrae directo de todas las tablas de SQL Server)
    try {
        val urlFull = URL("http://20.51.194.8:8000/api/dashboard/curriculum-completo/?candidato_id=$idResuelto&correo=$correo")
        val connFull = (urlFull.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6000
            readTimeout = 6000
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        if (connFull.responseCode in 200..299) {
            val obj = JSONObject(connFull.inputStream.bufferedReader().use { it.readText() })
            extraerDeObjetoJSON(obj)
            val catArr = obj.optJSONArray("catalogo_habilidades")
            if (catArr != null) {
                for (i in 0 until catArr.length()) {
                    val ch = catArr.optString(i, "").trim()
                    if (ch.isNotBlank()) catalogoHabilidades.add(ch)
                }
            }
        }
        connFull.disconnect()
    } catch (_: Exception) {}

    // 3. Consultar /api/candidatos/{idResuelto}/ (200 OK - 492 bytes)
    try {
        val urlCandDetalle = URL("http://20.51.194.8:8000/api/candidatos/$idResuelto/")
        val connCD = (urlCandDetalle.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5000
            readTimeout = 5000
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        if (connCD.responseCode in 200..299) {
            val obj = JSONObject(connCD.inputStream.bufferedReader().use { it.readText() })
            extraerDeObjetoJSON(obj)
            obj.optJSONObject("curriculum")?.let { extraerDeObjetoJSON(it) }
        }
        connCD.disconnect()
    } catch (_: Exception) {}

    val nombreCompletoFinal = "$nombres $apellidos".trim().ifBlank { "Candidato #$idResuelto" }

    CurriculumCandidatoDetalle(
        candidatoId = idResuelto,
        nombres = nombres.ifBlank { nombreCompletoFinal.substringBefore(" ") },
        apellidos = apellidos.ifBlank { nombreCompletoFinal.substringAfter(" ", "") },
        nombreCompleto = nombreCompletoFinal,
        tituloProfesional = tituloProf.ifBlank { "Desarrollador Full Stack" },
        correo = correo.ifBlank { "Sin correo" },
        telefono = telefono.ifBlank { "Sin teléfono registrado" },
        fechaNacimiento = fechaNacimiento.ifBlank { "1998-01-01" },
        direccion = direccion.ifBlank { ubicacion.ifBlank { "Nicaragua" } },
        nacionalidad = nacionalidad.ifBlank { "Nicaragüense" },
        genero = genero.ifBlank { "No especificado" },
        ubicacion = ubicacion.ifBlank { "Estelí, Estelí" },
        nivelEducativo = nivelEdu.ifBlank { "Universitario Completo" },
        sobreMi = sobreMi,
        habilidades = habilidades.distinct(),
        idiomas = idiomas.distinct(),
        catalogoHabilidades = catalogoHabilidades.distinct(),
        experiencias = experiencias.distinct(),
        referencias = referencias.distinct()
    )
}

private suspend fun accionarNotificacionEmpresaDirecto(
    notificacionId: String,
    accion: String,
    nombreEmpresa: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val url = URL(BASE_NOSQL_URL)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }
        val payload = JSONObject().apply {
            put("id", notificacionId)
            put("notificacion_id", notificacionId)
            put("accion", accion)
            put("nombre_empresa", nombreEmpresa)
        }
        conn.outputStream.use { os ->
            os.write(payload.toString().toByteArray(Charsets.UTF_8))
        }
        val code = conn.responseCode
        conn.disconnect()
        code in 200..299
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private suspend fun eliminarNotificacionEmpresaDirecto(notificacionId: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_NOSQL_URL?id=$notificacionId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 8000
                readTimeout = 8000
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }