package com.example.impulsonica

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Empresa_perfilActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PantallaPerfilEmpresaUnificada()
            }
        }
    }
}

@Composable
fun PantallaPerfilEmpresaUnificada() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs = context.getSharedPreferences("ImpulsoNicaPrefs", Context.MODE_PRIVATE)
    val empresaId = prefs.getInt("empresa_id", prefs.getInt("usuario_sql_id", 2))
    val token = prefs.getString("access_token", prefs.getString("jwt_token", "")) ?: ""

    val nombreGuardado = prefs.getString("nombre_empresa", "Casa Pellas") ?: "Casa Pellas"
    var nombreEmpresa by remember {
        mutableStateOf(if (nombreGuardado == "Empresa Corporativa") "Casa Pellas" else nombreGuardado)
    }
    var sectorEmpresa by remember { mutableStateOf("Comercio y Retail") }
    var correoEmpresa by remember { mutableStateOf(prefs.getString("correo_usuario", "grupopellas@org.com.ni") ?: "grupopellas@org.com.ni") }
    var ubicacionEmpresa by remember { mutableStateOf("León, León") }
    var telefonoEmpresa by remember { mutableStateOf("+505 72285617") }
    var direccionEmpresa by remember { mutableStateOf("Centro Corporativo") }

    var mostrarModalEditar by remember { mutableStateOf(false) }

    LaunchedEffect(empresaId) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL("http://20.51.194.8:8000/api/empresas/$empresaId/")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    if (token.isNotBlank()) {
                        setRequestProperty("Authorization", "Bearer $token")
                    }
                }
                if (conn.responseCode in 200..299) {
                    val o = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    val nom = o.optString("nombreempresa", o.optString("NombreEmpresa", ""))
                    if (nom.isNotBlank() && nom != "null") {
                        nombreEmpresa = nom
                        prefs.edit().putString("nombre_empresa", nom).apply()
                    }
                    val cor = o.optString("correo", o.optString("Correo", ""))
                    if (cor.isNotBlank() && cor != "null") correoEmpresa = cor
                    val tel = o.optString("telefono", o.optString("Telefono", ""))
                    if (tel.isNotBlank() && tel != "null") telefonoEmpresa = tel
                    val dir = o.optString("direccion", o.optString("Direccion", ""))
                    if (dir.isNotBlank() && dir != "null") direccionEmpresa = dir
                    val mun = o.optString("municipio_nombre", "")
                    if (mun.isNotBlank() && mun != "null") ubicacionEmpresa = mun
                    val sec = o.optString("sector_nombre", "")
                    if (sec.isNotBlank() && sec != "null") sectorEmpresa = sec
                }
                conn.disconnect()
            } catch (_: Exception) {}
        }
    }

    val azulOscuro = Color(0xFF0A2E5C)
    val fondoGris = Color(0xFFF4F6F9)

    Scaffold(
        containerColor = azulOscuro,
        bottomBar = {
            // Misma barra inferior unificada que Empresa_homeActivity, Empresa_vacantesActivity y Empresa_notificacionesActivity
            BarraInferiorEmpresa(context = context, itemActivo = 3)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(88.dp),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val logoId = remember {
                            obtenerLogoImpulsoNicaDrawableId(context)
                        }
                        if (logoId != 0) {
                            Image(
                                painter = painterResource(id = logoId),
                                contentDescription = null,
                                modifier = Modifier.size(68.dp)
                            )
                        } else {
                            IconoVectorialDelineado(tipo = "work", tint = azulOscuro, modifier = Modifier.size(44.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nombreEmpresa,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = sectorEmpresa,
                        color = Color(0xFF93C5FD),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = correoEmpresa,
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "📍 $ubicacionEmpresa | 📞 $telefonoEmpresa",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { mostrarModalEditar = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text("✏️ Editar Perfil Empresa", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = fondoGris,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OpcionMenuEmpresaNuevo(
                        iconoTipo = "profile",
                        titulo = "Modificar Datos de la Empresa",
                        onClick = { mostrarModalEditar = true }
                    )

                    OpcionMenuEmpresaNuevo(
                        iconoTipo = "work",
                        titulo = "Publicar Nueva Vacante",
                        onClick = {
                            context.startActivity(Intent(context, Empresa_vacantesActivity::class.java))
                        }
                    )

                    OpcionMenuEmpresaNuevo(
                        iconoTipo = "search",
                        titulo = "Ver Mis Vacantes Publicadas",
                        onClick = {
                            context.startActivity(Intent(context, Empresa_vacantesActivity::class.java))
                        }
                    )

                    OpcionMenuEmpresaNuevo(
                        iconoTipo = "bell",
                        titulo = "Notificaciones de Postulantes",
                        onClick = {
                            context.startActivity(Intent(context, Empresa_notificacionesActivity::class.java))
                        }
                    )

                    OpcionMenuEmpresaNuevo(
                        iconoTipo = "home",
                        titulo = "Cerrar sesión",
                        esDestructivo = true,
                        onClick = {
                            prefs.edit().clear().apply()
                            val i = Intent(context, MainActivity::class.java)
                            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(i)
                        }
                    )
                }
            }
        }
    }

    if (mostrarModalEditar) {
        AlertDialog(
            onDismissRequest = { mostrarModalEditar = false },
            containerColor = Color.White,
            title = {
                Text("Modificar Datos de la Empresa", fontWeight = FontWeight.ExtraBold, color = azulOscuro)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nombreEmpresa,
                        onValueChange = { nombreEmpresa = it },
                        label = { Text("Nombre de la Empresa") },
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = telefonoEmpresa,
                        onValueChange = { telefonoEmpresa = it },
                        label = { Text("Teléfono Corporativo") },
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = direccionEmpresa,
                        onValueChange = { direccionEmpresa = it },
                        label = { Text("Dirección") },
                        textStyle = TextStyle(color = Color.Black, fontWeight = FontWeight.SemiBold)
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = azulOscuro),
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                try {
                                    val url = URL("http://20.51.194.8:8000/api/empresas/$empresaId/")
                                    val conn = (url.openConnection() as HttpURLConnection).apply {
                                        requestMethod = "PATCH"
                                        connectTimeout = 6000
                                        readTimeout = 6000
                                        doOutput = true
                                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                                        if (token.isNotBlank()) {
                                            setRequestProperty("Authorization", "Bearer $token")
                                        }
                                    }
                                    val body = JSONObject().apply {
                                        put("nombreempresa", nombreEmpresa.trim())
                                        put("telefono", telefonoEmpresa.trim())
                                        put("direccion", direccionEmpresa.trim())
                                    }
                                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                                    conn.responseCode
                                    conn.disconnect()
                                } catch (_: Exception) {}
                            }
                            prefs.edit().putString("nombre_empresa", nombreEmpresa.trim()).apply()
                            mostrarModalEditar = false
                            Toast.makeText(context, "Datos corporativos actualizados", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Guardar Cambios", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarModalEditar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun OpcionMenuEmpresaNuevo(
    iconoTipo: String,
    titulo: String,
    esDestructivo: Boolean = false,
    onClick: () -> Unit
) {
    val colorTexto = if (esDestructivo) Color(0xFFDC2626) else Color(0xFF0A2E5C)
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoVectorialDelineado(
                    tipo = iconoTipo,
                    tint = colorTexto,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = titulo,
                    color = colorTexto,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Text("❯", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}