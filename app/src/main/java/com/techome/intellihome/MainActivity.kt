package com.techome.intellihome

import androidx.compose.material3.MaterialTheme
import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var requestNotificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solicitar permisos de notificación
        requestNotificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permisos de notificación concedidos", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permisos de notificación denegados", Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginScreen(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }

        // Verificar permisos de notificación si es Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermission()
        }
    }

    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Solicitar permisos de notificación
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoggedIn by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    if (isLoggedIn) {
        AdminMenuScreen(context = context)
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(16.dp)) {
                        if (username.isEmpty()) Text("Usuario")
                        innerTextField()
                    }
                }
            )
            BasicTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                visualTransformation = PasswordVisualTransformation(),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(16.dp)) {
                        if (password.isEmpty()) Text("Contraseña")
                        innerTextField()
                    }
                }
            )
            Button(onClick = {
                val savedPassword = readPasswordFromFile(context)
                if (username == "Admin" && password == savedPassword) {
                    isLoggedIn = true
                    startPasswordReminder(context)
                } else {
                    Toast.makeText(
                        context,
                        "Contraseña o usuario incorrectos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }) {
                Text("Login")
            }
        }
    }
}

@Composable
fun AdminMenuScreen(context: android.content.Context) {
    var appEnabled by remember { mutableStateOf(true) }
    var newPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bienvenido, Admin")
        Spacer(modifier = Modifier.height(16.dp))

        Text("Habilitar/deshabilitar App")
        Switch(
            checked = appEnabled,
            onCheckedChange = {
                appEnabled = it
                Toast.makeText(
                    context,
                    if (appEnabled) "App habilitada" else "App deshabilitada",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo para cambiar la contraseña
        Text("Cambiar Contraseña:")
        BasicTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (newPassword.isEmpty()) Text("Nueva Contraseña")
                    innerTextField()
                }
            }
        )

        Button(onClick = {
            if (newPassword.isNotEmpty()) {
                writePasswordToFile(context, newPassword)
                Toast.makeText(context, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                newPassword = "" // Limpiar campo
            }
        }) {
            Text("Actualizar Contraseña")
        }
    }
}

// Función para recordar al admin que cambie la contraseña cada 2 minutos usando notificaciones
fun startPasswordReminder(context: android.content.Context) {
    val handler = Handler(Looper.getMainLooper())

    // Crear el canal de notificaciones
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "PASSWORD_REMINDER",
            "Recordatorio de Contraseña",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Recordatorio para cambiar la contraseña predeterminada"
        }
        val notificationManager: NotificationManager =
            context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    val reminderRunnable = object : Runnable {
        @SuppressLint("MissingPermission")
        override fun run() {
            val builder = NotificationCompat.Builder(context, "PASSWORD_REMINDER")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Recordatorio de contraseña")
                .setContentText("Por favor cambie su contraseña predeterminada")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            with(NotificationManagerCompat.from(context)) {
                notify(1, builder.build())
            }

            // Repetir la llamada cada 2 minutos (120000 ms)
            handler.postDelayed(this, 120000)
        }
    }

    // Iniciar el recordatorio
    handler.post(reminderRunnable)
}

// Función para leer la contraseña desde un archivo
fun readPasswordFromFile(context: android.content.Context): String {
    val file = File(context.filesDir, "password.txt")
    return if (file.exists()) {
        file.readText()
    } else {
        "TEC2024" // Contraseña predeterminada si no existe el archivo
    }
}

// Función para escribir la contraseña en un archivo
fun writePasswordToFile(context: android.content.Context, password: String) {
    val file = File(context.filesDir, "password.txt")
    file.writeText(password)
}
