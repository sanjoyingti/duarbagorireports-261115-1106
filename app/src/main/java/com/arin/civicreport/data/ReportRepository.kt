package com.arin.civicreport.data

import android.content.Context
import com.arin.civicreport.model.Report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

class ReportRepository(private val context: Context) {

    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true; encodeDefaults = true }
    private val file = File(context.filesDir, "reports.json")
    private val lock = Mutex()
    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()
    private var loaded = false

    suspend fun load() = withContext(Dispatchers.IO) {
        lock.withLock {
            if (loaded) return@withLock
            loaded = true
            val text = runCatching { if (file.exists()) file.readText() else "" }.getOrDefault("")
            _reports.value = if (text.isBlank()) emptyList() else runCatching {
                json.decodeFromString<List<Report>>(text)
            }.getOrDefault(emptyList()).sortedByDescending { it.createdAt }
        }
    }

    private suspend fun persist(list: List<Report>) = withContext(Dispatchers.IO) {
        lock.withLock {
            runCatching { file.writeText(json.encodeToString(list)) }
        }
    }

    suspend fun upsert(report: Report) {
        val next = _reports.value.filterNot { it.id == report.id } + report
        _reports.value = next.sortedByDescending { it.createdAt }
        persist(_reports.value)
    }

    suspend fun delete(id: String) {
        _reports.value = _reports.value.filterNot { it.id == id }
        persist(_reports.value)
    }

    fun find(id: String): Report? = _reports.value.firstOrNull { it.id == id }
}
