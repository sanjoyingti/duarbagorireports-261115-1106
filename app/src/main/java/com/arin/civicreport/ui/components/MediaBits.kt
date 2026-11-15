package com.arin.civicreport.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.arin.civicreport.model.ReportStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))

fun openMediaFile(context: Context, path: String) {
    runCatching {
        val file = File(path)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            type = if (path.endsWith(".mp4", true)) "video/*" else "image/*"
        }
        context.startActivity(intent)
    }
}

@Composable
fun PhotoThumb(path: String, boxSize: Dp = 84.dp) {
    val bitmap = remember(path) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val sample = when {
            bounds.outWidth > 512 -> 4
            bounds.outWidth > 256 -> 2
            else -> 1
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        runCatching { BitmapFactory.decodeFile(path, options) }.getOrNull()
    }
    Box(
        modifier = Modifier
            .size(boxSize)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(boxSize)
            )
        } else {
            Text(text = "…", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun StatusChip(status: ReportStatus) {
    val color = when (status) {
        ReportStatus.SENT -> MaterialTheme.colorScheme.primary
        ReportStatus.PARTIAL -> MaterialTheme.colorScheme.secondary
        ReportStatus.DRAFT -> MaterialTheme.colorScheme.outline
    }
    Surface(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(50)) {
        Text(
            text = when (status) {
                ReportStatus.SENT -> "Sent"
                ReportStatus.PARTIAL -> "Partly sent"
                ReportStatus.DRAFT -> "Draft"
            },
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}
