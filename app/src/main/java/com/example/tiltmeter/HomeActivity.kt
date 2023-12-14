package com.example.tiltmeter

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.tiltmeter.data.Measurement
import com.example.tiltmeter.data.toEntry
import com.example.tiltmeter.databinding.ActivityHomeBinding
import com.example.tiltmeter.utils.DegreeValueFormatter
import com.example.tiltmeter.utils.HAWK_MEASUREMENTS
import com.example.tiltmeter.utils.LATITUDE
import com.example.tiltmeter.utils.LONGITUDE
import com.example.tiltmeter.utils.TimeValueFormatter
import com.example.tiltmeter.utils.checkLocationPermission
import com.example.tiltmeter.utils.getBitmapDescriptorFromResource
import com.example.tiltmeter.utils.requestLocationPermission
import com.example.tiltmeter.utils.requestStoragePermission
import com.example.tiltmeter.utils.startActivity
import com.github.mikephil.charting.charts.ScatterChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.orhanobut.hawk.Hawk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    // create variable for viewBinding
    private lateinit var binding: ActivityHomeBinding

    // create variable of last measurement data
    private var lastData: List<Measurement> = listOf()

    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var googleMap: GoogleMap? = null
    private var userMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // set viewBinding and activity view
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initMap()
        initAction()
    }

    override fun onResume() {
        super.onResume()
        checkRequiredPermission()
        // retrieve stored data from Hawk
        val hawkData = Hawk.get<String>(HAWK_MEASUREMENTS)
        // check if there is stored data
        if (hawkData != null) {
            // deserialize JSON data using Gson library
            val gson = Gson()
            val type = object : TypeToken<List<Measurement>>() {}.type
            lastData = gson.fromJson(hawkData, type)
            // display the last stored data in the tvLastData
            binding.tvLastData.text = lastData.joinToString("\n")
            setupLineChart(lastData)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        runBlocking {
            delay(500)
            getCurrentLocation()
        }
    }

    private fun checkRequiredPermission() {
        requestStoragePermission(this)
        requestLocationPermission(this)
    }

    private fun initMap() {
        val map = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        map.getMapAsync(this)
        binding.mapTouch.outerScrollView = binding.root
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun initAction() {
        with(binding) {
            scrollHelperData.outerScrollView = root
            scrollHelperChart.outerScrollView = root
            // navigate to MainActivity to listen the Sensor
            btnMeasure.setOnClickListener {
                this@HomeActivity.startActivity<MainActivity>()
            }

            fabMyLocation.setOnClickListener {
                getCurrentLocation()
            }
        }
    }

    private fun getCurrentLocation() {
        if (checkLocationPermission(this)) {
            fusedLocationProviderClient?.lastLocation?.addOnCompleteListener {
                val location = it.result
                Hawk.put(LATITUDE, location.latitude)
                Hawk.put(LONGITUDE, location.longitude)
                updateMyLocationPosition(LatLng(location.latitude, location.longitude))
            }
        }
    }

    private fun updateMyLocationPosition(position: LatLng) {
        if (googleMap == null) return
        if (userMarker == null) userMarker = googleMap?.addMarker(
            MarkerOptions().position(position).title(getString(R.string.label_my_location))
                .icon(getBitmapDescriptorFromResource(this, R.drawable.ic_my_location_24))
        )
        else userMarker?.position = position

        moveCameraTo(position)
    }

    private fun moveCameraTo(latLng: LatLng) {
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 19f))
    }

    private fun setupLineChart(measurements: List<Measurement>) {
        val entries = measurements.map { it.toEntry() }

        val normalEntries = entries.filter { it.y < 18.0 }
        val warningEntries = entries.filter { it.y in 18.0..22.0 }
        val dangerEntries = entries.filter { it.y in 22.1..27.0 }
        val veryDangerEntries = entries.filter { it.y > 27.0 }

        val normalDataSet = ScatterDataSet(normalEntries, "Normal")
        val warningDataSet = ScatterDataSet(warningEntries, "Warning")
        val dangerDataSet = ScatterDataSet(dangerEntries, "Danger")
        val veryDangerDataSet = ScatterDataSet(veryDangerEntries, "Very Danger")

        normalDataSet.setColorByType(Color.GREEN)
        warningDataSet.setColorByType(Color.YELLOW)
        dangerDataSet.setColorByType(Color.RED)
        veryDangerDataSet.setColorByType(Color.BLACK)

        val scatterData = ScatterData(normalDataSet, warningDataSet, dangerDataSet, veryDangerDataSet)

        with(binding.scatterChart) {
            setBackgroundColor(Color.GRAY)
            data = scatterData
            description.text = "Time vs Degree"
            description.textSize = 10f
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1F
            xAxis.valueFormatter = TimeValueFormatter() // Implement a custom time formatter if needed
            axisRight.valueFormatter = DegreeValueFormatter()
            axisLeft.isEnabled = false
            xAxis.gridColor = Color.WHITE
            axisRight.gridColor = Color.WHITE

            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
            legend.textSize = 10F
            legend.form = Legend.LegendForm.CIRCLE

            visibility = View.VISIBLE

            animateXY(1500, 1500)
            invalidate() // Refresh the chart
        }
    }

    private fun ScatterDataSet.setColorByType(color: Int) {
        valueTextSize = 12f
        this.color = color
        setScatterShape(ScatterChart.ScatterShape.CIRCLE)
        scatterShapeSize = 14f
    }
}