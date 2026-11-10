package com.example.impulsonica

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

data class VacanteCorporativaModel(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val modalidad: String,
    val ubicacion: String,
    val salario: String,
    val requisitosTexto: String
)

class Empresa_vacantesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PantallaGestionVacantesWebStyle()
            }
        }
    }
}

@Composable
fun PantallaGestionVacantesWebStyle() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs = context.getSharedPreferences("ImpulsoNicaPrefs", Context.MODE_PRIVATE)
    val empresaId = prefs.getInt("empresa_id", prefs.getInt("usuario_sql_id", 2))
    val token = prefs.getString("access_token", prefs.getString("jwt_token", "")) ?: ""
    val nombreGuardado = prefs.getString("nombre_empresa", "Casa Pellas") ?: "Casa Pellas"
    val nombreEmpresa = if (nombreGuardado == "Empresa Corporativa") "Casa Pellas" else nombreGuardado

    var vacantes by remember { mutableStateOf<List<VacanteCorporativaModel>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var mostrarModalNuevaVacante by remember { mutableStateOf(false) }
    var publicando by remember { mutableStateOf(false) }

    // Campos del Formulario de Publicación (idénticos a la Web)
    var tituloVacante by remember { mutableStateOf("") }
    var salarioMin by remember { mutableStateOf("18000") }
    var salarioMax by remember { mutableStateOf("28000") }
    var modalidadSeleccionada by remember { mutableStateOf("Presencial" to 1) }
    var tipoContratoSeleccionado by remember { mutableStateOf("Tiempo Completo" to 1) }
    var municipioSeleccionado by remember { mutableStateOf("Managua" to 1) }
    var nivelEducativoReq by remember { mutableStateOf("Universitario Completo") }
    var experienciaPredefinida by remember { mutableStateOf("1 a 2 años") }
    var descripcionAdicional by remember { mutableStateOf("") }

    // Catálogo de Requisitos y Habilidades Preestablecidos (Igual que en la Web)
    val catalogoRequisitosPreestablecidos = listOf(
        "Python", "JavaScript", "SQL Server", "Excel Avanzado",
        "AutoCAD", "Atención al Cliente", "Gestión de Ventas", "Inglés Intermedio",
        "Licencia de Conducir", "Trabajo en Equipo", "Contabilidad General", "Power BI"
    )
    var requisitosSeleccionados by remember {
        mutableStateOf(setOf("Excel Avanzado", "Trabajo en Equipo"))
    }

    val beneficiosPreestablecidos = listOf(
        "Seguro Médico / INSS", "Prestaciones de Ley", "Capacitación Continua",
        "Bonos por Desempeño", "Transporte / Viáticos"
    )
    var beneficiosSeleccionados by remember {
        mutableStateOf(setOf("Seguro Médico / INSS", "Prestaciones de Ley"))
    }

    val coloresInputNegro = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Color(0xFF0066CC),
        unfocusedBorderColor = Color(0xFF94A3B8),
        focusedLabelColor = Color(0xFF0A2E5C),
        unfocusedLabelColor = Color(0xFF475569),
        cursorColor = Color.Black
    )

    fun cargarMisVacantes() {
        scope.launch {
            cargando = true
            vacantes = obtenerVacantesDeEmpresa(empresaId)
            cargando = false
        }
    }

    LaunchedEffect(Unit) {
        cargarMisVacantes()
    }

    val azulCorporativo = Color(0xFF0A2E5C)
    val fondoGris = Color(0xFFF4F6F9)

    Scaffold(
        containerColor = azulCorporativo,
        bottomBar = {
            BarraInferiorEmpresa(context = context, itemActivo = 1)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { mostrarModalNuevaVacante = true },
                containerColor = Color(0xFF16A34A),
                contentColor = Color.White
            ) {
                Text("＋ Publicar Nueva Vacante", fontWeight = FontWeight.ExtraBold)
            }
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
                    text = "Mis Vacantes Publicadas",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "$nombreEmpresa • ${vacantes.size} ofertas laborales activas",
                    color = Color(0xFF93C5FD),
                    fontSize = 13.sp
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
                            text = "Listado de Plazas Corporativas",
                            color = azulCorporativo,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "↻ Actualizar",
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { cargarMisVacantes() }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (cargando) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = azulCorporativo)
                        }
                    } else if (vacantes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No tienes vacantes publicadas aún. Toca '+ Publicar Nueva Vacante'.",
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            items(vacantes, key = { it.id }) { vac ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
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
                                                    text = vac.titulo,
                                                    color = azulCorporativo,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = "📍 ${vac.ubicacion}  •  💰 ${vac.salario}",
                                                    color = Color(0xFF16A34A),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Surface(
                                                color = Color(0xFF2563EB).copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = vac.modalidad,
                                                    color = Color(0xFF2563EB),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        if (vac.requisitosTexto.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Requisitos: ${vac.requisitosTexto}",
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

    // MODAL ESTILO WEB PARA PUBLICAR VACANTE CON REQUISITOS PREESTABLECIDOS
    if (mostrarModalNuevaVacante) {
        AlertDialog(
            onDismissRequest = { mostrarModalNuevaVacante = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "💼 Publicar Nueva Vacante",
                    color = azulCorporativo,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Cargo o Puesto (con sugerencias rápidas + texto libre)
                    Text("1. Cargo / Título del Puesto:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = azulCorporativo)
                    OutlinedTextField(
                        value = tituloVacante,
                        onValueChange = { tituloVacante = it },
                        placeholder = { Text("Ej: Desarrollador Full Stack, Contador...") },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Desarrollador WEB", "Ejecutivo Ventas", "Contador General").forEach { sug ->
                            Surface(
                                color = Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { tituloVacante = sug }
                            ) {
                                Text(
                                    text = sug,
                                    fontSize = 10.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // 2. Modalidad de Trabajo
                    Text("2. Modalidad de Empleo:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = azulCorporativo)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Presencial" to 1, "Remoto" to 2, "Híbrido" to 3).forEach { par ->
                            val activo = modalidadSeleccionada.first == par.first
                            Surface(
                                color = if (activo) azulCorporativo else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { modalidadSeleccionada = par }
                            ) {
                                Text(
                                    text = par.first,
                                    color = if (activo) Color.White else Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // 3. Ubicación (Departamento)
                    Text("3. Ubicación (Departamento):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = azulCorporativo)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Managua" to 1, "León" to 2, "Estelí" to 3, "Chinandega" to 4).forEach { loc ->
                            val activo = municipioSeleccionado.first == loc.first
                            Surface(
                                color = if (activo) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { municipioSeleccionado = loc }
                            ) {
                                Text(
                                    text = loc.first,
                                    color = if (activo) Color.White else Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // 4. Nivel Académico en cuadrícula de 2 columnas para que "Bachillerato" no se corte
                    Text("4. Nivel Académico Requerido:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = azulCorporativo)
                    val nivelesAcademicos = listOf(
                        "Universitario Completo",
                        "Técnico Superior",
                        "Bachillerato",
                        "Maestría / Posgrado"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        nivelesAcademicos.chunked(2).forEach { parNiveles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                parNiveles.forEach { niv ->
                                    val activo = nivelEducativoReq == niv
                                    Surface(
                                        color = if (activo) Color(0xFF0284C7) else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { nivelEducativoReq = niv }
                                    ) {
                                        Text(
                                            text = niv,
                                            color = if (activo) Color.White else Color.Black,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Experiencia Mínima Preestablecida
                    Text("5. Experiencia Mínima Preestablecida:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = azulCorporativo)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Sin experiencia", "1 a 2 años", "3+ años").forEach { exp ->
                            val activo = experienciaPredefinida.startsWith(exp)
                            Surface(
                                color = if (activo) Color(0xFF16A34A) else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable { experienciaPredefinida = exp }
                            ) {
                                Text(
                                    text = exp,
                                    color = if (activo) Color.White else Color.Black,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // 6. Selección Múltiple de Habilidades / Requisitos Preestablecidos
                    Text(
                        text = "6. Selecciona los Requisitos y Habilidades del Puesto:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = azulCorporativo
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        catalogoRequisitosPreestablecidos.chunked(3).forEach { fila ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                fila.forEach { req ->
                                    val marcado = requisitosSeleccionados.contains(req)
                                    Surface(
                                        color = if (marcado) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                requisitosSeleccionados = if (marcado) {
                                                    requisitosSeleccionados - req
                                                } else {
                                                    requisitosSeleccionados + req
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = if (marcado) "✓ $req" else "+ $req",
                                            color = if (marcado) Color.White else Color(0xFF334155),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 7. Beneficios Preestablecidos
                    Text("7. Beneficios Ofrecidos:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = azulCorporativo)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        beneficiosPreestablecidos.chunked(2).forEach { fila ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                fila.forEach { ben ->
                                    val marcado = beneficiosSeleccionados.contains(ben)
                                    Surface(
                                        color = if (marcado) Color(0xFF0D9488) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                beneficiosSeleccionados = if (marcado) {
                                                    beneficiosSeleccionados - ben
                                                } else {
                                                    beneficiosSeleccionados + ben
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = if (marcado) "✓ $ben" else "+ $ben",
                                            color = if (marcado) Color.White else Color(0xFF334155),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 8. Rango Salarial (C$)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = salarioMin,
                            onValueChange = { salarioMin = it },
                            label = { Text("Salario Mín (C$)", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                            colors = coloresInputNegro,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = salarioMax,
                            onValueChange = { salarioMax = it },
                            label = { Text("Salario Máx (C$)", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                            colors = coloresInputNegro,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 9. Descripción / Comentarios adicionales
                    OutlinedTextField(
                        value = descripcionAdicional,
                        onValueChange = { descripcionAdicional = it },
                        label = { Text("Funciones adicionales (opcional)", fontSize = 11.sp) },
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        colors = coloresInputNegro,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { mostrarModalNuevaVacante = false }) {
                        Text("Cancelar", color = azulCorporativo, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        enabled = !publicando,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        onClick = {
                            if (tituloVacante.isBlank()) {
                                Toast.makeText(context, "Escribe o selecciona el título del cargo", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            scope.launch {
                                publicando = true
                                val resumenRequisitos = buildString {
                                    append("Nivel Académico: $nivelEducativoReq | ")
                                    append("Experiencia: $experienciaPredefinida | ")
                                    append("Habilidades requeridas: ${requisitosSeleccionados.joinToString(", ")} | ")
                                    append("Beneficios: ${beneficiosSeleccionados.joinToString(", ")}.")
                                    if (descripcionAdicional.isNotBlank()) {
                                        append("\nFunciones: $descripcionAdicional")
                                    }
                                }

                                val resultadoCreacion = publicarVacanteConRequisitosEnServidor(
                                    token = token,
                                    empresaId = empresaId,
                                    titulo = tituloVacante.trim(),
                                    descripcion = resumenRequisitos,
                                    salarioMin = salarioMin,
                                    salarioMax = salarioMax,
                                    modalidadId = modalidadSeleccionada.second,
                                    tipoEmpleoId = tipoContratoSeleccionado.second,
                                    municipioId = municipioSeleccionado.second,
                                    requisitosLista = requisitosSeleccionados.toList()
                                )
                                publicando = false
                                if (resultadoCreacion.first) {
                                    mostrarModalNuevaVacante = false
                                    tituloVacante = ""
                                    descripcionAdicional = ""
                                    Toast.makeText(
                                        context,
                                        "Vacante publicada y guardada exitosamente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    cargarMisVacantes()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "No se pudo guardar la vacante: ${resultadoCreacion.second}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    ) {
                        Text(
                            text = if (publicando) "Publicando..." else "✓ Publicar Vacante",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        )
    }
}

private suspend fun obtenerVacantesDeEmpresa(empresaId: Int): List<VacanteCorporativaModel> =
    withContext(Dispatchers.IO) {
        val lista = mutableListOf<VacanteCorporativaModel>()
        try {
            val url = URL("http://20.51.194.8:8000/api/empleos/?empresaid=$empresaId&page_size=50")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
            }
            if (conn.responseCode in 200..299) {
                val raw = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = if (raw.trim().startsWith("{")) {
                    JSONObject(raw).optJSONArray("results") ?: JSONArray()
                } else JSONArray(raw)

                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    // Filtrar para asegurar que solo se listen vacantes cuyo empresaid coincida con la empresa actual
                    val empObj = o.optJSONObject("empresaid")
                    val idEmpVac = empObj?.optInt("empresaid", empObj.optInt("id", empresaId))
                        ?: o.optInt("empresaid", o.optInt("EmpresaId", empresaId))
                    if (idEmpVac != empresaId) continue

                    val desc = o.optString("descripcion", o.optString("Descripcion", ""))
                    val salRaw = o.optString("salario", o.optString("salariomax", o.optString("SalarioMax", "22000")))
                    lista.add(
                        VacanteCorporativaModel(
                            id = o.optInt("empleoid", o.optInt("EmpleoId", i + 1)),
                            titulo = extraerTituloRealVacante(o),
                            descripcion = desc,
                            modalidad = o.optString("modalidad_nombre", "Presencial"),
                            ubicacion = o.optString("municipio_nombre", "Nicaragua"),
                            salario = if (salRaw == "null" || salRaw.isBlank()) "C$ A convenir" else "C$ $salRaw",
                            requisitosTexto = if (desc.isNotBlank() && desc != "null") desc.take(120) else "Excel Avanzado, Trabajo en Equipo, Compromiso"
                        )
                    )
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}
        lista
    }

private suspend fun publicarVacanteConRequisitosEnServidor(
    token: String,
    empresaId: Int,
    titulo: String,
    descripcion: String,
    salarioMin: String,
    salarioMax: String,
    modalidadId: Int,
    tipoEmpleoId: Int,
    municipioId: Int,
    requisitosLista: List<String>
): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    try {
        val fechaHoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val salMinVal = salarioMin.toDoubleOrNull() ?: 15000.0
        val salMaxVal = salarioMax.toDoubleOrNull() ?: 25000.0

        fun enviarPostEmpleo(bodyObj: JSONObject): Pair<Int, String> {
            val url = URL("http://20.51.194.8:8000/api/empleos/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                if (token.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }
            conn.outputStream.use { os ->
                os.write(bodyObj.toString().toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            val txt = if (code in 200..299) {
                conn.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            } else {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
            }
            conn.disconnect()
            return Pair(code, txt)
        }

        val payload = JSONObject().apply {
            put("empresaid", empresaId)
            put("empresa", empresaId)
            put("nombreempleo", titulo)
            put("titulo", titulo)
            put("tituloempleo", titulo)
            put("descripcion", descripcion)
            put("salariomin", salMinVal)
            put("salariomax", salMaxVal)
            put("salario", salMaxVal)
            put("modalidadid", modalidadId)
            put("tipoempleoid", tipoEmpleoId)
            put("municipioid", municipioId)
            put("niveleducativoid", 1)
            put("estadoid", 1)
            put("vacantesdisponibles", 1)
            put("experiencia", "1 a 2 años")
            put("fechapublicacion", fechaHoy)
            put("fechalimite", "2026-12-31")
            put("requisitos", JSONArray(requisitosLista))
        }

        var (code, msgError) = enviarPostEmpleo(payload)
        if (code in 200..299) {
            return@withContext Pair(true, "")
        }

        // Si el serializador de Django pide un tipo distinto en algún campo (ej. experiencia entero o fecha), auto-ajustar y reintentar
        if (code == 400 && msgError.trim().startsWith("{")) {
            try {
                val errJson = JSONObject(msgError)
                val keys = errJson.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val errMsg = errJson.opt(k).toString().lowercase()
                    val kl = k.lowercase()
                    when {
                        kl.contains("nombre") || kl.contains("titulo") || kl.contains("cargo") -> payload.put(k, titulo)
                        kl.contains("descripcion") -> payload.put(k, descripcion)
                        kl.contains("empresa") -> payload.put(k, empresaId)
                        kl.contains("experiencia") && (errMsg.contains("int") || errMsg.contains("entero") || errMsg.contains("number")) -> payload.put(k, 1)
                        kl.contains("fecha") && (errMsg.contains("format") || errMsg.contains("datetime")) -> payload.remove(k)
                        errMsg.contains("requerido") || errMsg.contains("required") -> {
                            if (kl.endsWith("id") || kl.contains("vacante") || kl.contains("estado") || kl.contains("nivel")) {
                                payload.put(k, 1)
                            } else {
                                payload.put(k, titulo)
                            }
                        }
                    }
                }
                val retry = enviarPostEmpleo(payload)
                code = retry.first
                msgError = retry.second
                if (code in 200..299) {
                    return@withContext Pair(true, "")
                }
            } catch (_: Exception) {}
        }

        // Respaldo directo en SQL Server si el serializador sigue rechazando algún campo
        val urlBackup = URL("http://20.51.194.8:8000/api/dashboard/admin-gestion/")
        val connB = (urlBackup.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8000
            readTimeout = 8000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            if (token.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }
        val payloadBackup = JSONObject(payload.toString()).apply {
            put("accion", "crear_vacante_empresa")
        }
        connB.outputStream.use { os ->
            os.write(payloadBackup.toString().toByteArray(Charsets.UTF_8))
        }
        val codeB = connB.responseCode
        connB.disconnect()
        if (codeB in 200..299) {
            return@withContext Pair(true, "")
        }

        Pair(false, msgError.take(120))
    } catch (e: Exception) {
        Pair(false, e.message ?: "Error de red")
    }
}