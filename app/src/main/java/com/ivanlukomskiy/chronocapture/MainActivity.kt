@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)

package com.ivanlukomskiy.chronocapture

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.ivanlukomskiy.chronocapture.ui.theme.ChronoCaptureTheme
import kotlinx.coroutines.*
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        setContent {
            ChronoCaptureTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
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
        composable("main") { MainScreen(context, navController) }
        composable("settings") { SettingsScreen(context, navController) }
        composable("capture") { CaptureScreen(context, navController) }
    }
}

fun takePhoto(
    context: Context,
    outputDirectory: File,
    onPhotoTaken: (File) -> Unit,
    onPhotoError: (Exception) -> Unit
) {
    if (context is Activity) {
        val activity = context as Activity
        val request = abs(Random.nextInt())
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("No permissions granted, asking...")
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.CAMERA), request
            )
            onPhotoError(java.lang.RuntimeException("Permissions required"))
            return
        }
    }

    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        val imageCapture = ImageCapture.Builder().build()
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                context as LifecycleOwner, cameraSelector, imageCapture
            )
            val photoFile = File(
                outputDirectory, SimpleDateFormat(
                    "yyyyMMdd-HHmmss", Locale.US
                ).format(System.currentTimeMillis()) + ".jpg"
            )
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            imageCapture.takePicture(outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        onPhotoTaken(photoFile)
                    }

                    override fun onError(exc: ImageCaptureException) {
                        onPhotoError(exc)
                    }
                })
        } catch (exc: Exception) {
            onPhotoError(exc)
        }
    }, ContextCompat.getMainExecutor(context))
}

fun sendImageToTelegramChannel(
    botToken: String, channelId: String, imagePath: String, onComplete: (error: Exception?) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        val lineEnd = "\r\n"
        val twoHyphens = "--"
        val boundary = "*****${System.currentTimeMillis()}*****"

        var httpURLConnection: HttpURLConnection? = null
        var exception: Exception? = null

        try {
            val url = URL("https://api.telegram.org/bot$botToken/sendPhoto")
            httpURLConnection = url.openConnection() as HttpURLConnection
            httpURLConnection.doInput = true
            httpURLConnection.doOutput = true
            httpURLConnection.useCaches = false
            httpURLConnection.requestMethod = "POST"
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive")
            httpURLConnection.setRequestProperty(
                "Content-Type", "multipart/form-data;boundary=$boundary"
            )

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

            val responseStream = if (httpURLConnection.responseCode == HttpURLConnection.HTTP_OK) {
                httpURLConnection.inputStream
            } else {
                httpURLConnection.errorStream
            }
            if (httpURLConnection.responseCode != HttpURLConnection.HTTP_OK) {
                throw RuntimeException("Server responded with code ${httpURLConnection.responseCode}")
            }
            responseStream.bufferedReader().use {
                val response = it.readText()
                println("Response: $response")
            }
        } catch (e: Exception) {
            exception = e
        } finally {
            httpURLConnection?.disconnect()
            onComplete(exception)
        }
    }
}

fun showToast(context: Context, text: String) {
    if (context !is Activity) {
        return
    }
    val activity = context as Activity
    activity.runOnUiThread {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
}

fun takeAndSendPhoto(
    context: Context, token: String, channel: String, onComplete: (error: Exception?) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        takePhoto(context, context.cacheDir, { it ->
            sendImageToTelegramChannel(token, channel, it.absolutePath) {
                if (it == null) {
                    showToast(context, "Photo sent")
                } else {
                    showToast(context, "Failed to send message: ${it.message}")
                    it.printStackTrace()
                }
                onComplete(it)
            }
        }, {
            showToast(context, "Failed to take photo: ${it.message}")
            onComplete(it)
            it.printStackTrace()
        })
    }
}

@Composable
fun SettingsScreen(context: Context, navController: NavController) {
    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    var token by remember { mutableStateOf(sharedPreferences.getString("tgToken", "") ?: "") }
    var channel by remember { mutableStateOf(sharedPreferences.getString("tgChannel", "") ?: "") }
    var time by remember { mutableStateOf(sharedPreferences.getString("time", "12:00") ?: "12:00") }
    var joinLink by remember { mutableStateOf(sharedPreferences.getString("joinLink", "") ?: "") }

    val keyboardController = LocalSoftwareKeyboardController.current
    var isLoading by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            TextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Telegram Token") },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                }),
                modifier = Modifier.fillMaxWidth(1f)
            )
        }
        item {
            TextField(
                value = channel,
                onValueChange = { channel = it },
                label = { Text("Channel ID") },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                }),
                modifier = Modifier.fillMaxWidth(1f)
            )
        }
        item {
            TextField(
                value = joinLink,
                onValueChange = { joinLink = it },
                label = { Text("Join link") },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                }),
                modifier = Modifier.fillMaxWidth(1f)
            )
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Button(onClick = {
                navController.popBackStack()
            }) {
                Text("Cancel")
            }
        }
        item {
            Button(onClick = {
                sharedPreferences.edit().putString("tgToken", token).apply()
                sharedPreferences.edit().putString("tgChannel", channel).apply()
                sharedPreferences.edit().putString("time", time).apply()
                sharedPreferences.edit().putString("joinLink", joinLink).apply()
                navController.popBackStack()
            }) {
                Text("Save")
            }
        }
        item {
            Button(onClick = {
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)

                val timePickerDialog = TimePickerDialog(
                    context, { _, selectedHour, selectedMinute ->
                        time = "$selectedHour:$selectedMinute"
                    }, hour, minute, true
                )
                timePickerDialog.show()
            }) {
                Text("Set time")
            }
        }
        item {
            Button(onClick = {
                isLoading = true
                takeAndSendPhoto(context, token, channel) {
                    isLoading = false
                }
            }, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sending...")
                } else {
                    Text("Send test message")
                }
            }
        }
    }
}

