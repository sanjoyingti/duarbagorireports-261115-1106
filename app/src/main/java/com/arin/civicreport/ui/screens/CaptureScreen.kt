package com.arin.civicreport.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arin.civicreport.data.ExifGeotagger
import com.arin.civicreport.model.ReportLocation
import com.arin.civicreport.ui.AppScaffold
import com.arin.civicreport.ui.components.CameraHandle
import com.arin.civicreport.ui.components.CameraPane
import com.arin.civicreport.ui.components.LocationMap
import com.arin.civicreport.ui.components.PhotoThumb
import com.arin.civicreport.ui.components.openMediaFile
import com.arin.civicreport.viewmodel.ReportViewModel
import kotlinx.coroutines.launch

@Composable
fun CaptureScreen(vm: ReportViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val cameraHandle = remember { CameraHandle() }

    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var audioGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraGranted = granted }

    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> audioGranted = granted }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationGranted = granted
        if (granted) vm.refreshLocationAndAuthorities()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val path = vm.mediaStore.import(uri)
                if (path != null) vm.addPhoto(path) else snackbar.showSnackbar("Photo could not be copied.")
            }
        }
    }

    AppScaffold(
        screenTitle = "Photo and exact location",
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!cameraGranted) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Camera access is needed to photograph the problem.")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { cameraLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Allow camera")
                        }
                    }
                }
            } else {
                CameraPane(
                    handle = cameraHandle,
                    onError = { message ->
                        scope.launch { snackbar.showSnackbar(message) }
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val fix = draft.location
                            if (!fix.hasFix) {
                                scope.launch {
                                    snackbar.showSnackbar("Get the location first so the photo is geotagged.")
                                }
                            }
                            val file = vm.mediaStore.newPhotoFile()
                            cameraHandle.takePhoto(
                                context = context,
                                output = file,
                                onSaved = { saved ->
                                    if (fix.hasFix) {
                                        ExifGeotagger.tagPhoto(saved.absolutePath, fix.latitude, fix.longitude)
                                    }
                                    vm.addPhoto(saved.absolutePath)
                                    scope.launch { snackbar.showSnackbar("Photo saved with the location.") }
                                },
                                onError = { message -> scope.launch { snackbar.showSnackbar(message) } }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Photo")
                    }
                    Button(
                        onClick = {
                            if (cameraHandle.isRecording) {
                                cameraHandle.stopRecording()
                            } else {
                                if (!audioGranted) audioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                val file = vm.mediaStore.newVideoFile()
                                cameraHandle.startRecording(
                                    context = context,
                                    output = file,
                                    withAudio = audioGranted,
                                    onFinished = { success ->
                                        if (success) {
                                            vm.setVideo(file.absolutePath)
                                            scope.launch { snackbar.showSnackbar("Video saved.") }
                                        } else {
                                            file.delete()
                                            scope.launch { snackbar.showSnackbar("Video recording failed.") }
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (cameraHandle.isRecording) Icons.Filled.Stop else Icons.Filled.Videocam,
                            contentDescription = null
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(if (cameraHandle.isRecording) "Stop" else "Video")
                    }
                    IconButton(onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = "Pick from gallery")
                    }
                }
            }

            if (draft.photos.isNotEmpty() || draft.video != null) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(draft.photos) { path ->
                        Box {
                            PhotoThumb(path)
                            IconButton(
                                onClick = { vm.removePhoto(path) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove photo")
                            }
                        }
                    }
                    draft.video?.let { path ->
                        item {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.FiberManualRecord, contentDescription = null)
                                    Text("Video", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Exact location", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = {
                            if (!locationGranted) locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            else vm.refreshLocationAndAuthorities()
                        }) {
                            Icon(Icons.Filled.MyLocation, contentDescription = "Get location")
                        }
                    }
                    if (draft.locating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Finding your position…")
                        }
                    }
                    LocationMapText(draft.location, vm)
                    draft.locationError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    if (!locationGranted) {
                        Text(
                            "Location permission is off. Allow it so the complaint carries exact coordinates.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                            modifier = Modifier.padding(top = 6.dp)
                        ) { Text("Allow location") }
                    }
                }
            }

            if (draft.location.hasFix) {
                LocationMap(
                    latitude = draft.location.latitude,
                    longitude = draft.location.longitude,
                    onPick = { lat, lon -> vm.refineLocation(lat, lon) }
                )
                Text(
                    "Tap the map to move the pin if the fix is slightly off.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = draft.notes,
                onValueChange = vm::setNotes,
                label = { Text("What exactly is wrong? (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onContinue,
                enabled = draft.canContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Find the responsible offices")
            }
            if (!draft.canContinue) {
                Text(
                    "Tick a problem type and get a location fix to continue.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LocationMapText(location: ReportLocation, vm: ReportViewModel) {
    Column {
        Text(
            location.oneLine,
            style = MaterialTheme.typography.bodyLarge
        )
        if (location.hasFix) {
            Text(
                "%.6f, %.6f  ±%.0f m".format(location.latitude, location.longitude, location.accuracy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val jurisdiction = listOf(location.city, location.district, location.state, location.postcode)
            .filter { it.isNotBlank() }
        if (jurisdiction.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(
                    "Detected jurisdiction: ${jurisdiction.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        if (!location.hasFix) {
            Text(
                "No coordinates yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (location.city.isNotBlank()) {
            Text(
                "Open map pin: tap the map below to refine.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        vm.hashCode()
    }
}
