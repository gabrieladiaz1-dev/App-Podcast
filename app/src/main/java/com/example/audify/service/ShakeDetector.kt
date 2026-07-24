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
        private const val SHAKE_THRESHOLD = 15f
        private const val SHAKE_COUNT_THRESHOLD = 2
        private const val SHAKE_TIMEOUT_MS = 800L
        private const val MIN_TIME_BETWEEN_SHAKES_MS = 300L
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    var onDoubleShake: (() -> Unit)? = null

    private var shakeCount = 0
    private var lastShakeTime = 0L
    private var lastRotationTime = 0L
    private var lastMagnitude = 0f
    private var peakDetected = false

    fun start() {
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d(TAG, "Gyroscope started")
        } ?: Log.w(TAG, "Gyroscope not available")
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        shakeCount = 0
        Log.d(TAG, "Gyroscope stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_GYROSCOPE) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        if (magnitude > SHAKE_THRESHOLD && !peakDetected) {
            peakDetected = true
            if (now - lastRotationTime > MIN_TIME_BETWEEN_SHAKES_MS) {
                shakeCount++
                lastRotationTime = now
                Log.d(TAG, "Shake detected (#$shakeCount)")
            }
        }

        if (magnitude < SHAKE_THRESHOLD / 2) {
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

        lastMagnitude = magnitude
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
