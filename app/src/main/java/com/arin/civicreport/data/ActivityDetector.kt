package com.arin.civicreport.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.arin.civicreport.model.IncidentType

/**
 * Uses the accelerometer to guess whether the person is walking, in a vehicle,
 * or standing still, and suggests incident types that fit that context.
 */
class ActivityDetector(private val context: Context) : SensorEventListener {

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    private var lastMagnitude = 0f
    private var variance = 0f
    private var samples = 0

    fun start() {
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        runCatching { sensorManager?.unregisterListener(this) }
        samples = 0
        variance = 0f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val e = event ?: return
        val x = e.values[0]
        val y = e.values[1]
        val z = e.values[2]
        val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
        val delta = kotlin.math.abs(magnitude - lastMagnitude)
        lastMagnitude = magnitude
        if (samples < 200) {
            samples++
            variance += (delta - variance) / samples
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** 0 = still, 1 = walking, 2 = vehicle. */
    fun movementClass(): Int = when {
        variance > 2.5f -> 2
        variance > 0.6f -> 1
        else -> 0
    }

    fun suggestedTypes(): List<IncidentType> = when (movementClass()) {
        2 -> listOf(IncidentType.POTHOLE, IncidentType.WATERLOGGING, IncidentType.ENCROACHMENT)
        1 -> listOf(IncidentType.GARBAGE_DUMPING, IncidentType.STREET_LIGHT, IncidentType.ENCROACHMENT)
        else -> listOf(IncidentType.NOISE_POLLUTION, IncidentType.ILLEGAL_CONSTRUCTION)
    }

    fun movementLabel(): String = when (movementClass()) {
        2 -> "Moving fast (in a vehicle)"
        1 -> "Walking"
        else -> "Standing still"
    }
}
