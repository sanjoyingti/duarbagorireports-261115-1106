package com.arin.civicreport.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.Report
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SendHelper(private val context: Context) {

    private val stamp: String
        get() = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

    fun buildBody(report: Report): String {
        val lines = StringBuilder()
        lines.appendLine("CIVIC / ENVIRONMENTAL COMPLAINT")
        lines.appendLine("Via Duarbagori Reports")
        lines.appendLine()
        lines.appendLine("Issues: ${report.types.joinToString(", ") { it.label }}")
        lines.appendLine("Reported on: $stamp")
        val loc = report.location
        lines.appendLine("Location: ${loc.oneLine}")
        if (loc.hasFix) {
            lines.appendLine("Coordinates: %.6f, %.6f (±%.0f m)".format(loc.latitude, loc.longitude, loc.accuracy))
            lines.appendLine("Map: https://maps.google.com/?q=${loc.latitude},${loc.longitude}")
        }
        if (loc.city.isNotBlank() || loc.district.isNotBlank() || loc.state.isNotBlank()) {
            lines.appendLine(
                "Jurisdiction: " + listOf(loc.city, loc.district, loc.state, loc.postcode)
                    .filter { it.isNotBlank() }.joinToString(", ")
            )
        }
        if (report.notes.isNotBlank()) {
            lines.appendLine()
            lines.appendLine("Details:")
            lines.appendLine(report.notes)
        }
        if (report.authorities.isNotEmpty()) {
            lines.appendLine()
            lines.appendLine("Addressed to:")
            report.authorities.forEach { lines.appendLine("- ${it.name}") }
        }
        lines.appendLine()
        lines.appendLine("Photos/videos are attached where the device allows it.")
        return lines.toString()
    }

    fun emailsOf(authorities: List<Authority>): List<String> =
        authorities.mapNotNull { it.email?.takeIf { e -> e.isNotBlank() } }.distinct()

    fun phonesOf(authorities: List<Authority>): List<String> =
        authorities.mapNotNull { it.phone?.takeIf { p -> p.isNotBlank() } }.distinct()

    /** Opens the mail app with recipients, subject, body and media attached. */
    fun sendEmail(report: Report, subject: String, body: String, attachMedia: Boolean = true): Boolean {
        val recipients = emailsOf(report.authorities)
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, recipients.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            if (attachMedia) {
                val uris = mediaUris(report)
                if (uris.isNotEmpty()) {
                    type = "multipart/mixed"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chosen = listOf(
            Intent.createChooser(intent, "Send complaint").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return runCatching { context.startActivity(chosen.first()) }.isSuccess
    }

    fun sendSms(report: Report, body: String): Boolean {
        val numbers = phonesOf(report.authorities)
        if (numbers.isEmpty()) return false
        var ok = true
        numbers.take(4).forEach { number ->
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${number.replace(" ", "")}")
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (runCatching { context.startActivity(intent) }.isFailure) ok = false
        }
        return ok
    }

    fun openUrl(url: String): Boolean =
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess

    fun dial(phone: String): Boolean =
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.replace(" ", "")}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess

    fun smsTo(phone: String, body: String): Boolean =
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${phone.replace(" ", "")}")).apply {
                    putExtra("sms_body", body)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.isSuccess

    private fun mediaUris(report: Report): List<Uri> {
        val list = mutableListOf<Uri>()
        report.videoUri?.let { path ->
            fileUri(path)?.let { list += it }
        }
        report.mediaUris.forEach { path -> fileUri(path)?.let { list += it } }
        return list
    }

    private fun fileUri(path: String): Uri? {
        val file = File(path)
        if (!file.exists()) return null
        return runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    }
}