@Composable
fun MainScreen(context: Context, navController: NavController, modifier: Modifier = Modifier) {
    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    val time by remember { mutableStateOf(sharedPreferences.getString("time", "12:00") ?: "12:00") }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Take photo each day at $time")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            navController.navigate("settings")
        }) {
            Text("Settings")
        }
        Button(onClick = {
            navController.navigate("capture")
        }) {
            Text("Start")
        }
    }
}

fun getSecondsLeft(time: String): Long {
    val split = time.split(":")
    val hours = split[0].toInt()
    val minutes = split[1].toInt()
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hours)
        set(Calendar.MINUTE, minutes)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (calendar.timeInMillis < System.currentTimeMillis()) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }
    return (calendar.timeInMillis - System.currentTimeMillis()) / 1000
}

fun formatSeconds(seconds: Long): String {
    var hours = (seconds / 60 / 60).toInt()
    if (hours > 1) {
        return "in $hours hours"
    }
    if (hours == 1) {
        return "in 1 hour"
    }
    val minutes = (seconds / 60).toInt() % 60
    if (minutes > 1) {
        return "in $minutes minutes"
    }
    if (minutes == 1) {
        return "in 1 minute"
    }
    if (seconds > 1) {
        return "in $seconds seconds"
    }
    if (seconds.toInt() == 1) {
        return "in 1 second"
    }
    return "now"
}

fun generateQRCode(text: String, foreground: Color, background: Color): Bitmap? {
    if (text == "") {
        return null
    }
    val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
    hints[EncodeHintType.MARGIN] = 0 // Set the margin (quiet zone) to zero
    val width = 500
    val height = 500
    val qrCodeWriter = QRCodeWriter()
    val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints)

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(
                x,
                y,
                if (bitMatrix[x, y]) foreground.toArgb() else background.toArgb()
            )
        }
    }
    return bitmap
}

@Composable
fun CaptureScreen(context: Context, navController: NavController, modifier: Modifier = Modifier) {
    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    val time by remember { mutableStateOf(sharedPreferences.getString("time", "12:00") ?: "12:00") }
    val token by remember { mutableStateOf(sharedPreferences.getString("tgToken", "") ?: "") }
    val channel by remember { mutableStateOf(sharedPreferences.getString("tgChannel", "") ?: "") }
    var secondsLeft by remember { mutableStateOf(getSecondsLeft(time)) }
    var ticker by remember { mutableStateOf(0) }
    var sendingStatus by remember { mutableStateOf("") }
    var lastSendingStatus by remember { mutableStateOf("") }
    val joinLink by remember { mutableStateOf(sharedPreferences.getString("joinLink", "") ?: "") }
    val foreground = MaterialTheme.colorScheme.primary;
    val background = MaterialTheme.colorScheme.background;
    val qrCodeBitmap by remember { mutableStateOf(generateQRCode(joinLink, foreground, background)) }

    LaunchedEffect(key1 = ticker) {
        delay(1000)
        val newSecondsLeft = getSecondsLeft(time)
        if (newSecondsLeft > secondsLeft) {
            sendingStatus = "Sending an image..."
            takeAndSendPhoto(context, token, channel) {
                sendingStatus = ""
                if (it == null) {
                    lastSendingStatus = "Last sending attempt: success"
                } else {
                    lastSendingStatus =
                        "Last sending attempt: failed with ${it.javaClass}: ${it.message}"
                }
            }
        }
        secondsLeft = newSecondsLeft
        ticker++
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        Text("Please, don't turn off", color = Color.Red)
        var displayText = sendingStatus
        if (displayText == "") {
            val formattedTime = formatSeconds(secondsLeft)
            displayText = "Taking a picture $formattedTime"
        }
        Text(displayText)
        if (lastSendingStatus != "") {
            Text(lastSendingStatus)
        }
        if (qrCodeBitmap != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Join channel:", color = foreground)
            Spacer(modifier = Modifier.height(8.dp))
            Image(bitmap = qrCodeBitmap!!.asImageBitmap(), contentDescription = "QR Code")
            Spacer(modifier = Modifier.height(16.dp))
        }
        Button(onClick = {
            navController.navigate("main")
        }) {
            Text("Stop")
        }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    ChronoCaptureTheme {
//        Greeting(LocalContext.current)
//    }
//}
