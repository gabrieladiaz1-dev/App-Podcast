package com.example.audify.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class ProximitySensorManager(context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "ProximitySensor"
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    var onNear: (() -> Unit)? = null
    var onFar: (() -> Unit)? = null

    private var isNear = false

    fun start() {
        proximitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Proximity sensor started")
        } ?: Log.w(TAG, "Proximity sensor not available")
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        Log.d(TAG, "Proximity sensor stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            val maxRange = proximitySensor?.maximumRange ?: 5f
            val nowNear = distance < maxRange / 2

            if (nowNear != isNear) {
                isNear = nowNear
                Log.d(TAG, "Proximity changed: near=$nowNear (distance=$distance)")
                if (nowNear) onNear?.invoke() else onFar?.invoke()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
