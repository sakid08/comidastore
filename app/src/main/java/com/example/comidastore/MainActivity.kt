package com.example.comidastore

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.Brush
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.KeyboardType


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrdenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    appOrdenComida()
                }
            }
        }
    }
}

@Composable
fun appOrdenComida() {
    var selectMainComida by remember { mutableStateOf("Tacos") }
    var seleccionTamano by remember { mutableStateOf("Normal") }
    var selectExtras by remember { mutableStateOf(setOf<String>()) }
    var selectBebida by remember { mutableStateOf("Ninguna") }
    var procesoOrden by remember { mutableStateOf(0.25f) }

    val precioTotal = calcPrecioTotal(selectMainComida, seleccionTamano, selectExtras, selectBebida)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "El progreso de pedido es ...",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        //progreso
        ProcesoOrdenIndicator(progress = procesoOrden)

        Spacer(modifier = Modifier.height(16.dp))

        //plato principal
        MainSelectorComida(
            selectMainComida = selectMainComida,
            onMainDishSelected = {
                selectMainComida = it
                procesoOrden = calculateProgress(true, seleccionTamano, selectExtras, selectBebida)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        //tamaño
        SizeSelector(
            seleccionTamano = seleccionTamano,
            onSizeSelected = {
                seleccionTamano = it
                procesoOrden = calculateProgress(selectMainComida.isNotEmpty(), seleccionTamano, selectExtras, selectBebida)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        //selección de extras
        ExtrasSelector(
            selectExtras = selectExtras,
            onExtrasSelected = {
                selectExtras = it
                procesoOrden = calculateProgress(selectMainComida.isNotEmpty(), seleccionTamano, selectExtras, selectBebida)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        //selección de bebida
        BibidaSeleccion(
            selectBebida = selectBebida,
            onDrinkSelected = {
                selectBebida = it
                procesoOrden = calculateProgress(selectMainComida.isNotEmpty(), seleccionTamano, selectExtras, selectBebida)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Resumen del pedido y precio total
        OrderSummary(
            selectMainComida = selectMainComida,
            seleccionTamano = seleccionTamano,
            selectExtras = selectExtras,
            selectBebida = selectBebida,
            precioTotal = precioTotal
        )

        Spacer(modifier = Modifier.height(16.dp))

    }
}

@Composable
fun OrderSummary(
    selectMainComida: String,
    seleccionTamano: String,
    selectExtras: Set<String>,
    selectBebida: String,
    precioTotal: Double
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("4433948965") } // Número por defecto

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val mensaje = buildOrderMessage(
                selectMainComida, seleccionTamano, selectExtras, selectBebida, precioTotal
            )
            sendSMS(context, phoneNumber, mensaje)
        } else {
            Toast.makeText(
                context,
                "Permiso denegado - No se puede enviar SMS",
                Toast.LENGTH_LONG
            ).show()
        }
        isSending = false
    }

    // Diálogo para explicar el permiso
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showPermissionDialog = false
                isSending = false
            },
            title = { Text("Permiso necesario") },
            text = { Text("Necesitamos permiso para enviar SMS y procesar tu pedido") },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        isSending = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Resumen del Pedido",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Plato Principal: $selectMainComida",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Tamaño: $seleccionTamano",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (selectExtras.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Extras: ${selectExtras.joinToString(", ")}",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            if (selectBebida != "Ninguna") {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Bebida: $selectBebida",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo para el número de teléfono
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Número de teléfono") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Precio Total:",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = "$${String.format("%.2f", precioTotal)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de envío con estado
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = {
                        if (phoneNumber.isBlank()) {
                            Toast.makeText(
                                context,
                                "Ingrese un número de teléfono",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        isSending = true
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.SEND_SMS
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            val mensaje = buildOrderMessage(
                                selectMainComida, seleccionTamano, selectExtras, selectBebida, precioTotal
                            )
                            sendSMS(context, phoneNumber, mensaje)
                            isSending = false
                        } else {
                            showPermissionDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isSending
                ) {
                    Text("Confirmar y enviar orden por SMS")
                }
            }
        }
    }
}

@Composable
fun ProcesoOrdenIndicator(progress: Float) {
    val steps = 5 // Número de puntos/pasos
    val stepProgress = 1f / steps

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Indicador de puntos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(steps) { index ->
                val stepFilled = progress >= (index + 1) * stepProgress
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (stepFilled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            width = 2.dp,
                            color = if (!stepFilled) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                            shape = CircleShape
                        )
                )
            }
        }

        // Barra de progreso decorativa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
            )
        }

        // Texto con porcentaje
        Text(
            text = "Progreso del pedido: ${(progress * 100).roundToInt()}%",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally),
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSelectorComida(
    selectMainComida: String,
    onMainDishSelected: (String) -> Unit
) {
    val mainDishes = listOf("Tacos", "Tortas")
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_agenda),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Seleccione qué llevará",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = when (selectMainComida) {
                    "Tacos" -> R.drawable.taco
                    "Tortas" -> R.drawable.torta
                    else -> android.R.drawable.ic_menu_gallery
                }),
                contentDescription = "Imagen de $selectMainComida",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectMainComida,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Plato principal") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    mainDishes.forEach { dish ->
                        DropdownMenuItem(
                            text = { Text(dish) },
                            onClick = {
                                onMainDishSelected(dish)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SizeSelector(
    seleccionTamano: String,
    onSizeSelected: (String) -> Unit
) {
    val sizes = listOf("Kids", "Normal", "Extra Grande")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Selecciona el Tamaño",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Radio group para selección de tamaño
            sizes.forEach { size ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = size == seleccionTamano,
                        onClick = { onSizeSelected(size) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Text(
                        text = size,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExtrasSelector(
    selectExtras: Set<String>,
    onExtrasSelected: (Set<String>) -> Unit
) {
    val extras = listOf("Papas a la francesa", "Dedos de queso", "Caramelos")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Selecciona un postre",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Checkboxes para selección de extras
            extras.forEach { extra ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectExtras.contains(extra),
                        onCheckedChange = {
                            val newselectExtras = selectExtras.toMutableSet()
                            if (it) newselectExtras.add(extra) else newselectExtras.remove(extra)
                            onExtrasSelected(newselectExtras)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Text(
                        text = extra,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BibidaSeleccion(
    selectBebida: String,
    onDrinkSelected: (String) -> Unit
) {
    val drinks = listOf("Ninguna", "Coca-Cola", "Pepsi", "Fanta", "Sprite", "Agua Mineral")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Selecciona una bebida",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Spinner simulado con DropdownMenu
            var expanded by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectBebida,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.arrow_down_float),
                                contentDescription = "Expandir"
                            )
                        }
                    }
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    drinks.forEach { drink ->
                        DropdownMenuItem(
                            text = { Text(drink) },
                            onClick = {
                                onDrinkSelected(drink)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}



fun buildOrderMessage(
    main: String,
    size: String,
    extras: Set<String>,
    drink: String,
    price: Double
): String {
    return """
        📝 Orden:
        Plato: $main
        Tamaño: $size
        Extras: ${if (extras.isNotEmpty()) extras.joinToString() else "Ninguno"}
        Bebida: $drink
        Total: $${"%.2f".format(price)}
    """.trimIndent()
}

fun sendSMS(context: Context, phoneNumber: String, message: String) {
    try {
        val smsManager = context.getSystemService(SmsManager::class.java)
        smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        Toast.makeText(context, "SMS enviado correctamente", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Error al enviar SMS: ${e.localizedMessage}",
            Toast.LENGTH_LONG
        ).show()
        e.printStackTrace()
    }
}


// Función para calcular el progreso del pedido
fun calculateProgress(
    hasMainDish: Boolean,
    size: String,
    extras: Set<String>,
    drink: String
): Float {
    var progress = 0f

    if (hasMainDish) progress += 0.25f
    if (size.isNotEmpty()) progress += 0.25f
    if (extras.isNotEmpty()) progress += 0.25f
    if (drink != "Ninguna") progress += 0.25f

    return progress
}

fun calcPrecioTotal(
    mainDish: String,
    size: String,
    extras: Set<String>,
    drink: String
): Double {
    // Precios
    val mainDishPrice = when (mainDish) {
        "Tacos" -> 50.0
        "Tortas" -> 45.0
        else -> 0.0
    }

    val sizeMultiplier = when (size) {
        "Kids" -> 1.0
        "Normal" -> 1.5
        "Extra Grande" -> 1.9
        else -> 1.0
    }

    val extrasPrice = extras.size * 20.0

    val drinkPrice = when (drink) {
        "Ninguna" -> 0.0
        "Coca-Cola" -> 25.0
        "Pepsi" -> 23.0
        "Fanta" -> 22.0
        "Sprite" -> 22.0
        "Agua Mineral" -> 18.0
        else -> 0.0
    }

    return (mainDishPrice * sizeMultiplier) + extrasPrice + drinkPrice
}

@Composable
fun OrdenTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF1976D2),  // Azul
        onPrimary = Color.White,
        primaryContainer = Color(0xFFBBDEFB),  // Azul claro
        onPrimaryContainer = Color(0xFF003c8f),
        secondary = Color(0xFF4CAF50),  // Verde
        background = Color(0xFFE8F5E9),  // Verde muy claro
        surface = Color.White,
        onSurface = Color(0xFF212121),
        onSurfaceVariant = Color(0xFF757575)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}