package com.example.audify.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

class ShakeDetector(context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "ShakeDetector"
        private const val SHAKE_COUNT_THRESHOLD = 2
        private const val SHAKE_TIMEOUT_MS = 800L
        private const val MIN_TIME_BETWEEN_SHAKES_MS = 300L
        private const val GYRO_THRESHOLD = 12f
        private const val ACCEL_THRESHOLD = 25f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val isGyroscope = sensor?.type == Sensor.TYPE_GYROSCOPE
    private val threshold = if (isGyroscope) GYRO_THRESHOLD else ACCEL_THRESHOLD

    var onDoubleShake: (() -> Unit)? = null

    private var shakeCount = 0
    private var lastShakeTime = 0L
    private var lastRotationTime = 0L
    private var peakDetected = false

    fun start() {
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d(TAG, "Sensor started: ${it.name} (threshold=$threshold)")
        } ?: Log.w(TAG, "No shake sensor available")
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        shakeCount = 0
        Log.d(TAG, "Shake sensor stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val type = event?.sensor?.type ?: return
        if (type != Sensor.TYPE_GYROSCOPE && type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        if (magnitude > threshold && !peakDetected) {
            peakDetected = true
            if (now - lastRotationTime > MIN_TIME_BETWEEN_SHAKES_MS) {
                shakeCount++
                lastRotationTime = now
                Log.d(TAG, "Shake detected (#$shakeCount, mag=$magnitude)")
            }
        }

        if (magnitude < threshold / 2) {
            peakDetected = false
        }

        if (shakeCount >= SHAKE_COUNT_THRESHOLD) {
            val timeSinceFirstShake = now - lastShakeTime
            if (timeSinceFirstShake < SHAKE_TIMEOUT_MS || lastShakeTime == 0L) {
                Log.d(TAG, "Double shake confirmed!")
                onDoubleShake?.invoke()
                shakeCount = 0
                lastShakeTime = 0L
            }
        }

        if (shakeCount == 1 && lastShakeTime == 0L) {
            lastShakeTime = now
        }

        if (shakeCount > 0 && now - lastShakeTime > SHAKE_TIMEOUT_MS) {
            shakeCount = 0
            lastShakeTime = 0L
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
