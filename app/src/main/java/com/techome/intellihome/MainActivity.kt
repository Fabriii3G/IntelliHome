package com.techome.intellihome

import android.util.Log
import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.io.File
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import androidx.compose.material3.OutlinedTextField


import java.util.*
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage


class MainActivity : ComponentActivity() {
    companion object {
        val globalHouseList = mutableListOf<HouseDetails>()
        var cardInfo = CardInfo()
        var tempUser: User? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        // Asegurarse de que el usuario Admin esté en el archivo
        addDefaultAdminUser()

        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var currentScreen by rememberSaveable { mutableStateOf("login") }

                    // Función para cambiar la pantalla
                    fun changeScreen(screen: String) {
                        currentScreen = screen
                    }

                    when (currentScreen) {
                        "login" -> LoginScreen(
                            modifier = Modifier.padding(innerPadding),
                            onForgotPassword = { changeScreen("requestEmail") },
                            onRegisterClick = { changeScreen("register") }  // Navega a la pantalla de registro
                        )
                        "requestEmail" -> RequestPasswordResetScreen(
                            onEmailSent = { changeScreen("verifyCode") }
                        )
                        "verifyCode" -> VerifyRecoveryCodeScreen(
                            onCodeVerified = { changeScreen("resetPassword") },
                            onError = { changeScreen("requestEmail") }
                        )
                        "resetPassword" -> ResetPasswordScreen(
                            onPasswordReset = { changeScreen("login") }
                        )
                        "register" -> RegisterUserScreen(
                            onBack = { changeScreen("login") },  // Vuelve a la pantalla de login
                            onPaymentClick = { changeScreen("payment") }  // Navega a la pantalla de pago
                        )
                        "payment" -> PaymentScreen(
                            onBack = { changeScreen("register") }  // Función para regresar a la pantalla de registro
                        )
                    }
                }
            }
        }
    }
    // Función para agregar el usuario Admin con la contraseña predeterminada si no existe en el archivo
    private fun addDefaultAdminUser() {
        val file = File(filesDir, "users.txt")
        if (!file.exists() || !file.readText().contains("Admin")) {
            file.appendText("Admin,Administrador,admin@tec.com,TEC2024,30,Apartamento Inteligente,Carro\n")
        }
    }
}


data class User(
    var alias: String,
    var fullName: String,
    var email: String,
    var password: String,
    val dateOfBirth: String,
    var houseType: String,
    var vehicleType: String,
)

data class CardInfo(
    var cardholderName: String = "",
    var cardNumber: String = "",
    var expiryDate: String = "",
    var securityCode: String = ""
)

data class HouseDetails(
    val capacity: Int,
    val rooms: Int,
    val bathrooms: Int,
    val amenities: String,
    val generalFeatures: String,
    val photos: List<Uri>,  // Uri para las imágenes de la casa
    val paymentPlan: String,
    //val latitude: Double,
    //val longitude: Double
    val Devices: MutableList<IOT> = mutableListOf<IOT>(),
    var Iot: IOT,
    //var IOTDevice: String = "",
    //var TypeOfDevice: String = "",
    //var LocationAtHouse: String = "",
)

data class IOT(
    var Device: String = "",
    var Type: String = "",
    var Location: String = "",
    var Activated: Boolean
)
var isAppEnabled by mutableStateOf(true) // Variable para habilitar/deshabilitar el aplicativo
var isLoggedIn by mutableStateOf(false)  // Variable global para estado de sesión
var isAdmin by mutableStateOf(false)     // Variable global para saber si el usuario es Admin
var currentUser by mutableStateOf(User("A", "B", "C", "D", "E", "F", "G"))

// Función para validar contraseñas
fun isPasswordValid(password: String): Boolean {
    val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=!])(?=\\S+\$).{8,}\$"
    val passwordMatcher = Regex(passwordPattern)
    return passwordMatcher.matches(password)
}




// Función para guardar los datos de usuario en un archivo
fun saveUserToFile(context: android.content.Context, user: User) {
    val file = File(context.filesDir, "users.txt")
    Log.d("UserFilePath", "Ruta del archivo: ${file.absolutePath}")


    try {
        // Verifica si el archivo existe, si no, lo crea
        if (!file.exists()) {
            val isFileCreated = file.createNewFile()
            println("Archivo creado: $isFileCreated en ${file.absolutePath}")
        }

        // Comprobación de contenido duplicado
        val lines = file.readLines()
        val userExists = lines.any { it.startsWith(user.alias) }

        if (!userExists) {
            file.appendText("${user.alias},${user.fullName},${user.email},${user.password},${user.dateOfBirth},${user.houseType},${user.vehicleType}\n")
            println("Usuario '${user.alias}' guardado correctamente.")
        } else {
            println("El usuario '${user.alias}' ya existe.")
        }

    } catch (e: Exception) {
        e.printStackTrace()
        println("Error al guardar el usuario: ${e.message}")
    }
}

fun deleteUserFromFile(context: android.content.Context, userAlias: String) {
    val file = File(context.filesDir, "users.txt")
    Log.d("UserFilePath", "Ruta del archivo: ${file.absolutePath}")


    if (file.exists()) {
        // Leer todas las líneas del archivo
        val lines = file.readLines().toMutableList()

        // Filtrar las líneas que no corresponden al usuario que deseas eliminar
        val updatedLines = lines.filter { !it.startsWith("$userAlias,") }

        // Sobrescribir el archivo con las líneas actualizadas
        file.writeText(updatedLines.joinToString("\n"))
    }
}


