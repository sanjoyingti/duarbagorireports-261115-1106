package com.arin.civicreport.model

import kotlinx.serialization.Serializable

/** Every kind of incident the app can report. */
enum class IncidentType(val label: String, val shortLabel: String, val emoji: String, val blurb: String) {
    POTHOLE("Pothole / bad road", "Pothole", "\uD83D\uDEA7", "Broken road surface, dangerous for traffic"),
    DEFORESTATION("Deforestation / tree cutting", "Deforestation", "\uD83C\uDF32", "Illegal felling of trees or forest clearing"),
    SAND_MINING("Illegal sand mining", "Sand mining", "\u26F1", "Unauthorised extraction of sand from river beds"),
    ILLEGAL_CONSTRUCTION("Illegal construction", "Construction", "\uD83C\uDFD7", "Construction without sanction or in a restricted zone"),
    GARBAGE_DUMPING("Garbage dumping", "Garbage", "\uD83D\uDDD1", "Waste dumped on roads, drains or open plots"),
    WATERLOGGING("Waterlogging", "Waterlogging", "\uD83D\uDCA7", "Standing water after rain, blocked drains"),
    STREET_LIGHT("Street light not working", "Street light", "\uD83D\uDCA1", "Dark street, faulty or missing lamp"),
    ENCROACHMENT("Encroachment", "Encroachment", "\uD83D\uDEA7", "Footpath or public land illegally occupied"),
    NOISE_POLLUTION("Noise pollution", "Noise", "\uD83D\uDD0A", "Loudspeakers, construction noise, honking")
}

/** Which department a report of a given type must reach. */
enum class AuthorityRole(val label: String) {
    MUNICIPAL("Municipal body"),
    PWD("Public Works (PWD)"),
    TRAFFIC("Traffic police"),
    DISTRICT("District collector"),
    POWER("Electricity board"),
    WATER("Water board"),
    FOREST("Forest department"),
    MINES("Mines & geology"),
    POLLUTION("Pollution control board"),
    STATE_CM("State Chief Minister's office"),
    PMO("Prime Minister's Office"),
    CPCB("Central Pollution Control Board")
}

/** A contactable office. */
@Serializable
data class Authority(
    val id: String,
    val name: String,
    val category: String,
    val email: String? = null,
    val phone: String? = null,
    val url: String? = null
)

/** The place of a report, with both raw coordinates and a readable address. */
@Serializable
data class ReportLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val address: String = "",
    val city: String = "",
    val district: String = "",
    val state: String = "",
    val postcode: String = ""
) {
    val hasFix: Boolean get() = latitude != 0.0 || longitude != 0.0
    val oneLine: String
        get() = address.ifBlank {
            if (hasFix) "%.6f, %.6f".format(latitude, longitude) else "No location"
        }
}

/** One saved report. */
@Serializable
data class Report(
    val id: String,
    val types: List<IncidentType>,
    val createdAt: Long,
    val location: ReportLocation,
    val notes: String = "",
    val mediaUris: List<String> = emptyList(),
    val videoUri: String? = null,
    val authorities: List<Authority> = emptyList(),
    val contactEmail: String = "",
    val sendEmail: Boolean = true,
    val sendSms: Boolean = true,
    val status: ReportStatus = ReportStatus.DRAFT,
    val lastSentAt: Long = 0L,
    val sentChannels: List<String> = emptyList(),
    val mapSnapshot: String = ""
) {
    val title: String get() = types.joinToString(", ") { it.shortLabel }
    val primaryType: IncidentType? get() = types.firstOrNull()
}

@Serializable
enum class ReportStatus { DRAFT, SENT, PARTIAL }

/** Settings the person can change. */
data class AppSettings(
    val defaultChannel: SendChannel = SendChannel.BOTH,
    val notificationsEnabled: Boolean = true,
    val senderName: String = "",
    val senderEmail: String = "",
    val senderPhone: String = "",
    val highAccuracy: Boolean = true,
    val includeNationalAuthorities: Boolean = true
)

enum class SendChannel(val label: String) { EMAIL("Email"), SMS("SMS"), BOTH("Email and SMS") }
