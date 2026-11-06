package dev.hdytrfli.omata.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

private data class OcrResult(val lines: List<String>) {
    val fullText: String = lines.joinToString("\n")
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasCameraPermission()) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            granted = result
            askedOnce = true
        }

    LaunchedEffect(Unit) { if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA) }

    if (granted) {
        CameraCaptureScreen()
    } else {
        PermissionScreen(
            askedOnce = askedOnce,
            onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = { context.openAppSettings() },
        )
    }
}

@Composable
private fun CameraCaptureScreen() {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val imageCapture = remember {
        val resolution =
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1920, 1440),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    )
                )
                .build()
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(resolution)
            .build()
    }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val decodeExecutor = remember { Executors.newSingleThreadExecutor() }

    var result by remember { mutableStateOf<OcrResult?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            recognizer.close()
            decodeExecutor.shutdown()
        }
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Omata", text))
        scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
    }

    fun fail(message: String) {
        isProcessing = false
        result = null
        isError = true
        statusMessage = message
    }

    fun recognise(bitmap: Bitmap, rotationDegrees: Int) {
        recognizer
            .process(InputImage.fromBitmap(bitmap, rotationDegrees))
            .addOnSuccessListener { text ->
                val lines =
                    text.textBlocks
                        .flatMap { block -> block.lines }
                        .map { line -> line.text.trim() }
                        .filter { line -> line.isNotBlank() }

                isProcessing = false
                if (lines.isEmpty()) {
                    fail("No text found. Move closer, hold steady and try again.")
                } else {
                    isError = false
                    statusMessage = null
                    result = OcrResult(lines)
                }
            }
            .addOnFailureListener { error ->
                fail("Could not read text: ${error.message ?: "unknown error"}")
            }
    }

    fun capture() {
        if (isProcessing) return
        isProcessing = true
        isError = false
        statusMessage = null
        result = null

        imageCapture.takePicture(
            decodeExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val rotation = image.imageInfo.rotationDegrees
                    val bitmap: Bitmap? =
                        try {
                            image.toBitmap()
                        } catch (t: Throwable) {
                            null
                        } finally {
                            image.close()
                        }

                    mainHandler.post {
                        if (bitmap == null) {
                            fail("Could not read the captured frame.")
                        } else {
                            recognise(bitmap.rotate(rotation), rotationDegrees = 0)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    mainHandler.post {
                        fail("Capture failed: ${exception.message ?: "unknown error"}")
                    }
                }
            },
        )
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(imageCapture = imageCapture)

        Column(
            modifier =
                Modifier.align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Omata",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = "Capture a photo, then tap a pill to copy its line",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
        ) {
            AnimatedVisibility(
                visible = result != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                result?.let { ocr ->
                    ResultsOverlay(
                        result = ocr,
                        onCopy = ::copyToClipboard,
                        onClose = { result = null },
                    )
                }
            }

            CaptureControls(
                isProcessing = isProcessing,
                statusText =
                    statusMessage
                        ?: if (result == null) "Point at text and tap the shutter"
                        else "Tap a pill to copy that line",
                isError = isError,
                onCapture = ::capture,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 170.dp),
        )
    }
}

@Composable
private fun CameraPreview(imageCapture: ImageCapture) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraError by remember { mutableStateOf<String?>(null) }
    val currentOnError by rememberUpdatedState { message: String -> cameraError = message }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener(
                        {
                            runCatching {
                                val provider = providerFuture.get()
                                val preview = Preview.Builder().build()
                                preview.setSurfaceProvider(surfaceProvider)
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture,
                                )
                            }
                                .onFailure { error ->
                                    currentOnError(
                                        "Camera unavailable: ${error.message ?: "unknown error"}"
                                    )
                                }
                        },
                        ContextCompat.getMainExecutor(ctx),
                    )
                }
            },
        )

        cameraError?.let { message ->
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = message,
                    color = Color(0xFFFFB4AB),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
        }
    }
}

@Composable
private fun ResultsOverlay(
    result: OcrResult,
    onCopy: (String) -> Unit,
    onClose: () -> Unit,
) {
    val pillColor = Color.Black.copy(alpha = 0.62f)
    val pillTextColor = Color(0xFFEDEFF2)
    val accent = Color(0xFF9FCAFF)

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text =
                    "${result.lines.size} ${if (result.lines.size == 1) "line" else "lines"} found",
                style = MaterialTheme.typography.labelLarge,
                color = pillTextColor,
                modifier =
                    Modifier.clip(CircleShape)
                        .background(pillColor)
                        .padding(horizontal = 14.dp, vertical = 9.dp),
            )
            Spacer(Modifier.weight(1f))
            ActionPill(
                text = "Copy all",
                containerColor = accent,
                contentColor = Color(0xFF003257),
                onClick = { onCopy(result.fullText) },
            )
            ActionPill(
                text = "✕",
                containerColor = pillColor,
                contentColor = pillTextColor,
                onClick = onClose,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 2.dp),
        ) {
            itemsIndexed(result.lines) { _, line ->
                TextPill(
                    text = line,
                    containerColor = pillColor,
                    textColor = pillTextColor,
                    actionColor = accent,
                    onClick = { onCopy(line) },
                )
            }
        }
    }
}

@Composable
private fun TextPill(
    text: String,
    containerColor: Color,
    textColor: Color,
    actionColor: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(shape)
                .background(containerColor)
                .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
                .clickable(onClick = onClick)
                .padding(start = 18.dp, end = 14.dp, top = 13.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Copy",
            color = actionColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier =
                Modifier.padding(start = 14.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ActionPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = contentColor,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier =
            Modifier.clip(CircleShape)
                .background(containerColor)
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun CaptureControls(
    isProcessing: Boolean,
    statusText: String,
    isError: Boolean,
    onCapture: () -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
                .padding(top = 28.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = statusText,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (isError) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )

        Box(
            modifier =
                Modifier.padding(top = 10.dp)
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (isProcessing) 0.12f else 0.22f))
                    .border(
                        width = 3.dp,
                        color = Color.White.copy(alpha = if (isProcessing) 0.4f else 1f),
                        shape = CircleShape,
                    )
                    .clickable(enabled = !isProcessing, onClick = onCapture),
            contentAlignment = Alignment.Center,
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(34.dp),
                    color = Color.White,
                    strokeWidth = 3.dp,
                )
            } else {
                Box(Modifier.size(58.dp).clip(CircleShape).background(Color.White))
            }
        }
    }
}

@Composable
private fun PermissionScreen(
    askedOnce: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Camera access needed",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text =
                    "Omata reads text straight from your camera. Photos are processed on " +
                        "this device, never uploaded and never saved.",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRequest, modifier = Modifier.padding(top = 24.dp)) {
                Text(if (askedOnce) "Try again" else "Allow camera")
            }
            if (askedOnce) {
                TextButton(onClick = onOpenSettings) { Text("Open app settings") }
            }
        }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
    )
}

private fun Bitmap.rotate(degrees: Int): Bitmap {
    if (degrees % 360 == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    val rotated = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    if (rotated != this) recycleQuietly()
    return rotated
}

private fun Bitmap.recycleQuietly() {
    if (!isRecycled) recycle()
}
