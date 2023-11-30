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
    var text by remember { mutableStateOf(sharedPreferences.getString("cronTab", "") ?: "") }

    Column(modifier = modifier) {
        Text(text = "Hello $name!")

        Row {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Image capture cron tab") }
            )

            Button(onClick = {
                sharedPreferences.edit().putString("cronTab", text).apply()
            }) {
                Text("Apply")
            }
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
