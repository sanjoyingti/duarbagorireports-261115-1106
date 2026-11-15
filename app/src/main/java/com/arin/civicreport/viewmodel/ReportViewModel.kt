package com.arin.civicreport.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arin.civicreport.CivicReportApp
import com.arin.civicreport.data.AuthorityRules
import com.arin.civicreport.model.AppSettings
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.Report
import com.arin.civicreport.model.ReportLocation
import com.arin.civicreport.model.ReportStatus
import com.arin.civicreport.model.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class DraftState(
    val types: Set<IncidentType> = emptySet(),
    val location: ReportLocation = ReportLocation(),
    val notes: String = "",
    val photos: List<String> = emptyList(),
    val video: String? = null,
    val authorities: List<Authority> = emptyList(),
    val selectedAuthorityIds: Set<String> = emptySet(),
    val locating: Boolean = false,
    val locationError: String? = null,
    val searchingAuthorities: Boolean = false,
    val loadError: String? = null
) {
    val canContinue: Boolean get() = types.isNotEmpty() && location.hasFix
}

class ReportViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as CivicReportApp).container

    val settingsStore = container.settings
    val locationHelper = container.location
    val authorityFinder = container.authorities
    val sendHelper = container.sender
    val detector = container.detector
    val notifications = container.notifications
    val mediaStore = container.media

    private val _draft = MutableStateFlow(DraftState())
    val draft: StateFlow<DraftState> = _draft.asStateFlow()

    val reports: StateFlow<List<Report>> = container.reports.reports

    val settings: StateFlow<AppSettings> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _sendResult = MutableStateFlow<String?>(null)
    val sendResult: StateFlow<String?> = _sendResult.asStateFlow()

    val defaultChannel: StateFlow<SendChannel> = settings
        .map { it.defaultChannel }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SendChannel.BOTH)

    init {
        viewModelScope.launch { container.reports.load() }
        container.notifications.ensureChannel()
    }

    // ---------- draft editing ----------

    fun toggleType(type: IncidentType) {
        _draft.update { d ->
            val next = if (type in d.types) d.types - type else d.types + type
            d.copy(types = next, authorities = emptyList(), selectedAuthorityIds = emptySet())
        }
        refreshLocationAndAuthorities()
    }

    fun setNotes(text: String) = _draft.update { it.copy(notes = text) }

    fun addPhoto(path: String) = _draft.update { it.copy(photos = it.photos + path) }

    fun removePhoto(path: String) {
        mediaStore.deleteFile(path)
        _draft.update { it.copy(photos = it.photos.filterNot { p -> p == path }) }
    }

    fun setVideo(path: String?) {
        _draft.update { d ->
            d.video?.let { old -> if (old != path) mediaStore.deleteFile(old) }
            d.copy(video = path)
        }
    }

    fun toggleAuthority(id: String) = _draft.update { d ->
        val next =
            if (id in d.selectedAuthorityIds) d.selectedAuthorityIds - id else d.selectedAuthorityIds + id
        d.copy(selectedAuthorityIds = next)
    }

    fun selectAllAuthorities(select: Boolean) = _draft.update { d ->
        d.copy(selectedAuthorityIds = if (select) d.authorities.map { it.id }.toSet() else emptySet())
    }

    fun updateLocation(location: ReportLocation) = _draft.update { it.copy(location = location) }

    fun clearDraft() = _draft.update { DraftState() }

    fun clearSendResult() {
        _sendResult.value = null
    }

    // ---------- location ----------

    fun refreshLocationAndAuthorities() {
        if (_draft.value.types.isEmpty()) return
        if (_draft.value.location.hasFix) {
            searchAuthorities()
            return
        }
        _draft.update { it.copy(locating = true, locationError = null) }
        viewModelScope.launch {
            val setting = settings.value
            val pos = locationHelper.currentPosition(setting.highAccuracy)
            if (pos == null) {
                _draft.update {
                    it.copy(
                        locating = false,
                        locationError = "Could not get a GPS fix. Turn on location and try again."
                    )
                }
                return@launch
            }
            val geocoded = locationHelper.reverseGeocode(pos.latitude, pos.longitude)
                .copy(accuracy = pos.accuracy)
            _draft.update { it.copy(location = geocoded, locating = false, locationError = null) }
            runAuthoritySearch()
        }
    }

    /** Moves the pin and re-detects the jurisdiction and authorities. */
    fun refineLocation(latitude: Double, longitude: Double) {
        _draft.update {
            it.copy(
                locating = true,
                location = it.location.copy(latitude = latitude, longitude = longitude)
            )
        }
        viewModelScope.launch {
            val geocoded = locationHelper.reverseGeocode(latitude, longitude)
            _draft.update { it.copy(location = geocoded, locating = false) }
            runAuthoritySearch()
        }
    }

    fun searchAuthorities() {
        viewModelScope.launch { runAuthoritySearch() }
    }

    private suspend fun runAuthoritySearch() {
        val d = _draft.value
        if (d.types.isEmpty()) return
        _draft.update { it.copy(searchingAuthorities = true, authorities = emptyList()) }
        val found = authorityFinder.findAuthorities(
            types = d.types.toList(),
            location = d.location,
            includeNational = settings.value.includeNationalAuthorities
        )
        _draft.update {
            it.copy(
                authorities = found,
                selectedAuthorityIds = found.map { a -> a.id }.toSet(),
                searchingAuthorities = false,
                loadError = if (found.isEmpty()) {
                    "No office matched this place. Move the pin or add the address by hand."
                } else null
            )
        }
    }

    fun detectActivitySuggestion(): List<IncidentType> = detector.suggestedTypes()

    fun rolesFor(type: IncidentType) = AuthorityRules.rolesFor(type)

    // ---------- sending ----------

    fun buildReport(): Report? {
        val d = _draft.value
        if (d.types.isEmpty() || !d.location.hasFix) return null
        val selected = d.authorities.filter { it.id in d.selectedAuthorityIds }
        val channel = settings.value.defaultChannel
        return Report(
            id = UUID.randomUUID().toString(),
            types = d.types.toList(),
            createdAt = System.currentTimeMillis(),
            location = d.location,
            notes = d.notes.trim(),
            mediaUris = d.photos,
            videoUri = d.video,
            authorities = selected,
            sendEmail = channel != SendChannel.SMS,
            sendSms = channel != SendChannel.EMAIL
        )
    }

    fun sendCurrent(report: Report, subject: String, body: String, channel: SendChannel): Boolean {
        var ok = false
        if (channel == SendChannel.EMAIL || channel == SendChannel.BOTH) {
            ok = sendHelper.sendEmail(report, subject, body) || ok
        }
        if (channel == SendChannel.SMS || channel == SendChannel.BOTH) {
            val smsOk = sendHelper.sendSms(report, body)
            // The SMS chooser may swallow the mail app, so only treat SMS as failure
            // when nothing at all could be opened.
            ok = ok || smsOk
        }
        if (ok) {
            if (settings.value.notificationsEnabled) {
                notifications.notifyReportSent(report.title, report.authorities.size)
            }
            _sendResult.value = "Complaint handed to your app for sending."
        } else {
            _sendResult.value = "No mail or SMS app could be opened for this report."
        }
        return ok
    }

    suspend fun persist(report: Report) = container.reports.upsert(report)

    fun resend(report: Report): Boolean {
        val channel = when {
            report.sendEmail && report.sendSms -> SendChannel.BOTH
            report.sendSms -> SendChannel.SMS
            else -> SendChannel.EMAIL
        }
        val subject = "Complaint: ${report.title}"
        val body = sendHelper.buildBody(report)
        val ok = sendCurrent(report, subject, body, channel)
        if (ok) {
            viewModelScope.launch {
                container.reports.upsert(
                    report.copy(status = ReportStatus.SENT, lastSentAt = System.currentTimeMillis())
                )
            }
        }
        return ok
    }

    fun deleteReport(report: Report) {
        report.mediaUris.forEach { mediaStore.deleteFile(it) }
        report.videoUri?.let { mediaStore.deleteFile(it) }
        viewModelScope.launch { container.reports.delete(report.id) }
    }

    // ---------- settings ----------

    fun saveSettings(next: AppSettings) {
        viewModelScope.launch { settingsStore.update { next } }
    }

    fun previewAuthorities(): List<Authority> {
        val d = _draft.value
        if (d.types.isEmpty()) return emptyList()
        return authorityFinder.findAuthorities(d.types.toList(), d.location, true)
    }
}
