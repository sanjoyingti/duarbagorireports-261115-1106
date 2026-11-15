package com.arin.civicreport.ui.components

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraHandle {
    var isRecording by mutableStateOf(false)
        internal set
    internal var imageCapture: ImageCapture? = null
    internal var recorder: Recorder? = null
    internal var activeRecording: Recording? = null
    internal var provider: ProcessCameraProvider? = null

    @SuppressLint("MissingPermission")
    fun takePhoto(context: Context, output: File, onSaved: (File) -> Unit, onError: (String) -> Unit) {
        val capture = imageCapture ?: run {
            onError("Camera is not ready yet.")
            return
        }
        val executor = ContextCompat.getMainExecutor(context)
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(output).build(),
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(results: ImageCapture.OutputFileResults) = onSaved(output)
                override fun onError(exception: ImageCaptureException) =
                    onError(exception.message ?: "Photo could not be saved.")
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun startRecording(
        context: Context,
        output: File,
        withAudio: Boolean,
        onFinished: (Boolean) -> Unit
    ) {
        val rec = recorder ?: run {
            onFinished(false)
            return
        }
        val pending = try {
            val options = FileOutputOptions.Builder(output).build()
            if (withAudio) rec.prepareRecording(context, options).withAudioEnabled()
            else rec.prepareRecording(context, options)
        } catch (e: Exception) {
            onFinished(false)
            return
        }
        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Finalize) {
                isRecording = false
                onFinished(!event.hasError())
            }
        }
        isRecording = true
    }

    fun stopRecording() {
        runCatching { activeRecording?.stop() }
        activeRecording = null
    }
}

@Composable
fun CameraPane(
    handle: CameraHandle,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(300.dp)
        .clip(RoundedCornerShape(16.dp))
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(Unit) {
        val provider = try {
            ProcessCameraProvider.getInstance(context).await(context)
        } catch (e: Exception) {
            onError("Camera could not start: ${e.message}")
            return@LaunchedEffect
        }
        handle.provider = provider
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(Quality.HD, androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))
            )
            .build()
        val videoCapture = VideoCapture.withOutput(recorder)
        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
                videoCapture
            )
            handle.imageCapture = imageCapture
            handle.recorder = recorder
        } catch (e: Exception) {
            onError("Camera could not be bound: ${e.message}")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            handle.stopRecording()
            runCatching { handle.provider?.unbindAll() }
            handle.imageCapture = null
            handle.recorder = null
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    }
}

private suspend fun <T> com.google.common.util.concurrent.ListenableFuture<T>.await(
    context: Context
): T = suspendCancellableCoroutine { cont ->
    addListener(
        {
            try {
                cont.resume(get())
            } catch (t: Throwable) {
                cont.resumeWithException(t)
            }
        },
        ContextCompat.getMainExecutor(context)
    )
}
