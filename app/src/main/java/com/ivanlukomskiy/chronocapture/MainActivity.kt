@file:OptIn(ExperimentalMaterial3Api::class)

package com.ivanlukomskiy.chronocapture

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ivanlukomskiy.chronocapture.ui.theme.ChronoCaptureTheme
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChronoCaptureTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(this)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(context: Context) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") { Greeting(context, navController) }
        composable("editTelegramToken") { EditTelegramTokenScreen(context, navController) }
    }
}

@Composable
fun EditTelegramTokenScreen(context: Context, navController: NavController) {
    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    var token by remember { mutableStateOf(sharedPreferences.getString("tgToken", "") ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("Telegram Token") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = {
                sharedPreferences.edit().putString("tgToken", "").apply()
                token = ""
            }) {
                Text("Clear")
            }
            Button(onClick = {
                sharedPreferences.edit().putString("tgToken", token).apply()
                navController.popBackStack()
            }) {
                Text("Save")
            }
        }
    }
}

@Composable
fun Greeting(context: Context, navController: NavController, modifier: Modifier = Modifier) {
    var showError by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(text = "Take photo each day at 14:00")
        MyTimePicker()

        Button(onClick = {
            navController.navigate("editTelegramToken")
        }) {
            Text("Edit telegram token")
        }

        Button(onClick = {

        }) {
            Text("Test send message")
        }

        if (showError) {
            Text("Invalid cron expression", color = Color.Red)
        }
    }
}

@Composable
fun MyTimePicker() {
    val context = LocalContext.current
    var time by remember { mutableStateOf("") }

    Button(onClick = {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                time = "$selectedHour:$selectedMinute"
            }, hour, minute, true
        )
        timePickerDialog.show()
    }) {
        Text("Edit time")
    }
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    ChronoCaptureTheme {
//        Greeting(LocalContext.current)
//    }
//}
