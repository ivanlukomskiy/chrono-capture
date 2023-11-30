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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChronoCaptureTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android", this)
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, context: Context, modifier: Modifier = Modifier) {
    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    val defaultCron = "0 14 * * *"
    var text by remember { mutableStateOf(sharedPreferences.getString("cronTab", defaultCron) ?: defaultCron) }
    var savedCron by remember { mutableStateOf(sharedPreferences.getString("cronTab", defaultCron) ?: defaultCron) }
    var showError by remember { mutableStateOf(false) }

    fun isValidCronExpression(cronExpression: String): Boolean {
        return cronExpression.matches("^\\d+ \\d+ \\* \\* \\*$".toRegex())
    }

    Column(modifier = modifier) {
        Text(text = "Hello $name!")

        Row {
            TextField(
                value = text,
                onValueChange = {
                    text = it
                    showError = !isValidCronExpression(it)
                },
                label = { Text("Image capture cron tab") },
                isError = showError
            )

            Button(
                onClick = {
                    if (isValidCronExpression(text)) {
                        sharedPreferences.edit().putString("cronTab", text).apply()
                        savedCron = text // Update the savedCron state
                        showError = false
                    } else {
                        showError = true
                    }
                },
                enabled = text != savedCron && isValidCronExpression(text) // Check against savedCron
            ) {
                Text("Apply")
            }
        }

        if (showError) {
            Text("Invalid cron expression", color = Color.Red)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ChronoCaptureTheme {
        Greeting("Android", LocalContext.current)
    }
}
