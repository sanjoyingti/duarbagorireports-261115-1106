package com.arin.civicreport.data

import com.arin.civicreport.model.AuthorityRole
import com.arin.civicreport.model.IncidentType

/** Which roles must be notified for each incident type. */
object AuthorityRules {
    fun rolesFor(type: IncidentType): List<AuthorityRole> = when (type) {
        IncidentType.POTHOLE -> listOf(AuthorityRole.PWD, AuthorityRole.MUNICIPAL, AuthorityRole.DISTRICT)
        IncidentType.DEFORESTATION -> listOf(AuthorityRole.FOREST, AuthorityRole.DISTRICT, AuthorityRole.POLLUTION)
        IncidentType.SAND_MINING -> listOf(AuthorityRole.MINES, AuthorityRole.DISTRICT, AuthorityRole.POLLUTION)
        IncidentType.ILLEGAL_CONSTRUCTION -> listOf(AuthorityRole.MUNICIPAL, AuthorityRole.PWD, AuthorityRole.DISTRICT)
        IncidentType.GARBAGE_DUMPING -> listOf(AuthorityRole.MUNICIPAL, AuthorityRole.POLLUTION)
        IncidentType.WATERLOGGING -> listOf(AuthorityRole.MUNICIPAL, AuthorityRole.WATER)
        IncidentType.STREET_LIGHT -> listOf(AuthorityRole.POWER, AuthorityRole.MUNICIPAL)
        IncidentType.ENCROACHMENT -> listOf(AuthorityRole.MUNICIPAL, AuthorityRole.TRAFFIC, AuthorityRole.DISTRICT)
        IncidentType.NOISE_POLLUTION -> listOf(AuthorityRole.POLLUTION, AuthorityRole.MUNICIPAL)
    }
}
