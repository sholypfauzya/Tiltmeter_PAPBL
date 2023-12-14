package com.example.tiltmeter

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tiltmeter.data.Measurement
import com.example.tiltmeter.databinding.ActivityMainBinding
import com.example.tiltmeter.utils.HAWK_MEASUREMENTS
import com.example.tiltmeter.utils.LATITUDE
import com.example.tiltmeter.utils.LONGITUDE
import com.example.tiltmeter.utils.Orientation
import com.example.tiltmeter.utils.checkStoragePermission
import com.example.tiltmeter.utils.requestStoragePermission
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.orhanobut.hawk.Hawk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileWriter
import java.io.IOException

// activity class that listen to interface Orientation.Listener
class MainActivity : AppCompatActivity(), Orientation.Listener {

    // create variable for viewBinding
    private lateinit var binding: ActivityMainBinding

    // create instance for Orientation class
    private var mOrientation: Orientation? = null

    // create variable to store measurement results
    private var measurements: MutableList<Measurement> = mutableListOf()

    // create variable to store sensor data results
    private var tempMeasurements: MutableList<Double> = mutableListOf()

    // create variable to determine sensor activity status
    private var isActive: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // set viewBinding and activity view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // set status bar color to Black
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)

        // initialize orientation
        mOrientation = Orientation(this)

        with(binding) {
            // add listener to btnControl
            btnControl.setOnClickListener {
                //
                if (isActive) {
                    saveResultToPreference()
                    saveToCSV()
                    measurements.clear()
                    finish()
                }
                // toggle isActive to true or false
                isActive = !isActive
                // set btnControl text to "Stop" if isActive=true and "Start" if isActive=false
                btnControl.text = getString(if (isActive) R.string.label_stop else R.string.label_start)
            }

            btnInfo.setOnClickListener {
                HowToDialog(this@MainActivity).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // check storage permission, request storage permission if not enabled, and start listen to sensor if enabled
        if (!checkStoragePermission(this)) {
            requestStoragePermission(this)
        } else {
            mOrientation?.startListening(this)
        }
    }

    override fun onStop() {
        super.onStop()
        // stop listening to orientation on Activity Stop
        mOrientation?.stopListening()
    }

    @SuppressLint("SetTextI18n")
    // onOrientationChanged callback from Orientation.Listener
    override fun onOrientationChanged(pitch: Float, roll: Float, pitchDegree: Float, rollDegree: Float) {
        // calculate a 360-degree representation of the roll
        val degree360 = if (rollDegree > 0) rollDegree else 360 + rollDegree
        // calculate a 180-degree representation of the roll
        val degree180 = if (degree360 > 180) 360 - degree360 else degree360

        // update UI elements with the calculated degree values
        with(binding) {
            tvSlope.text = "${String.format("%.1f", degree180)}\u00B0"
            tvSlope.rotation = -degree360
            btnControl.rotation = -degree360
            horizontalView.rotation = -degree360
        }

        // if the measurement is active, record the degree value
        if (isActive) {
            // add the degree value to a temporary list
            tempMeasurements.add(degree180.toDouble())

            // get measurements average every 1 seconds
            if (tempMeasurements.size == 50) {
                val formattedDegree = String.format("%.1f", tempMeasurements.average()).toDouble()

                // add the average degree to the measurements list
                measurements.add(Measurement((measurements.size + 1), (measurements.size + 1), formattedDegree))

                // clear the temporary measurements list for the next set of measurements
                tempMeasurements.clear()
            }
        }
    }


    // onSensorNotAvailable callback from Orientation.Listener
    override fun onSensorNotAvailable() {
        // set tvSlope text to Sensor not available
        binding.tvSlope.text = getString(R.string.label_sensor_not_available)
    }

    // function to create Csv file
    private fun saveToCSV() {
        try {
            // get the external files directory and create directory if not exists
            val directory = getExternalFilesDir(null)
            if (directory?.exists() != true) {
                directory?.mkdirs()
            }

            // create or overwrite the CSV file
            val fileName = "sensor_data.csv"
            val csvFile = File(directory, fileName)

            // delete existing csv file if exists
            if (csvFile.exists()) {
                runBlocking(Dispatchers.IO) {
                    deleteCsvFile(csvFile) {
                        writeCsvFile(csvFile)
                    }
                }
            } else writeCsvFile(csvFile)
        } catch (e: IOException) {
            // show Toast if get Exception
            Toast.makeText(this, "Failed to create csv file", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun writeCsvFile(file: File) {
        // initialize FileWriter
        val fileWriter = FileWriter(file, true)
        // write CSV header
        fileWriter.append("Latitude, Longitude\n")
        fileWriter.append("${Hawk.get<Double>(LATITUDE)}, ${Hawk.get<Double>(LONGITUDE)}\n\n")

        fileWriter.append("measurement_id, second, degree\n")
        // write sensor data to the CSV file
        measurements.forEach {
            fileWriter.append("${it.measurementId}, ${it.seconds}, ${it.degree}\n")
        }

        // close the FileWriter
        fileWriter.close()

        // show Toast if Csv file created
        runOnUiThread {
            Toast.makeText(this, "Csv file created as ${file.absolutePath.substringAfterLast("/")}", Toast.LENGTH_LONG).show()
        }
    }

    // function delete csv file
    private fun deleteCsvFile(file: File, onDeleted: () -> Unit) {
        if (file.deleteRecursively()) onDeleted.invoke()
    }

    // function to save List<Measurement> to Hawk
    private fun saveResultToPreference() {
        // use Gson library to convert measurements to JSON String
        val gson = Gson()
        val type = object : TypeToken<List<Measurement>>() {}.type
        val measurements = gson.toJson(measurements, type)

        // save JSON String of measurements to Hawk
        Hawk.put(HAWK_MEASUREMENTS, measurements)
    }
}
