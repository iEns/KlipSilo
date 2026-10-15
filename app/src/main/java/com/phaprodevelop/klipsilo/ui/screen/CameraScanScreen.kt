package com.phaprodevelop.klipsilo.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.phaprodevelop.klipsilo.R
import com.phaprodevelop.klipsilo.allowSensorOrientation
import com.phaprodevelop.klipsilo.applyPhonePortraitLock
import com.phaprodevelop.klipsilo.ui.components.produceLatest
import com.phaprodevelop.klipsilo.ui.components.rememberDisplayedBitmap
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

enum class ScanMode { QR, OCR }

private const val MAX_CAPTURE_DIM = 4000
private const val FALLBACK_CAPTURE_DIM = 2000

private enum class DragMode { RESIZE_LEFT, RESIZE_RIGHT, RESIZE_TOP, RESIZE_BOTTOM, MOVE }

@Composable
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
fun CameraScanScreen(
    mode: ScanMode,
    onNavigateBack: () -> Unit,
    onScanned: (String) -> Unit,
    onSaveImage: ((Bitmap, (Boolean) -> Unit) -> Unit)? = null,
    onSaveCrop: ((Bitmap, (Boolean) -> Unit) -> Unit)? = null,
    onSaveTextAsClip: ((String) -> Unit)? = null,
    onOpenTextInEditor: ((String) -> Unit)? = null,
    autoBackOnNoText: Boolean = true,
    imagePath: String? = null,
    existingImage: Boolean = false,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val activity = context as? Activity
    val displayed = rememberDisplayedBitmap()
    val capturedBitmap = displayed.bitmap
    val imageGate = remember { AtomicInteger(0) }
    DisposableEffect(Unit) {
        activity?.allowSensorOrientation()
        onDispose {
            activity?.applyPhonePortraitLock()
        }
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var scanned by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    var isCropSaved by remember { mutableStateOf(false) }
    var imageLoadFailed by remember { mutableStateOf(false) }
    var imageReady by remember { mutableStateOf(imagePath == null && !existingImage) }
    var capturing by remember { mutableStateOf(false) }
    var cameraBindFailed by remember { mutableStateOf(false) }
    val staticImage = existingImage || imagePath != null
    var extractedText by remember { mutableStateOf<String?>(null) }
    var showNoText by remember { mutableStateOf(false) }
    var cropX by remember { mutableFloatStateOf(0.15f) }
    var cropY by remember { mutableFloatStateOf(0.25f) }
    var cropW by remember { mutableFloatStateOf(0.7f) }
    var cropH by remember { mutableFloatStateOf(0.5f) }

    val density = LocalDensity.current
    val handleHitOuterPx = remember { with(density) { 72.dp.toPx() } }
    val handleHitMaxInnerPx = remember { with(density) { 32.dp.toPx() } }
    val handleVisualHalfW = remember { with(density) { 14.dp.toPx() } }
    val handleVisualHalfH = remember { with(density) { 3.dp.toPx() } }

    var permissionDeniedPermanently by remember { mutableStateOf(false) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted
            if (!granted) {
                permissionDeniedPermanently =
                    activity == null ||
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
            }
        }

    LaunchedEffect(Unit) {
        if (!hasPermission && !staticImage) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(imagePath, existingImage) {
        if (imagePath == null) {
            if (existingImage) {
                imageLoadFailed = true
                imageReady = true
            }
            return@LaunchedEffect
        }
        imageReady = false
        imageLoadFailed = false
        val loaded =
            produceLatest(
                imageGate,
                produce = { decodeSampledWithFallback(context, imagePath, MAX_CAPTURE_DIM) },
                discard = { bitmap -> bitmap?.recycle() },
            ) ?: return@LaunchedEffect
        displayed.update(loaded.value)
        imageLoadFailed = loaded.value == null
        imageReady = true
    }

    LaunchedEffect(showNoText) {
        if (showNoText) {
            kotlinx.coroutines.delay(2500)
            if (autoBackOnNoText) {
                onNavigateBack()
            } else {
                showNoText = false
            }
        }
    }

    LaunchedEffect(isCropSaved) {
        if (isCropSaved) {
            kotlinx.coroutines.delay(1200)
            isCropSaved = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (hasPermission || staticImage) {
            val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FIT_CENTER } }
            val imageCapture =
                remember {
                    ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                                .build(),
                        )
                        .build()
                }
            val mainHandler = remember { Handler(Looper.getMainLooper()) }
            if (capturedBitmap != null && mode == ScanMode.OCR) {
                val bmp = capturedBitmap!!
                val rotated = bmp
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .pointerInput(rotated) {
                            val bmpW = rotated.width.toFloat()
                            val bmpH = rotated.height.toFloat()
                            var dragMode: DragMode? = null
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    val sw = size.width.toFloat()
                                    val sh = size.height.toFloat()
                                    val imageAspect = bmpW / bmpH
                                    val screenAspect = sw / sh
                                    val dw =
                                        if (imageAspect > screenAspect) {
                                            sw
                                        } else {
                                            sh * imageAspect
                                        }
                                    val dh =
                                        if (imageAspect > screenAspect) {
                                            sw / imageAspect
                                        } else {
                                            sh
                                        }
                                    val ox =
                                        if (imageAspect > screenAspect) {
                                            0f
                                        } else {
                                            (sw - dw) / 2f
                                        }
                                    val oy =
                                        if (imageAspect > screenAspect) {
                                            (sh - dh) / 2f
                                        } else {
                                            0f
                                        }
                                    val rx = ox + dw * cropX
                                    val ry = oy + dh * cropY
                                    val rw = dw * cropW
                                    val rh = dh * cropH
                                    val px = startOffset.x
                                    val py = startOffset.y
                                    val innerX = minOf(handleHitMaxInnerPx, rw * 0.25f)
                                    val innerY = minOf(handleHitMaxInnerPx, rh * 0.25f)
                                    val nearLeft = px in (rx - handleHitOuterPx)..(rx + innerX)
                                    val nearRight = px in (rx + rw - innerX)..(rx + rw + handleHitOuterPx)
                                    val nearTop = py in (ry - handleHitOuterPx)..(ry + innerY)
                                    val nearBottom = py in (ry + rh - innerY)..(ry + rh + handleHitOuterPx)
                                    val edgeCount =
                                        listOf(nearLeft, nearRight, nearTop, nearBottom).count { it }
                                    dragMode =
                                        when {
                                            edgeCount == 1 && nearLeft -> DragMode.RESIZE_LEFT
                                            edgeCount == 1 && nearRight -> DragMode.RESIZE_RIGHT
                                            edgeCount == 1 && nearTop -> DragMode.RESIZE_TOP
                                            edgeCount == 1 && nearBottom -> DragMode.RESIZE_BOTTOM
                                            else -> DragMode.MOVE
                                        }
                                },
                                onDragEnd = { dragMode = null },
                                onDragCancel = { dragMode = null },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val sw = size.width.toFloat()
                                    val sh = size.height.toFloat()
                                    val imageAspect = bmpW / bmpH
                                    val screenAspect = sw / sh
                                    val dw =
                                        if (imageAspect > screenAspect) {
                                            sw
                                        } else {
                                            sh * imageAspect
                                        }
                                    val dh =
                                        if (imageAspect > screenAspect) {
                                            sw / imageAspect
                                        } else {
                                            sh
                                        }
                                    val dx = dragAmount.x / dw
                                    val dy = dragAmount.y / dh
                                    val minF = 0.08f

                                    when (dragMode) {
                                        DragMode.RESIZE_LEFT -> {
                                            cropX = (cropX + dx).coerceIn(0f, cropX + cropW - minF)
                                            cropW = (cropW - dx).coerceAtLeast(minF)
                                        }
                                        DragMode.RESIZE_RIGHT -> {
                                            cropW = (cropW + dx).coerceIn(minF, 1f - cropX)
                                        }
                                        DragMode.RESIZE_TOP -> {
                                            cropY = (cropY + dy).coerceIn(0f, cropY + cropH - minF)
                                            cropH = (cropH - dy).coerceAtLeast(minF)
                                        }
                                        DragMode.RESIZE_BOTTOM -> {
                                            cropH = (cropH + dy).coerceIn(minF, 1f - cropY)
                                        }
                                        else -> {
                                            cropX = (cropX + dx).coerceIn(0f, 1f - cropW)
                                            cropY = (cropY + dy).coerceIn(0f, 1f - cropH)
                                        }
                                    }
                                },
                            )
                        },
                ) {
                    Image(
                        bitmap = rotated.asImageBitmap(),
                        contentDescription = stringResource(R.string.captured),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val imageAspect = rotated.width.toFloat() / rotated.height.toFloat()
                        val screenAspect = size.width / size.height
                        val dw: Float
                        val dh: Float
                        val ox: Float
                        val oy: Float
                        if (imageAspect > screenAspect) {
                            dw = size.width
                            dh = size.width / imageAspect
                            ox = 0f
                            oy = (size.height - dh) / 2f
                        } else {
                            dh = size.height
                            dw = size.height * imageAspect
                            ox = (size.width - dw) / 2f
                            oy = 0f
                        }
                        val rw = dw * cropW
                        val rh = dh * cropH
                        val rx = ox + dw * cropX
                        val ry = oy + dh * cropY
                        drawRect(
                            Color.Black,
                            Offset(rx, ry),
                            Size(rw, rh),
                            style = Stroke(width = 4f),
                        )
                        val cx = rx + rw / 2f
                        val cy = ry + rh / 2f
                        drawRect(
                            Color.Gray,
                            Offset(cx - handleVisualHalfW, ry - handleVisualHalfH),
                            Size(handleVisualHalfW * 2f, handleVisualHalfH * 2f),
                        )
                        drawRect(
                            Color.Gray,
                            Offset(cx - handleVisualHalfW, ry + rh - handleVisualHalfH),
                            Size(handleVisualHalfW * 2f, handleVisualHalfH * 2f),
                        )
                        drawRect(
                            Color.Gray,
                            Offset(rx - handleVisualHalfH, cy - handleVisualHalfW),
                            Size(handleVisualHalfH * 2f, handleVisualHalfW * 2f),
                        )
                        drawRect(
                            Color.Gray,
                            Offset(rx + rw - handleVisualHalfH, cy - handleVisualHalfW),
                            Size(handleVisualHalfH * 2f, handleVisualHalfW * 2f),
                        )
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(16.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            if (onSaveImage != null) {
                                Button(
                                    onClick = {
                                        if (!isSaved) {
                                            onSaveImage(rotated) { ok -> isSaved = ok }
                                        }
                                    },
                                    enabled = !isSaved,
                                ) {
                                    Text(
                                        if (isSaved) {
                                            stringResource(R.string.saved)
                                        } else {
                                            stringResource(R.string.save_image)
                                        },
                                    )
                                }
                            }
                            if (onSaveCrop != null) {
                                Button(
                                    onClick = {
                                        if (!isCropSaved) {
                                            val left = (rotated.width * cropX).toInt().coerceIn(0, rotated.width - 1)
                                            val top = (rotated.height * cropY).toInt().coerceIn(0, rotated.height - 1)
                                            val w = (rotated.width * cropW).toInt().coerceIn(1, rotated.width - left)
                                            val h = (rotated.height * cropH).toInt().coerceIn(1, rotated.height - top)
                                            onSaveCrop(Bitmap.createBitmap(rotated, left, top, w, h)) { ok ->
                                                isCropSaved = ok
                                            }
                                        }
                                    },
                                    enabled = !isCropSaved,
                                ) {
                                    Text(
                                        if (isCropSaved) {
                                            stringResource(R.string.selection_saved)
                                        } else {
                                            stringResource(R.string.save_selection)
                                        },
                                    )
                                }
                            }
                            Button(onClick = {
                                val left = (rotated.width * cropX).toInt().coerceIn(0, rotated.width - 1)
                                val top = (rotated.height * cropY).toInt().coerceIn(0, rotated.height - 1)
                                val w = (rotated.width * cropW).toInt().coerceIn(1, rotated.width - left)
                                val h = (rotated.height * cropH).toInt().coerceIn(1, rotated.height - top)
                                val cropped = Bitmap.createBitmap(rotated, left, top, w, h)
                                val inputImage = InputImage.fromBitmap(cropped, 0)
                                val recognizer =
                                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                recognizer.process(inputImage)
                                    .addOnSuccessListener { visionText ->
                                        val text = visionText.text
                                        if (text.isNotBlank()) {
                                            scanned = true
                                            if (onSaveTextAsClip != null || onOpenTextInEditor != null) {
                                                extractedText = text
                                            } else {
                                                mainHandler.post { onScanned(text) }
                                            }
                                        } else {
                                            mainHandler.post { showNoText = true }
                                        }
                                    }
                                    .addOnFailureListener {
                                        mainHandler.post { showNoText = true }
                                    }
                            }) {
                                Text(stringResource(R.string.analyze))
                            }
                        }
                    }
                }
            } else if (staticImage) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (!imageReady && !imageLoadFailed) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            text =
                                stringResource(
                                    if (imagePath == null) {
                                        R.string.clip_not_found
                                    } else {
                                        R.string.file_cannot_open
                                    },
                                ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
            } else {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener(
                        {
                            val cameraProvider = cameraProviderFuture.get()
                            val previewResolution =
                                if (isLandscape) {
                                    android.util.Size(1920, 1440)
                                } else {
                                    android.util.Size(1440, 1920)
                                }
                            val preview =
                                Preview.Builder()
                                    .setTargetResolution(previewResolution)
                                    .build()
                                    .also { it.surfaceProvider = previewView.surfaceProvider }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                if (mode == ScanMode.QR) {
                                    val options =
                                        BarcodeScannerOptions.Builder()
                                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                            .build()
                                    val scanner = BarcodeScanning.getClient(options)
                                    val analysisResolution =
                                        if (isLandscape) {
                                            android.util.Size(1280, 720)
                                        } else {
                                            android.util.Size(720, 1280)
                                        }
                                    val analysis =
                                        ImageAnalysis.Builder()
                                            .setTargetResolution(analysisResolution)
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                    analysis.setAnalyzer(
                                        Executors.newSingleThreadExecutor(),
                                    ) { imageProxy ->
                                        if (scanned) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }
                                        val mediaImage =
                                            imageProxy.image
                                                ?: run {
                                                    imageProxy.close()
                                                    return@setAnalyzer
                                                }
                                        val inputImage =
                                            InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees,
                                            )
                                        scanner.process(inputImage)
                                            .addOnSuccessListener { barcodes ->
                                                for (barcode in barcodes) {
                                                    barcode.rawValue?.let { value ->
                                                        scanned = true
                                                        mainHandler.post { onScanned(value) }
                                                        return@addOnSuccessListener
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
                                    }
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        analysis,
                                    )
                                } else {
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture,
                                    )
                                }
                            } catch (_: Exception) {
                                cameraBindFailed = true
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.camera_permission_needed),
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        },
                        ContextCompat.getMainExecutor(context),
                    )
                }

                if (mode == ScanMode.OCR) {
                    Box(
                        modifier =
                            Modifier
                                .align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .border(3.dp, Color.White, CircleShape)
                                    .clickable(enabled = !capturing && !scanned) {
                                        capturing = true
                                        val photoFile =
                                            File(context.cacheDir, "ocr_${System.currentTimeMillis()}.jpg")
                                        val outputOptions =
                                            ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                        imageCapture.takePicture(
                                            outputOptions,
                                            ContextCompat.getMainExecutor(context),
                                            object : ImageCapture.OnImageSavedCallback {
                                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                    val decoded =
                                                        decodeSampledWithFallback(
                                                            context,
                                                            photoFile.absolutePath,
                                                            MAX_CAPTURE_DIM,
                                                        )
                                                    displayed.update(decoded)
                                                    photoFile.delete()
                                                    capturing = false
                                                    if (decoded == null) {
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(R.string.clip_save_failed),
                                                            Toast.LENGTH_SHORT,
                                                        ).show()
                                                    }
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    photoFile.delete()
                                                    capturing = false
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.clip_save_failed),
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                }
                                            },
                                        )
                                    },
                        ) {
                            Box(
                                Modifier
                                    .size(if (capturing) 40.dp else 54.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                            )
                        }
                    }
                }
                if (cameraBindFailed) {
                    Text(
                        text = stringResource(R.string.camera_permission_needed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(8.dp),
                                ),
                    )
                }
            }
            if (showNoText) {
                Text(
                    stringResource(R.string.no_text_found),
                    color = Color.White,
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(8.dp),
                            ),
                )
            }
        }

        extractedText?.let { text ->
            AlertDialog(
                onDismissRequest = { extractedText = null },
                title = { Text(stringResource(R.string.analyze)) },
                text = {
                    Text(
                        text,
                        maxLines = 12,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                },
                confirmButton = {
                    if (onSaveTextAsClip != null) {
                        TextButton(onClick = {
                            val t = text
                            extractedText = null
                            onSaveTextAsClip(t)
                        }) {
                            Text(stringResource(R.string.save_as_clip))
                        }
                    }
                },
                dismissButton = {
                    Row {
                        if (onOpenTextInEditor != null) {
                            TextButton(onClick = {
                                val t = text
                                extractedText = null
                                onOpenTextInEditor(t)
                            }) {
                                Text(stringResource(R.string.open_in_editor))
                            }
                        }
                        TextButton(onClick = { extractedText = null }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                },
            )
        }

        if (!hasPermission && !staticImage) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.camera_permission_needed),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.size(16.dp))
                if (permissionDeniedPermanently) {
                    Button(onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    }) {
                        Text(stringResource(R.string.open_settings))
                    }
                } else {
                    Button(onClick = {
                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }) {
                        Text(stringResource(R.string.try_again))
                    }
                    Spacer(Modifier.size(8.dp))
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    }) {
                        Text(stringResource(R.string.open_settings))
                    }
                }
            }
        }

        IconButton(
            onClick = onNavigateBack,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Color.White,
            )
        }

        if (hasPermission && !staticImage && !cameraBindFailed) {
            Column(
                modifier =
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 60.dp, top = 8.dp),
            ) {
                Text(
                    text =
                        stringResource(
                            if (mode == ScanMode.QR) R.string.tool_qr else R.string.tool_ocr_camera,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier =
                        Modifier
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector =
                            if (mode == ScanMode.QR) {
                                Icons.Filled.QrCodeScanner
                            } else {
                                Icons.Filled.DocumentScanner
                            },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text =
                            stringResource(
                                if (mode == ScanMode.QR) R.string.qr_hint else R.string.ocr_hint,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier =
                            Modifier
                                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

private fun decodeSampled(
    path: String,
    maxDim: Int,
): Bitmap? {
    return try {
        val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) > maxDim) {
            sample *= 2
        }
        val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = android.graphics.BitmapFactory.decodeFile(path, options) ?: return null
        exifRotate(bmp, path)
    } catch (_: Exception) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }
}

private fun decodeSampledWithFallback(
    context: android.content.Context,
    path: String,
    maxDim: Int,
): Bitmap? {
    var bmp = decodeSampled(path, maxDim)
    if (bmp != null) return bmp
    return try {
        System.gc()
        val fallback = decodeSampled(path, FALLBACK_CAPTURE_DIM)
        if (fallback != null) {
            Toast.makeText(
                context,
                context.getString(R.string.capture_oom_fallback),
                Toast.LENGTH_LONG,
            ).show()
        }
        fallback
    } catch (_: OutOfMemoryError) {
        Toast.makeText(
            context,
            context.getString(R.string.capture_oom_failed),
            Toast.LENGTH_LONG,
        ).show()
        null
    } catch (_: Exception) {
        null
    }
}

private fun rotateIfNeeded(bitmap: Bitmap): Bitmap {
    val matrix = android.graphics.Matrix()
    matrix.postRotate(90f)
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun exifRotate(
    bitmap: Bitmap,
    filePath: String,
): Bitmap {
    return try {
        val exif = androidx.exifinterface.media.ExifInterface(filePath)
        val orientation =
            exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL,
            )
        if (orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL) {
            return bitmap
        }
        val matrix = android.graphics.Matrix()
        when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.setScale(-1f, 1f)
                matrix.postTranslate(bitmap.width.toFloat(), 0f)
            }
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setScale(1f, -1f)
                matrix.postTranslate(0f, bitmap.height.toFloat())
            }
            androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
                matrix.postTranslate(bitmap.height.toFloat(), 0f)
            }
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(1f, -1f)
                matrix.postTranslate(0f, bitmap.width.toFloat())
            }
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            try {
                bitmap.recycle()
            } catch (_: Exception) {
            }
        }
        rotated
    } catch (_: Exception) {
        bitmap
    } catch (_: OutOfMemoryError) {
        bitmap
    }
}
