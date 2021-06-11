package com.cse.coari.activity


import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cse.coari.R
import kotlinx.android.synthetic.main.activity_navigation.*
import kotlin.math.abs

class ARDistance : AppCompatActivity(), SensorEventListener {

    /* Sensor */
    private val sensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private var mAccelerometer: Sensor? = null
    private var mMagnetField: Sensor? = null

    /*Sensor variables*/
    private val mAccValues = FloatArray(3)
    private val mMagnetValues = FloatArray(3)
    private val mR = FloatArray(9)
    private val mOrientation = FloatArray(3)
    private var accRunning = false
    private var magnetRunning = false
    val REQUEST_IMAGE_CAPTURE = 1

    /* distance &  */
    var distance: Double = 0.0
    var azimuthDegrees = 0f
    var direction: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mMagnetField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, mMagnetField, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {

        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        if (event!!.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mAccValues, 0, event.values.size)
            if (!accRunning) accRunning = true
        } else if (event.sensor == mMagnetField) {
            System.arraycopy(event.values, 0, mMagnetValues, 0, event.values.size)
            if (!magnetRunning) magnetRunning = true
        }

        if(accRunning || magnetRunning) {
            SensorManager.getRotationMatrix(mR, null, mAccValues, mMagnetValues)

            azimuthDegrees = ((Math.toDegrees(SensorManager.getOrientation(mR, mOrientation)[0].toDouble()) + 360).toInt() % 360).toFloat()

            var pose_x = 1.1
            var destinationAnchorPost = 0.8

            distance = pose_x - destinationAnchorPost * 8 // 1px = 8cm

            if (azimuthDegrees >= 0 && azimuthDegrees < 180) {
                if(distance < 0) direction = "right"
                else direction = "left"

            } else if(azimuthDegrees >= 180 && azimuthDegrees < 360) {
                if(distance < 0) direction = "left"
                else direction = "right"
            }
            distance = abs(distance)
        }
        navi_roomNum.text = direction
        navi_roomDistance.text = distance.toString()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}