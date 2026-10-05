package de.wcinfo.app.data.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.StringRes
import de.wcinfo.app.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class CompassData(
    val deviceHeading: Float,
    val targetBearing: Float,
    val relativeAngle: Float,
    val distanceMeters: Float
) {
    val isFacingTarget: Boolean
        get() = kotlin.math.abs(relativeAngle) <= 15f || kotlin.math.abs(relativeAngle - 360f) <= 15f

    val hasArrived: Boolean
        get() = distanceMeters in 0f..10f
}

class CompassSensorManager(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun getHeadingUpdates(): Flow<Float> = callbackFlow {
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var lastHeading = 0f

        val listener = object : SensorEventListener {
            private val lastAccelerometer = FloatArray(3)
            private val lastMagnetometer = FloatArray(3)
            private var lastAccelerometerSet = false
            private var lastMagnetometerSet = false

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val rawAzimuthDeg = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
                    // Smooth filter
                    lastHeading = smoothAngle(lastHeading, rawAzimuthDeg, 0.2f)
                    trySend(lastHeading)
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                    lastAccelerometerSet = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                    lastMagnetometerSet = true
                }

                if (rotationVectorSensor == null && lastAccelerometerSet && lastMagnetometerSet) {
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)) {
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val rawAzimuthDeg = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
                        lastHeading = smoothAngle(lastHeading, rawAzimuthDeg, 0.2f)
                        trySend(lastHeading)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            if (accelerometer != null) {
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            }
            if (magnetometer != null) {
                sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
            }
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    private fun smoothAngle(current: Float, target: Float, factor: Float): Float {
        var diff = (target - current) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return (current + diff * factor + 360f) % 360f
    }

    companion object {
        fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
            val toRad = { deg: Double -> Math.toRadians(deg) }
            val toDeg = { rad: Double -> Math.toDegrees(rad) }

            val dLon = toRad(lon2 - lon1)
            val phi1 = toRad(lat1)
            val phi2 = toRad(lat2)

            val y = sin(dLon) * cos(phi2)
            val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
            val radians = atan2(y, x)
            return ((toDeg(radians) + 360.0) % 360.0).toFloat()
        }

        @StringRes
        fun getDirectionResId(relativeAngle: Float, distanceMeters: Float): Int {
            if (distanceMeters in 0f..10f) {
                return R.string.arrived
            }
            val angle = (relativeAngle + 360f) % 360f
            return when {
                angle >= 345f || angle < 15f -> R.string.straight_ahead
                angle in 15f..<75f -> R.string.slight_right
                angle in 75f..<105f -> R.string.right
                angle in 105f..<165f -> R.string.sharp_right
                angle in 165f..<195f -> R.string.behind_you
                angle in 195f..<255f -> R.string.sharp_left
                angle in 255f..<285f -> R.string.left
                else -> R.string.slight_left
            }
        }

        fun getDirectionDescription(relativeAngle: Float, distanceMeters: Float, isGerman: Boolean = true): String {
            if (distanceMeters in 0f..10f) {
                return if (isGerman) "Du hast das Ziel erreicht!" else "You arrived at destination!"
            }
            val angle = (relativeAngle + 360f) % 360f
            return if (isGerman) {
                when {
                    angle >= 345f || angle < 15f -> "Geradeaus"
                    angle in 15f..<75f -> "Halb rechts"
                    angle in 75f..<105f -> "Rechts"
                    angle in 105f..<165f -> "Scharf rechts"
                    angle in 165f..<195f -> "Hinter dir"
                    angle in 195f..<255f -> "Scharf links"
                    angle in 255f..<285f -> "Links"
                    else -> "Halb links"
                }
            } else {
                when {
                    angle >= 345f || angle < 15f -> "Straight ahead"
                    angle in 15f..<75f -> "Slight right"
                    angle in 75f..<105f -> "Right"
                    angle in 105f..<165f -> "Sharp right"
                    angle in 165f..<195f -> "Behind you"
                    angle in 195f..<255f -> "Sharp left"
                    angle in 255f..<285f -> "Left"
                    else -> "Slight left"
                }
            }
        }
    }
}
