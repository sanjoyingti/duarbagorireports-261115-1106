package com.arin.civicreport.data

import android.content.Context
import com.arin.civicreport.model.Authority
import com.arin.civicreport.model.AuthorityRole
import com.arin.civicreport.model.IncidentType
import com.arin.civicreport.model.ReportLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AuthorityFinder(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private var cache: Catalog? = null

    private data class CityContacts(
        val city: String,
        val state: String,
        val byRole: Map<AuthorityRole, Authority>
    )

    private data class StateContacts(
        val state: String,
        val cm: Authority,
        val forest: Authority,
        val mines: Authority,
        val pollution: Authority
    )

    private data class Catalog(
        val defaults: Map<AuthorityRole, Authority>,
        val cities: List<CityContacts>,
        val states: List<StateContacts>,
        val national: Map<AuthorityRole, Authority>
    )

    private fun readAsset(name: String): String? =
        runCatching { context.assets.open(name).bufferedReader().use { it.readText() } }.getOrNull()

    private fun authority(id: String, name: String, category: String, node: JsonObject): Authority =
        Authority(
            id = id,
            name = name,
            category = category,
            email = node["email"]?.jsonPrimitive?.contentOrNullSafe(),
            phone = node["phone"]?.jsonPrimitive?.contentOrNullSafe()
        )

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
        runCatching { content }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun load(): Catalog {
        cache?.let { return it }
        val roleKeys = mapOf(
            "municipal" to AuthorityRole.MUNICIPAL,
            "pwd" to AuthorityRole.PWD,
            "traffic" to AuthorityRole.TRAFFIC,
            "district" to AuthorityRole.DISTRICT,
            "power" to AuthorityRole.POWER,
            "water" to AuthorityRole.WATER
        )
        val defaults = mutableMapOf<AuthorityRole, Authority>()
        val cities = mutableListOf<CityContacts>()
        val states = mutableListOf<StateContacts>()
        val national = mutableMapOf<AuthorityRole, Authority>()

        readAsset("authorities_local.json")?.let { text ->
            runCatching {
                val root = json.parseToJsonElement(text).jsonObject
                root["defaults"]?.jsonObject?.forEach { (key, value) ->
                    val role = roleKeys[key] ?: return@forEach
                    val obj = value.jsonObject
                    defaults[role] = authority(
                        id = "default-$key",
                        name = obj["name"]?.jsonPrimitive?.contentOrNullSafe() ?: return@forEach,
                        category = "Local",
                        node = obj
                    )
                }
                root["cities"]?.jsonArray?.forEach { element ->
                    val obj = element.jsonObject
                    val city = obj["city"]?.jsonPrimitive?.contentOrNullSafe() ?: return@forEach
                    val state = obj["state"]?.jsonPrimitive?.contentOrNullSafe().orEmpty()
                    val byRole = mutableMapOf<AuthorityRole, Authority>()
                    roleKeys.forEach { (key, role) ->
                        obj[key]?.jsonObject?.let { node ->
                            val name = node["name"]?.jsonPrimitive?.contentOrNullSafe() ?: return@let
                            byRole[role] = authority("$city-$key", name, "Local", node)
                        }
                    }
                    cities += CityContacts(city, state, byRole)
                }
            }
        }

        readAsset("authorities_states.json")?.let { text ->
            runCatching {
                val root = json.parseToJsonElement(text).jsonObject
                root["states"]?.jsonArray?.forEach { element ->
                    val obj = element.jsonObject
                    val state = obj["state"]?.jsonPrimitive?.contentOrNullSafe() ?: return@forEach
                    fun node(key: String, role: AuthorityRole): Authority? {
                        val n = obj[key]?.jsonObject ?: return null
                        val name = n["name"]?.jsonPrimitive?.contentOrNullSafe() ?: return null
                        return authority("$state-$key", name, role.label, n)
                    }
                    val cm = node("cm", AuthorityRole.STATE_CM) ?: return@forEach
                    val forest = node("forest", AuthorityRole.FOREST) ?: return@forEach
                    val mines = node("mines", AuthorityRole.MINES) ?: return@forEach
                    val pollution = node("pollution", AuthorityRole.POLLUTION) ?: return@forEach
                    states += StateContacts(state, cm, forest, mines, pollution)
                }
            }
        }

        readAsset("authorities_national.json")?.let { text ->
            runCatching {
                val root = json.parseToJsonElement(text).jsonObject
                root["central"]?.jsonArray?.forEach { element ->
                    val obj = element.jsonObject
                    val id = obj["id"]?.jsonPrimitive?.contentOrNullSafe() ?: return@forEach
                    val name = obj["name"]?.jsonPrimitive?.contentOrNullSafe() ?: return@forEach
                    val role = when (id) {
                        "pmo" -> AuthorityRole.PMO
                        "cpcb" -> AuthorityRole.CPCB
                        else -> return@forEach
                    }
                    national[role] = authority(id, name, "National", obj)
                }
            }
        }

        return Catalog(defaults, cities, states, national).also { cache = it }
    }

    private fun findCity(cities: List<CityContacts>, location: ReportLocation): CityContacts? {
        val city = location.city.trim().lowercase()
        val district = location.district.trim().lowercase()
        val address = location.address.lowercase()
        return cities.firstOrNull { it.city.lowercase() == city }
            ?: cities.firstOrNull { city.isNotBlank() && it.city.lowercase().contains(city) }
            ?: cities.firstOrNull { district.isNotBlank() && it.city.lowercase() == district }
            ?: cities.firstOrNull { district.isNotBlank() && it.city.lowercase().contains(district) }
            ?: cities.firstOrNull { it.city.isNotBlank() && address.contains(it.city.lowercase()) }
    }

    private fun findState(states: List<StateContacts>, location: ReportLocation): StateContacts? {
        val state = location.state.trim().lowercase()
        if (state.isNotBlank()) {
            states.firstOrNull { it.state.lowercase() == state }?.let { return it }
        }
        val address = location.address.lowercase()
        return states.firstOrNull { address.contains(it.state.lowercase()) }
    }

    /**
     * Builds the authority list for the given incident types and place.
     * Always includes the state CM office and the PMO, plus the activity-specific
     * departments matched from the detected jurisdiction.
     */
    fun findAuthorities(
        types: List<IncidentType>,
        location: ReportLocation,
        includeNational: Boolean
    ): List<Authority> {
        val catalog = load()
        val roles = types.flatMap { AuthorityRules.rolesFor(it) }.toSet().toMutableSet()
        val city = findCity(catalog.cities, location)
        val state = findState(catalog.states, location)

        val result = mutableListOf<Authority>()
        val seen = mutableSetOf<String>()

        fun add(a: Authority?) {
            if (a == null) return
            if (a.email.isNullOrBlank() && a.phone.isNullOrBlank()) return
            val key = a.name.lowercase()
            if (seen.add(key)) result += a
        }

        // Activity-specific state departments first.
        roles.forEach { role ->
            when (role) {
                AuthorityRole.FOREST -> add(state?.forest)
                AuthorityRole.MINES -> add(state?.mines)
                AuthorityRole.POLLUTION -> add(state?.pollution)
                else -> Unit
            }
        }
        // Local bodies for the detected city, else the generic defaults.
        roles.forEach { role ->
            if (role in setOf(
                    AuthorityRole.MUNICIPAL, AuthorityRole.PWD, AuthorityRole.TRAFFIC,
                    AuthorityRole.DISTRICT, AuthorityRole.POWER, AuthorityRole.WATER
                )
            ) {
                add(city?.byRole?.get(role) ?: catalog.defaults[role])
            }
        }
        // Higher offices are always notified.
        add(state?.cm)
        if (includeNational) {
            add(catalog.national[AuthorityRole.CPCB])
            add(catalog.national[AuthorityRole.PMO])
        }
        return result
    }
}
