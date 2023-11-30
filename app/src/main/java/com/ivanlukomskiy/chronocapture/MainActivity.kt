@file:OptIn(ExperimentalMaterial3Api::class)

package com.ivanlukomskiy.chronocapture

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.ivanlukomskiy.chronocapture.ui.theme.ChronoCaptureTheme

//import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation(this)
        }
        setContent {
            ChronoCaptureTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(this)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(context: Context) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") { Greeting(context) }
        composable("editTelegramToken") { EditTelegramTokenScreen(navController) }
    }
}

@Composable
fun EditTelegramTokenScreen(navController: NavController) {
    var token by remember { mutableStateOf("") }

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
        Button(onClick = { navController.popBackStack() }) {
            Text("Save")
        }
    }
}

@Composable
fun Greeting(context: Context, modifier: Modifier = Modifier) {
    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    val defaultCron = "0 14 * * *"
    var text by remember { mutableStateOf(sharedPreferences.getString("cronTab", defaultCron) ?: defaultCron) }
    var savedCron by remember { mutableStateOf(sharedPreferences.getString("cronTab", defaultCron) ?: defaultCron) }
    var showError by remember { mutableStateOf(false) }

    fun isValidCronExpression(cronExpression: String): Boolean {
        return cronExpression.matches("^\\d+ \\d+ \\* \\* \\*$".toRegex())
    }

    Column(modifier = modifier) {
        Text(text = "Take photo each day at 14:00")

//        Row {
//            TextField(
//                value = text,
//                onValueChange = {
//                    text = it
//                    showError = !isValidCronExpression(it)
//                },
//                label = { Text("Image capture cron tab") },
//                isError = showError
//            )
//
//            Button(
//                onClick = {
//                    if (isValidCronExpression(text)) {
//                        sharedPreferences.edit().putString("cronTab", text).apply()
//                        savedCron = text // Update the savedCron state
//                        showError = false
//                    } else {
//                        showError = true
//                    }
//                },
//                enabled = text != savedCron && isValidCronExpression(text) // Check against savedCron
//            ) {
//                Text("Apply")
//            }
//        }

        MyTimePicker()

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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ChronoCaptureTheme {
        Greeting(LocalContext.current)
    }
}
