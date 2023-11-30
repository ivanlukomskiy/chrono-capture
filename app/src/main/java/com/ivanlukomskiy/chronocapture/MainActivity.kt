@file:OptIn(ExperimentalMaterial3Api::class)

package com.ivanlukomskiy.chronocapture

import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ivanlukomskiy.chronocapture.ui.theme.ChronoCaptureTheme
import kotlinx.coroutines.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random
import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.DataOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

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

fun sendMessageToTelegramChannel(botToken: String, channelId: String, message: String) {
    val urlString = "https://api.telegram.org/bot$botToken/sendMessage?chat_id=$channelId&text=${
        java.net.URLEncoder.encode(
            message,
            "UTF-8"
        )
    }"

    try {
        val url = URL(urlString)
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET" // Telegram Bot API uses GET for sending messages

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // The message was sent successfully
                println("Message sent to Telegram channel successfully.")
            } else {
                // There was an error sending the message
                println("Failed to send message. Response Code: $responseCode")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun takePhoto(context: Context, outputDirectory: File, onPhotoTaken: (File) -> Unit) {
    val activity = context as Activity
    val request = Random.nextInt()
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
        println("No permissions granted, asking...")
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), request)
        return
    }

    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        val imageCapture = ImageCapture.Builder().build()

        try {
            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(/* lifecycleOwner */ context as LifecycleOwner,
                cameraSelector,
                imageCapture
            )

            val photoFile = File(
                outputDirectory,
                SimpleDateFormat(
                    "yyyyMMdd-HHmmss",
                    Locale.US
                ).format(System.currentTimeMillis()) + ".jpg"
            )

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        println("Image saved! $photoFile")
                        onPhotoTaken(photoFile)
                    }

                    override fun onError(exc: ImageCaptureException) {
                        println("Image not saved( $exc")
                        exc.printStackTrace()
                        // Handle error
                    }
                })
        } catch (exc: Exception) {
            // Handle error
        }
    }, ContextCompat.getMainExecutor(context))
}

fun sendImageToTelegramChannel(botToken: String, channelId: String, imagePath: String) {
    val lineEnd = "\r\n"
    val twoHyphens = "--"
    val boundary = "*****${System.currentTimeMillis()}*****"

    var httpURLConnection: HttpURLConnection? = null

    try {
        val url = URL("https://api.telegram.org/bot$botToken/sendPhoto")
        httpURLConnection = url.openConnection() as HttpURLConnection
        httpURLConnection.doInput = true
        httpURLConnection.doOutput = true
        httpURLConnection.useCaches = false
        httpURLConnection.requestMethod = "POST"
        httpURLConnection.setRequestProperty("Connection", "Keep-Alive")
        httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")

        val outputStream = DataOutputStream(httpURLConnection.outputStream)

        outputStream.writeBytes(twoHyphens + boundary + lineEnd)
        outputStream.writeBytes("Content-Disposition: form-data; name=\"chat_id\"" + lineEnd)
        outputStream.writeBytes(lineEnd)
        outputStream.writeBytes(channelId + lineEnd)
        outputStream.writeBytes(twoHyphens + boundary + lineEnd)

        outputStream.writeBytes("Content-Disposition: form-data; name=\"photo\";filename=\"image.jpg\"" + lineEnd)
        outputStream.writeBytes("Content-Type: image/jpeg" + lineEnd)
        outputStream.writeBytes(lineEnd)

        val fileInputStream = FileInputStream(imagePath)
        val buffer = ByteArray(1024)
        var bytesRead: Int

        while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }
        fileInputStream.close()

        outputStream.writeBytes(lineEnd)
        outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)

        outputStream.flush()
        outputStream.close()

        // Read response
        val responseStream = if (httpURLConnection.responseCode == HttpURLConnection.HTTP_OK) {
            httpURLConnection.inputStream
        } else {
            httpURLConnection.errorStream
        }

        responseStream.bufferedReader().use {
            val response = it.readText()
            println("Response: $response")
        }

    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        httpURLConnection?.disconnect()
    }
}


@Composable
fun EditTelegramTokenScreen(context: Context, navController: NavController) {
    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    var token by remember { mutableStateOf(sharedPreferences.getString("tgToken", "") ?: "") }
    var channel by remember { mutableStateOf(sharedPreferences.getString("tgChannel", "") ?: "") }

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
        TextField(
            value = channel,
            onValueChange = { channel = it },
            label = { Text("Channel ID") }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            navController.popBackStack()
        }) {
            Text("Cancel")
        }
        Button(onClick = {
            sharedPreferences.edit().putString("tgToken", token).apply()
            sharedPreferences.edit().putString("tgChannel", channel).apply()
            navController.popBackStack()
        }) {
            Text("Save")
        }
        Button(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                try {
//                    sendMessageToTelegramChannel(token, channel, "it worked!")



                    withContext(Dispatchers.IO) {
//                        val photoFile = File.createTempFile("photo_", ".jpg", context.cacheDir)
                        takePhoto(context, context.cacheDir) {
                            println("image taken, sending $it")
                            CoroutineScope(Dispatchers.IO).launch {
                                sendImageToTelegramChannel(token, channel, it.absolutePath)
                            }
                        }
                    }


                } catch (e: Exception) {
                    println("FAILED")
                    // Handle exception
                }
            }
        }) {
            Text("Test send message")
        }
    }
}

@Composable
fun Greeting(context: Context, navController: NavController, modifier: Modifier = Modifier) {
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Take photo each day at 14:00")
        Spacer(modifier = Modifier.height(16.dp))
        MyTimePicker()

        Button(onClick = {
            navController.navigate("editTelegramToken")
        }) {
            Text("Edit telegram token")
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
