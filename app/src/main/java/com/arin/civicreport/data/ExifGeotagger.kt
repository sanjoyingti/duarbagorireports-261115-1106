package com.arin.civicreport.data

import android.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Stamps a saved photo with the exact GPS fix taken at capture time.
 *
 * `android.media.ExifInterface` has no `setLatLong`, so the GPS tags are written
 * by hand in the standard degrees/minutes/seconds rational form together with the
 * hemisphere reference tags.
 */
object ExifGeotagger {

    fun tagPhoto(path: String, latitude: Double, longitude: Double) {
        if (latitude == 0.0 && longitude == 0.0) return
        runCatching {
            val exif = ExifInterface(path)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, rational(latitude))
            exif.setAttribute(
                ExifInterface.TAG_GPS_LATITUDE_REF,
                if (latitude >= 0) "N" else "S"
            )
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, rational(longitude))
            exif.setAttribute(
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                if (longitude >= 0) "E" else "W"
            )
            exif.setAttribute(
                ExifInterface.TAG_DATETIME,
                SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
            )
            exif.setAttribute(ExifInterface.TAG_MAKE, "Duarbagori Reports")
            exif.saveAttributes()
        }
    }

    /** Converts a decimal degree into the "deg/1,min/1,sec/100" tag format. */
    private fun rational(value: Double): String {
        val absolute = kotlin.math.abs(value)
        val degrees = absolute.toInt()
        val minutesFull = (absolute - degrees) * 60.0
        val minutes = minutesFull.toInt()
        val seconds = (minutesFull - minutes) * 60.0
        return "$degrees/1,$minutes/1,${(seconds * 100).roundToInt()}/100"
    }
}