// Función para actualizar la contraseña del Admin en el archivo
fun updateAdminPassword(context: android.content.Context, newPassword: String) {
    val file = File(context.filesDir, "users.txt")
    Log.d("UserFilePath", "Ruta del archivo: ${file.absolutePath}")

    if (file.exists()) {
        val lines = file.readLines()
        val newLines = lines.map { line ->
            if (line.startsWith("Admin")) {
                val parts = line.split(",")
                "Admin,${parts[1]},${parts[2]},$newPassword,${parts[4]},${parts[5]},${parts[6]}"
            } else {
                line
            }
        }
        file.writeText(newLines.joinToString("\n"))
    }
}

// Función para hacer a un usuario administrador
fun makeUserAdmin(context: android.content.Context, user: User) {
    val file = File(context.filesDir, "users.txt")
    Log.d("UserFilePath", "Ruta del archivo: ${file.absolutePath}")

    if (file.exists()) {
        val lines = file.readLines()
        val newLines = lines.map { line ->
            val parts = line.split(",")
            if (parts[0] == user.alias) {
                "${user.alias},Administrador,${user.email},${user.password},${user.dateOfBirth},${user.houseType},${user.vehicleType}"
            } else {
                line
            }
        }
        file.writeText(newLines.joinToString("\n"))
    }
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onForgotPassword: () -> Unit,
    onRegisterClick: () -> Unit // Se agrega este parámetro para manejar la navegación
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    if (isLoggedIn) {
        if (isAdmin) {
            AdminMenuScreen(context = context)
        } else {
            UserMenuScreen()
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mostrar mensaje si el aplicativo está deshabilitado
            if (!isAppEnabled) {
                Text("El aplicativo está deshabilitado. Solo el administrador puede iniciar sesión.", Modifier.padding(16.dp))
            }

            // Campo para el nombre de usuario
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

            // Campo para la contraseña
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

            // Botón de login
            Button(onClick = {
                val userData = readUserDataFromFile(context, username)
                currentUser = readUserDataFromFile(context, username)

                // Permitir solo al administrador iniciar sesión si la app está deshabilitada
                if (!isAppEnabled && username != "Admin") {
                    Toast.makeText(context, "El aplicativo está deshabilitado. Solo el administrador puede iniciar sesión.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (username == "Admin" && password == "TEC2024") {
                    isLoggedIn = true
                    isAdmin = true
                    Toast.makeText(context, "Ingreso exitoso como Admin", Toast.LENGTH_SHORT).show()
                    startPasswordReminder(context) // Inicia recordatorio de contraseña
                } else if (userData.password == password) {
                    isLoggedIn = true
                    isAdmin = false
                    Toast.makeText(context, "Ingreso exitoso", Toast.LENGTH_SHORT).show()
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

            // Mostrar el botón de registro solo si la app está habilitada
            if (isAppEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRegisterClick) {  // Llama a onRegisterClick en lugar de modificar showRegister
                    Text("Registrar nuevo usuario")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onForgotPassword) {
                Text("¿Olvidaste tu contraseña?")
            }
        }
    }
}


//Funcion para poder previsualizar el codigo
@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    LoginScreen(
        onForgotPassword = {},
        onRegisterClick = {}
    )
}


@Composable
fun RequestPasswordResetScreen(onEmailSent: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Recuperar contraseña")

        BasicTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (email.isEmpty()) Text("Correo electrónico")
                    innerTextField()
                }
            }
        )

        Button(onClick = {
            val user = readUserDataByEmail(context, email)
            if (user != null) {
                sendRecoveryCode(context, email)
                onEmailSent() // Navegar a la pantalla de verificación del código
            } else {
                Toast.makeText(context, "Correo no encontrado", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Enviar código")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRequestPasswordResetScreen() {
    RequestPasswordResetScreen(onEmailSent = {})
}

@Composable
fun VerifyRecoveryCodeScreen(onCodeVerified: () -> Unit, onError: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ingrese el código de recuperación")

        BasicTextField(
            value = code,
            onValueChange = { code = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (code.isEmpty()) Text("Código de recuperación")
                    innerTextField()
                }
            }
        )

        Button(onClick = {
            val sharedPreferences = context.getSharedPreferences("RecoveryPrefs", Context.MODE_PRIVATE)
            val savedCode = sharedPreferences.getString("recovery_code", "")

            if (code == savedCode) {
                onCodeVerified() // Navegar a la pantalla para cambiar contraseña
            } else {
                onError() // Manejar el error
            }
        }) {
            Text("Verificar código")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewVerifyRecoveryCodeScreen() {
    VerifyRecoveryCodeScreen(onCodeVerified = {}, onError = {})
}

@Composable
fun ResetPasswordScreen(onPasswordReset: () -> Unit) {
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Cambiar Contraseña")

        BasicTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            visualTransformation = PasswordVisualTransformation(),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (newPassword.isEmpty()) Text("Nueva contraseña")
                    innerTextField()
                }
            }
        )

        BasicTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            visualTransformation = PasswordVisualTransformation(),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (confirmPassword.isEmpty()) Text("Confirmar contraseña")
                    innerTextField()
                }
            }
        )

        Button(onClick = {
            if (newPassword == confirmPassword) {
                saveNewPassword(context, newPassword)
                onPasswordReset() // Navegar de vuelta al login
            } else {
                Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Cambiar contraseña")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewResetPasswordScreen() {
    ResetPasswordScreen(onPasswordReset = {})
}


// Pantalla del menú de administrador
@Composable
fun AdminMenuScreen(context: Context) {
    var newPassword by rememberSaveable { mutableStateOf("") }
    val users = remember { mutableStateListOf<User>() }
    var showAddHouseScreen by remember { mutableStateOf(false) } // Control para navegar a AddHouseScreen
    var showEditHousesScreen by remember { mutableStateOf(false) }

    // Leer todos los usuarios al iniciar la pantalla
    LaunchedEffect(Unit) {
        users.clear()
        users.addAll(readAllUsers(context))
    }

    if (showAddHouseScreen) {
        // Mostrar la pantalla para agregar casa
        AddHouseScreen(onBack = { showAddHouseScreen = false })
    } else if (showEditHousesScreen){
            EditHousesScreen(onBack = {showEditHousesScreen = false})
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Bienvenido, Admin")

            // Botón para habilitar/deshabilitar el aplicativo
            Button(onClick = {
                isAppEnabled = !isAppEnabled
                Toast.makeText(
                    context,
                    if (isAppEnabled) "Aplicativo habilitado" else "Aplicativo deshabilitado",
                    Toast.LENGTH_SHORT
                ).show()
            }) {
                Text(if (isAppEnabled) "Deshabilitar aplicativo" else "Habilitar aplicativo")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo para cambiar la contraseña
            BasicTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(16.dp)) {
                        if (newPassword.isEmpty()) Text("Nueva contraseña")
                        innerTextField()
                    }
                }
            )

            // Botón para actualizar la contraseña
            Button(onClick = {
                if (newPassword.isNotEmpty()) {
                    updateAdminPassword(context, newPassword)
                    Toast.makeText(context, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                    newPassword = "" // Limpiar el campo después de actualizar
                } else {
                    Toast.makeText(context, "Por favor ingrese una nueva contraseña", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Cambiar Contraseña")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de usuarios
            Text("Lista de usuarios:")
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(users) { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${user.alias} - ${user.fullName}", modifier = Modifier.weight(1f))
                        Button(onClick = {
                            if (user.alias != "Admin") {
                                makeUserAdmin(context, user)
                                Toast.makeText(context, "${user.alias} ahora es administrador", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Hacer Admin")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para cerrar sesión
            Button(onClick = {
                isLoggedIn = false
                isAdmin = false
            }) {
                Text("Cerrar sesión")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nuevo botón para agregar una casa
             Button(onClick = { showAddHouseScreen = true }) {
                Text("Agregar Nueva Casa")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para editar casas
            Button(onClick = { showEditHousesScreen = true }) {
                Text("Editar Casas")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAdminMenuScreen() {
    val context = LocalContext.current // Necesario para el contexto
    AdminMenuScreen(context = context)
}

//----------------------------------------------------------------------------------------------------------------
@Composable
fun EditHousesScreen(onBack: () -> Unit) {
    var selectedHouse by remember { mutableStateOf<HouseDetails?>(null) } // Casa seleccionada para edición
    var showIOTDevicesScreen by remember { mutableStateOf(false) }
    var showEditHouseDetailsScreen by remember { mutableStateOf(false) }  // Control para mostrar EditHouseDetailsScreen

    if (showEditHouseDetailsScreen && selectedHouse != null) {
        EditHouseDetailsScreen(
            house = selectedHouse!!,
            onSave = { updatedHouse ->
                val index = MainActivity.globalHouseList.indexOf(selectedHouse)
                if (index >= 0) {
                    MainActivity.globalHouseList[index] = updatedHouse // Actualizar la casa en la lista
                }
                selectedHouse = null
                showEditHouseDetailsScreen = false
            },
            onCancel = {
                selectedHouse = null
                showEditHouseDetailsScreen = false
            }
        )
    }else if (showIOTDevicesScreen && selectedHouse != null){
        showIOTDevicesScreen(
            house = selectedHouse!!,
            onSave = { updatedHouse ->
                val index = MainActivity.globalHouseList.indexOf(selectedHouse)
                if (index >= 0) {
                    MainActivity.globalHouseList[index] = updatedHouse // Actualizar la casa en la lista
                }
                selectedHouse = null
                showEditHouseDetailsScreen = false
            },
            onCancel = {
                selectedHouse = null
                showEditHouseDetailsScreen = false
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Editar Casas")

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(MainActivity.globalHouseList) { house ->
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Capacidad: ${house.capacity}")
                        Text("Habitaciones: ${house.rooms}")
                        Text("Baños: ${house.bathrooms}")
                        Text("Amenities: ${house.amenities}")
                        Text("Características Generales: ${house.generalFeatures}")
                        Text("Plan de Pago: ${house.paymentPlan}")

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            selectedHouse = house // Seleccionar la casa actual
                            showEditHouseDetailsScreen = true // Mostrar pantalla de edición
                        }) {
                            Text("Agregar dispositivos")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            selectedHouse = house // Seleccionar la casa actual
                            showIOTDevicesScreen = true
                        }) {
                            Text("Ver dispositivos")
                        }

                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onBack) {
                Text("Volver")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEditHouseScreen() {
    EditHousesScreen(onBack={})
}

@Composable
fun showIOTDevicesScreen(house: HouseDetails, onSave: (HouseDetails) -> Unit, onCancel: () -> Unit){
    val index = MainActivity.globalHouseList.indexOf(house)
    var selectedDevice by remember { mutableStateOf<IOT?>(null) } // Casa seleccionada para edición
    var showEditIotsScreen by remember { mutableStateOf(false) }  // Control para mostrar EditHouseDetailsScreen

    if (showEditIotsScreen && selectedDevice != null) {
        EditIotsScreen(
            device = selectedDevice!!,
            onSave = { updated ->
                val index1 = MainActivity.globalHouseList[index].Devices.indexOf(selectedDevice)
                if (index1 >= 0) {
                    MainActivity.globalHouseList[index].Devices[index1] = updated // Actualizar la casa en la lista
                }
                selectedDevice = null
                showEditIotsScreen = false
            },
            onCancel = {
                selectedDevice = null
                showEditIotsScreen = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Editar Casas")

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(MainActivity.globalHouseList[index].Devices) { device ->
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Dispositivo IOT: ${device.Device}")
                    Text("Tipo de dispositivo: ${device.Type}")
                    Text("Ubicacion: ${device.Location}")

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        selectedDevice = device // Seleccionar la casa actual
                        showEditIotsScreen = true // Mostrar pantalla de edición
                    }) {
                        Text("Editar")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onCancel) {
            Text("Volver")
        }
    }
}



@Composable
    fun EditIotsScreen(device: IOT, onSave: (IOT) -> Unit, onCancel: () -> Unit){
    var deviceName by rememberSaveable { mutableStateOf(device.Device) }
    var deviceType by rememberSaveable { mutableStateOf(device.Type) }
    var location by rememberSaveable { mutableStateOf(device.Location) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Editar Detalles de la Casa")

        Spacer(modifier = Modifier.height(16.dp))

        // Campos de texto para los detalles adicionales
        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text("Nombre del dispositivo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = deviceType,
            onValueChange = { deviceType = it },
            label = { Text("Tipo de dispositivo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Ubicación en la casa") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para guardar cambios
        Button(onClick = {
            device.Device = deviceName
            device.Type = deviceType
            device.Location = location
        }) {
            Text("Guardar Cambios")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onCancel) {
            Text("Volver")
        }
    }
}

@Composable
fun EditHouseDetailsScreen(house: HouseDetails, onSave: (HouseDetails) -> Unit, onCancel: () -> Unit) {
    var deviceName by rememberSaveable { mutableStateOf("") }
    var deviceType by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Editar Detalles de la Casa")

        Spacer(modifier = Modifier.height(16.dp))

        // Campos de texto para los detalles adicionales
        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text("Nombre del dispositivo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = deviceType,
            onValueChange = { deviceType = it },
            label = { Text("Tipo de dispositivo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Ubicación en la casa") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para guardar cambios
        Button(onClick = {
            val index = MainActivity.globalHouseList.indexOf(house)
            var IotDevice = IOT(deviceName, deviceType, location, true)
            MainActivity.globalHouseList[index].Devices.add(IotDevice)

            MainActivity.globalHouseList[index].Iot.Device = deviceName
            MainActivity.globalHouseList[index].Iot.Type = deviceType
            MainActivity.globalHouseList[index].Iot.Location = location
        }) {
            Text("Guardar Cambios")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onCancel) {
            Text("Volver")
        }
    }
}


@Composable
fun AddHouseScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit, // Callback para volver al menú
    maxPhotos: Int = 3 // Número máximo de fotos permitido (por defecto: 3)
) {
    // Variables para los campos del formulario
    var capacity by remember { mutableStateOf("") }
    var rooms by remember { mutableStateOf("") }
    var bathrooms by remember { mutableStateOf("") }
    var amenities by remember { mutableStateOf("") }
    var generalFeatures by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf(mutableListOf<Uri>()) }
    val selectedImages = remember { mutableStateListOf<Uri>() }
    val context = LocalContext.current
    // Variable para el plan de pago seleccionado
    var selectedPlan by remember { mutableStateOf("Cuota mensual con servicios básicos") }
    var expanded by remember { mutableStateOf(false) } // Controla si el menú está abierto
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var showSelectLocation by remember { mutableStateOf(false) }


    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (selectedImages.size < maxPhotos) {
                selectedImages.add(it) // Agrega la imagen seleccionada
            }
        }
    }

    val paymentPlans = listOf(
        "Cuota mensual con servicios básicos",
        "Cuota mensual sin servicios básicos",
        "Cuota diaria con servicios básicos"
    )

    if (showSelectLocation) {
        SelectLocationScreen(
            onLocationSelected = { location ->
                selectedLocation = location
                showSelectLocation = false // Volver a la pantalla de agregar casa
            }
        )
    } else {

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Formulario de detalles importantes
            Text("Agregar nueva casa")

            Spacer(modifier = Modifier.height(16.dp))

            BasicTextField(
                value = capacity,
                onValueChange = { capacity = it },
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(16.dp)) {
                        if (capacity.isEmpty()) Text("Capacidad de personas")
                        innerTextField()
                    }
                }
            )

            BasicTextField(
                value = rooms,
                onValueChange = { rooms = it },
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(16.dp)) {
                        if (rooms.isEmpty()) Text("Cantidad de habitaciones")
                        innerTextField()
                    }
                }
            )

            BasicTextField(
                value = bathrooms,
                onValueChange = { bathrooms = it },
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(16.dp)) {
                        if (bathrooms.isEmpty()) Text("Cantidad de banos")
                        innerTextField()
                    }
                }
            )

            BasicTextField(
                value = amenities,
                onValueChange = { amenities = it },
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(16.dp)) {
                        if (amenities.isEmpty()) Text("Amenidades")
                        innerTextField()
                    }
                }
            )

            BasicTextField(
                value = generalFeatures,
                onValueChange = { generalFeatures = it },
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.padding(16.dp)) {
                        if (generalFeatures.isEmpty()) Text("Características generales")
                        innerTextField()
                    }
                }
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { expanded = true }) {
                    Text(selectedPlan) // Muestra el plan seleccionado
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false } // Cierra el menú al hacer clic fuera
                ) {
                    paymentPlans.forEach { plan ->
                        DropdownMenuItem(
                            text = { Text(plan) },
                            onClick = {
                                selectedPlan = plan // Actualiza el plan seleccionado
                                expanded = false // Cierra el menú
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                showSelectLocation = true // Cambiar a la pantalla de selección de ubicación
            }) {
                Text("Seleccionar ubicación")
            }

            selectedLocation?.let {
                Text("Ubicación seleccionada: ${it.latitude}, ${it.longitude}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección para subir imágenes
            Text("Subir imagenes de la casa (Maximo: $maxPhotos fotos)")

            if (selectedImages.size < maxPhotos) {
                Button(onClick = {
                    launcher.launch("image/*") // Abre el selector de imágenes
                }) {
                    Text("Agregar imagen")
                }
            }

            LazyColumn {
                items(selectedImages) { imageUri ->
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .width(100.dp)  // Ajusta el ancho
                            .height(100.dp) // Ajusta el alto
                            .padding(8.dp)  // Espacio alrededor de la imagen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para guardar la casa
            Button(onClick = {
                if (capacity.isNotEmpty() && rooms.isNotEmpty() && bathrooms.isNotEmpty() &&
                    amenities.isNotEmpty()
                ) {
                    // Guardar la casa si todo es válido
                    val newHouse = HouseDetails(
                        capacity = capacity.toInt(),
                        rooms = rooms.toInt(),
                        bathrooms = bathrooms.toInt(),
                        amenities = amenities,
                        generalFeatures = generalFeatures,
                        photos = selectedImages.toList(),
                        paymentPlan = selectedPlan,
                        Iot = IOT("", "", "", true)
                        //latitude = selectedLocation!!.latitude,
                        //longitude = selectedLocation!!.longitude
                    )
                    // Agregar la nueva casa a la lista global
                    MainActivity.globalHouseList.add(newHouse)

                    // Mostrar confirmación
                    Toast.makeText(context, "Casa agregada con éxito", Toast.LENGTH_SHORT).show()

                    onBack()


                } else {
                    // Mostrar error si faltan datos
                    Toast.makeText(
                        context,
                        "Complete todos los campos y agregue al menos $maxPhotos imágenes",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }) {
                Text("Guardar casa")
            }

        }
    }

}

@SuppressLint("MissingPermission")
@Composable
fun SelectLocationScreen(onLocationSelected: (LatLng) -> Unit) {
    val context = LocalContext.current
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    // Ubicación inicial (podría ser la ubicación actual del usuario o un punto por defecto)
    val initialLocation = LatLng(-34.0, 151.0) // Ejemplo, se puede cambiar
  /*
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.maps.android.compose.CameraPositionState.fromLatLngZoom(initialLocation, 10f)
    }
*/
    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            //cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                selectedLocation = latLng
            }
        )

        // Muestra un marcador donde el usuario hace click
        selectedLocation?.let { location ->
            Marker(
                state = MarkerState(position = location),
                title = "Ubicación seleccionada"
            )
        }

        // Botón para confirmar la ubicación seleccionada
        Button(
            onClick = {
                selectedLocation?.let { onLocationSelected(it) }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Confirmar ubicación")
        }
    }
}

// Pantalla del menú de usuario común
@Composable
fun UserMenuScreen() {
    // Estado para controlar si el usuario está editando los datos
    var isEditing by rememberSaveable { mutableStateOf(false) }

    // Pantalla principal o pantalla de edición basada en el estado
    if (isEditing) {
        // Mostrar pantalla de edición de usuario
        EditUserScreen(onSave = {
            isEditing = false // Volver al menú del usuario una vez que se guardan los datos
        })
    } else {
        // Mostrar pantalla del menú de usuario
        UserMenuContent(onEdit = {
            isEditing = true // Cambiar el estado a edición
        })
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUserMenuScreen() {
    UserMenuScreen()
}


@Composable
fun UserMenuContent(onEdit: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bienvenido, ${currentUser.alias}")

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para cerrar sesión
        Button(onClick = {
            isLoggedIn = false
            isAdmin = false
        }) {
            Text("Cerrar sesión")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para editar datos
        Button(onClick = {
            onEdit() // Llama a la función de edición pasada por parámetro
        }) {
            Text("Editar datos")
        }
    }
}

@Composable
fun EditUserScreen(onSave: () -> Unit) {
    val context = LocalContext.current
    var alias by rememberSaveable { mutableStateOf(currentUser.alias) }
    var fullName by rememberSaveable { mutableStateOf(currentUser.fullName) }
    var email by rememberSaveable { mutableStateOf(currentUser.email) }

    // Selector de tipo de casa
    val houseTypes = listOf("Apartamento Inteligente", "Apartamento Normal", "Casa Normal", "Casa con Apartamento")
    var selectedHouseType by rememberSaveable { mutableStateOf(currentUser.houseType) }
    var expandedHouseType by rememberSaveable { mutableStateOf(false) }

    // Selector de tipo de vehículo
    val vehicleTypes = listOf("Bicicleta", "Carro", "Moto", "Otro")
    var selectedVehicleType by rememberSaveable { mutableStateOf(currentUser.vehicleType) }
    var expandedVehicleType by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Editar datos", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        BasicTextField(
            value = alias,
            onValueChange = { alias = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (alias.isEmpty()) Text("Usuario (alias)")
                    innerTextField()
                }
            }
        )

        BasicTextField(
            value = fullName,
            onValueChange = { fullName = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (fullName.isEmpty()) Text("Nombre completo")
                    innerTextField()
                }
            }
        )

        BasicTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (email.isEmpty()) Text("Correo electrónico")
                    innerTextField()
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de tipo de casa
        Text("Seleccione el tipo de casa:")
        Box {
            Button(onClick = { expandedHouseType = !expandedHouseType }) {
                Text(selectedHouseType)
            }
            DropdownMenu(
                expanded = expandedHouseType,
                onDismissRequest = { expandedHouseType = false }
            ) {
                houseTypes.forEach { houseType ->
                    DropdownMenuItem(
                        text = { Text(houseType) },
                        onClick = {
                            selectedHouseType = houseType
                            expandedHouseType = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de tipo de vehículo
        Text("Seleccione el tipo de vehículo:")
        Box {
            Button(onClick = { expandedVehicleType = !expandedVehicleType }) {
                Text(selectedVehicleType)
            }
            DropdownMenu(
                expanded = expandedVehicleType,
                onDismissRequest = { expandedVehicleType = false }
            ) {
                vehicleTypes.forEach { vehicleType ->
                    DropdownMenuItem(
                        text = { Text(vehicleType) },
                        onClick = {
                            selectedVehicleType = vehicleType
                            expandedVehicleType = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val oldUser = currentUser.alias
            // Guardar los cambios
            currentUser.alias = alias
            currentUser.fullName = fullName
            currentUser.email = email
            currentUser.houseType = selectedHouseType
            currentUser.vehicleType = selectedVehicleType

            onSave()  // Llamar al callback de guardado
            saveUserToFile(context, currentUser) // Pasa el contexto y el usuario

            deleteUserFromFile(context, oldUser) //Elimina informacion anterior
        }) {
            Text("Guardar cambios")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onSave) {
            Text("Cancelar")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEditUserScreen() {
    EditUserScreen(onSave = {})
}

//Ventana de registro de usuario
@Composable
fun RegisterUserScreen(
    onBack: () -> Unit,
    onPaymentClick: () -> Unit
) {
    // Recupera los datos del usuario al iniciar la pantalla de registro
    val tempUser = MainActivity.tempUser
    var alias by rememberSaveable { mutableStateOf(tempUser?.alias ?: "") }
    var fullName by rememberSaveable { mutableStateOf(tempUser?.fullName ?: "") }
    var email by rememberSaveable { mutableStateOf(tempUser?.email ?: "") }
    var password by rememberSaveable { mutableStateOf(tempUser?.password ?: "") }
    var dateOfBirth by rememberSaveable { mutableStateOf("") }
    val dateOfBirthPattern = Regex("""\d{2}/\d{2}/\d{4}""")
    var houseType by rememberSaveable { mutableStateOf(tempUser?.houseType ?: "") }
    var vehicleType by rememberSaveable { mutableStateOf(tempUser?.vehicleType ?: "") }
    val context = LocalContext.current


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(25.dp))

        // Campos de texto para el registro de usuario
        BasicTextField(
            value = alias,
            onValueChange = { alias = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(8.dp)) {
                    if (alias.isEmpty()) Text("Alias")
                    innerTextField()
                }
            }
        )

        BasicTextField(
            value = fullName,
            onValueChange = { fullName = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(8.dp)) {
                    if (fullName.isEmpty()) Text("Nombre Completo")
                    innerTextField()
                }
            }
        )

        BasicTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(8.dp)) {
                    if (email.isEmpty()) Text("Email")
                    innerTextField()
                }
            }
        )

        BasicTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            visualTransformation = PasswordVisualTransformation(),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(8.dp)) {
                    if (password.isEmpty()) Text("Contraseña")
                    innerTextField()
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Campo de texto para la fecha de nacimiento con autoformateo
            OutlinedTextField(
                value = dateOfBirth,
                onValueChange = { input ->
                    dateOfBirth = formatDateString(input)  // Llama a la función de autoformateo
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                label = { Text("Fecha de Nacimiento (dd/mm/aaaa)") },
                isError = dateOfBirth.isNotEmpty() && !dateOfBirth.matches(dateOfBirthPattern)
            )

            // Validación de la fecha al salir del campo
            if (dateOfBirth.isNotEmpty() && !dateOfBirth.matches(dateOfBirthPattern)) {
                Text(
                    text = "Por favor ingresa la fecha de nacimiento en el formato dd/mm/aaaa",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

        // Selector de tipo de casa
        val houseTypes = listOf(
            "Apartamento Inteligente",
            "Apartamento Normal",
            "Casa Normal",
            "Casa con Apartamento"
        )
        var selectedHouseType by rememberSaveable { mutableStateOf(houseType) }
        var expandedHouseType by rememberSaveable { mutableStateOf(false) }
        Box {
            Button(onClick = { expandedHouseType = !expandedHouseType }) {
                Text(if (selectedHouseType.isEmpty()) "Seleccionar tipo de casa" else selectedHouseType)
            }
            DropdownMenu(
                expanded = expandedHouseType,
                onDismissRequest = { expandedHouseType = false }
            ) {
                houseTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            selectedHouseType = type // Cambiar al tipo seleccionado
                            expandedHouseType = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de tipo de vehículo
        val vehicleTypes = listOf("Bicicleta", "Carro", "Moto", "Otro")
        var selectedVehicleType by rememberSaveable { mutableStateOf(vehicleType) }
        var expandedVehicleType by rememberSaveable { mutableStateOf(false) }
        Box {
            Button(onClick = { expandedVehicleType = !expandedVehicleType }) {
                Text(if (selectedVehicleType.isEmpty()) "Seleccionar tipo de vehiculo" else selectedVehicleType)
            }
            DropdownMenu(
                expanded = expandedVehicleType,
                onDismissRequest = { expandedVehicleType = false }
            ) {
                vehicleTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            selectedVehicleType = type
                            expandedVehicleType = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Contenedor para botones "Volver" y "Registrar Usuario"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onBack) {
                Text("Volver")
            }

            Button(onClick = {
                val user =
                    User(alias, fullName, email, password, dateOfBirth, houseType, vehicleType)
                if (isPasswordValid(password)) {
                    saveUserToFile(context, user)
                    Toast.makeText(context, "Usuario registrado exitosamente", Toast.LENGTH_LONG)
                        .show()
                    MainActivity.tempUser = null
                    onBack()
                } else {
                    Toast.makeText(context, "Contraseña inválida", Toast.LENGTH_LONG).show()
                }
            }) {
                Text("Registrar Usuario")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

// Botón para ingresar datos de tarjeta
        Button(onClick = {
            MainActivity.tempUser = User(
                alias = alias,
                fullName = fullName,
                email = email,
                password = password,
                dateOfBirth = dateOfBirth,  // Aquí no es necesario convertirlo
                houseType = houseType,
                vehicleType = vehicleType
            )
            onPaymentClick()
        }) {
            Text("Ingresar datos de Tarjeta")
        }
    }

// Función para formatear la fecha automáticamente mientras el usuario escribe
fun formatDateString(input: String): String {
    // Elimina cualquier carácter que no sea dígito
    val digits = input.filter { it.isDigit() }
    val builder = StringBuilder()

    for (i in digits.indices) {
        if (i == 2 || i == 4) builder.append("/")
        builder.append(digits[i])
    }

    return builder.toString()
}

@Preview(showBackground = true)
@Composable
fun PreviewRegisterUserScreen() {
    RegisterUserScreen(
        onBack = {}, // Puedes dejar esto vacío
        onPaymentClick = {} // Esto también, ya que solo es una vista previa
    )
}

@Composable
fun PaymentScreen(onBack: () -> Unit) {  // Aceptar un callback para volver
    var cardholderName by rememberSaveable { mutableStateOf("") }
    var cardNumber by rememberSaveable { mutableStateOf("") }
    var expiryDate by rememberSaveable { mutableStateOf("") }
    var securityCode by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var cardBrand by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nombre del Tarjetahabiente
        BasicTextField(
            value = cardholderName,
            onValueChange = { cardholderName = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (cardholderName.isEmpty()) Text("Nombre del Tarjetahabiente")
                    innerTextField()
                }
            }
        )

        // Número de tarjeta
        BasicTextField(
            value = cardNumber,
            onValueChange = {
                if (it.length <= 16) {
                    cardNumber = it
                    validateCardNumber(it)?.let { brand ->
                        cardBrand = brand
                        errorMessage = null
                    } ?: run {
                        cardBrand = null
                        errorMessage = "El número de tarjeta no es válido. Verifique el primer dígito."
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (cardNumber.isEmpty()) Text("Número de tarjeta (16 dígitos)")
                    innerTextField()
                }
            }
        )

        // Fecha de validez (Mes/Año)
        BasicTextField(
            value = expiryDate,
            onValueChange = { expiryDate = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (expiryDate.isEmpty()) Text("Fecha de Validez (MM/AA)")
                    innerTextField()
                }
            }
        )

        // Número verificador
        BasicTextField(
            value = securityCode,
            onValueChange = {
                if (it.length <= 4) {
                    securityCode = it
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.padding(16.dp)) {
                    if (securityCode.isEmpty()) Text("Número Verificador (4 dígitos)")
                    innerTextField()
                }
            }
        )

        // Mensajes de error y marca de la tarjeta
        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        } else if (cardBrand != null) {
            Text(text = "Marca de la tarjeta: $cardBrand", modifier = Modifier.padding(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para confirmar el pago
        Button(onClick = {
            if (cardNumber.length == 16 && securityCode.length == 4 && cardBrand != null) {
                MainActivity.cardInfo = CardInfo(
                    cardholderName = cardholderName,
                    cardNumber = cardNumber,
                    expiryDate = expiryDate,
                    securityCode = securityCode
                )
                Toast.makeText(context, "Datos agregados con éxito", Toast.LENGTH_LONG).show()
                onBack()  // Regresar a la pantalla anterior
            } else {
                Toast.makeText(context, "Verifica la información ingresada", Toast.LENGTH_LONG).show()
            }
        }) {
            Text("Confirmar Datos")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón para volver a la pantalla de registro
        Button(onClick = onBack) {
            Text("Volver a Registro")
        }
    }
}


// Preview para visualizar PaymentScreen
@Preview(showBackground = true)
@Composable
fun PaymentScreenPreview() {
    PaymentScreen(onBack = { /* Acción para volver */ })
}

// Función para validar el número de tarjeta y determinar la marca
fun validateCardNumber(cardNumber: String): String? {
    return when (cardNumber.firstOrNull()) {
        '1' -> "Visca"
        '2' -> "MasterChef"
        '3' -> "AmericanCity"
        '5' -> "TicaPay"
        else -> null
    }
}

// Función para leer los datos de usuario desde el archivo
fun readUserDataFromFile(context: Context, alias: String): User {
    val file = File(context.filesDir, "users.txt")
    if (file.exists()) {
        val lines = file.readLines()
        for (line in lines) {
            val parts = line.split(",")
            if (parts.size == 7 && parts[0] == alias) {
                return User(
                    alias = parts[0],
                    fullName = parts[1],
                    email = parts[2],
                    password = parts[3],
                    dateOfBirth = parts[4], // Convertir el campo de edad a entero
                    houseType = parts[5],
                    vehicleType = parts[6]
                )
            }
        }
    }
    return User("A", "B", "C", "D", "E", "F", "G")
}

fun readUserDataByEmail(context: Context, email: String): User? {
    val file = File(context.filesDir, "users.txt")
    if (file.exists()) {
        val lines = file.readLines()
        for (line in lines) {
            val parts = line.split(",")
            if (parts.size == 7 && parts[2] == email) {
                return User(
                    alias = parts[0],
                    fullName = parts[1],
                    email = parts[2],
                    password = parts[3],
                    dateOfBirth = parts[4],
                    houseType = parts[5],
                    vehicleType = parts[6]
                )
            }
        }
    }
    return null
}

fun sendRecoveryCode(context: Context, email: String) {
    // Verifica si el correo existe en la lista de usuarios
    val user = readUserDataByEmail(context, email)
    if (user == null) {
        // Mostrar mensaje si el correo no existe
        Toast.makeText(context, "Correo no encontrado", Toast.LENGTH_SHORT).show()
        return
    }

    // Genera el código de recuperación
    val code = (10000..99999).random().toString()

    // Guarda el código en SharedPreferences
    val sharedPreferences = context.getSharedPreferences("RecoveryPrefs", Context.MODE_PRIVATE)
    sharedPreferences.edit().putString("recovery_code", code).putString("recovery_email", email).apply()

    // Enviar el código por correo electrónico
    sendEmail(email, code)

    // Mostrar mensaje de confirmación de envío
    Toast.makeText(context, "Código enviado al correo: $email", Toast.LENGTH_LONG).show()
}

private fun sendEmail(toEmail: String, code: String) {
    // Configuración del servidor SMTP
    val props = Properties()
    props["mail.smtp.auth"] = "true"
    props["mail.smtp.starttls.enable"] = "true"
    props["mail.smtp.host"] = "587.gmail.com" // Cambiar según tu servidor SMTP
    props["mail.smtp.port"] = "587"

    // Credenciales del remitente
    val username = "tr9j1x@gmail.com" // Reemplaza con tu correo
    val password = "Tr9j1x1208" // Reemplaza con tu contraseña o token de aplicación

    val session = Session.getInstance(props, object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(username, password)
        }
    })

    try {
        val message = MimeMessage(session)
        message.setFrom(InternetAddress(username))
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
        message.subject = "Código de recuperación de cuenta"
        message.setText("Su código de verificación es: $code")

        // Enviar el mensaje
        Transport.send(message)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
fun saveNewPassword(context: Context, newPassword: String) {
    val sharedPreferences = context.getSharedPreferences("RecoveryPrefs", Context.MODE_PRIVATE)
    val email = sharedPreferences.getString("recovery_email", "")

    if (email != null && email.isNotEmpty()) {
        val file = File(context.filesDir, "users.txt")
        if (file.exists()) {
            val lines = file.readLines().toMutableList()
            val updatedLines = lines.map { line ->
                val parts = line.split(",")
                if (parts.size == 7 && parts[2] == email) {
                    "${parts[0]},${parts[1]},$email,$newPassword,${parts[4]},${parts[5]},${parts[6]}"
                } else {
                    line
                }
            }
            file.writeText(updatedLines.joinToString("\n"))
            Toast.makeText(context, "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show()
        }
    }
}

// Función para leer todos los usuarios
fun readAllUsers(context: Context): List<User> {
    val file = File(context.filesDir, "users.txt")
    val users = mutableListOf<User>()
    if (file.exists()) {
        val lines = file.readLines()
        for (line in lines) {
            val parts = line.split(",")
            if (parts.size == 7) {
                users.add(
                    User(
                        alias = parts[0],
                        fullName = parts[1],
                        email = parts[2],
                        password = parts[3],
                        dateOfBirth = parts[4],
                        houseType = parts[5],
                        vehicleType = parts[6]
                    )
                )
            }
        }
    }
    return users
}

// Función para verificar si el alias o el correo ya existen
fun isUserAlreadyRegistered(context: Context, alias: String, email: String): Boolean {
    val file = File(context.filesDir, "users.txt")
    if (file.exists()) {
        val lines = file.readLines()
        for (line in lines) {
            val parts = line.split(",")
            if (parts.size == 7) {
                val existingAlias = parts[0]
                val existingEmail = parts[2]
                if (existingAlias == alias || existingEmail == email) {
                    return true // Alias o correo ya están en uso
                }
            }
        }
    }
    return false
}



// Función para recordar al admin que cambie la contraseña cada 2 minutos usando notificaciones
fun startPasswordReminder(context: Context) {
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
                .setSmallIcon(R.drawable.ic_dialog_alert)
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