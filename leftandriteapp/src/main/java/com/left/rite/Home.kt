package com.left.rite

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.clustering.ClusterManager
import com.left.rite.place.Place
import com.left.rite.place.PlaceRenderer
import com.left.rite.place.PlacesReader
import fr.quentinklein.slt.LocationTracker
import fr.quentinklein.slt.ProviderError
import java.util.Collections


class Home : Fragment(), View.OnClickListener, OnMapReadyCallback, LocationTracker.Listener {
    private var binding: View? = null

    private val places: List<Place> by lazy {
        PlacesReader(requireContext()).read()
    }
    private lateinit var mMap: GoogleMap

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionCode = 1

    val polylineOptions = PolylineOptions()
        .color(Color.BLUE)
        .width(10f)

    var mCurrLocationMarker: Marker? = null

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = inflater.inflate(R.layout.home, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
        locationTracker.addListener(this)
        this.binding = binding
        return binding
    }
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                locationPermissionCode
            )
        } else {
            enableMyLocation()
        }

        mMap.uiSettings.isMyLocationButtonEnabled = true

        addClusteredMarkers(googleMap)
//        locationTracker.startListening(requireContext())
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        mMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener(requireActivity()) { location: Location? ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            }
        }
    }

    /**
     * Adds markers to the map with clustering support.
     */
    private fun addClusteredMarkers(googleMap: GoogleMap) {
        // Create the ClusterManager class and set the custom renderer
        val clusterManager = ClusterManager<Place>(activity, googleMap)
        clusterManager.renderer =
            PlaceRenderer(
                requireContext(),
                googleMap,
                clusterManager
            )

        // Set custom info window adapter
        clusterManager.markerCollection.setInfoWindowAdapter(MarkerInfoWindowAdapter(requireContext()))

        // Add the places to the ClusterManager
        clusterManager.addItems(places)
        clusterManager.cluster()

        // When the camera starts moving, change the alpha value of the marker to translucent
        googleMap.setOnCameraMoveStartedListener {
            clusterManager.markerCollection.markers.forEach { it.alpha = 0.3f }
            clusterManager.clusterMarkerCollection.markers.forEach { it.alpha = 0.3f }
        }

        googleMap.setOnCameraIdleListener {
            // When the camera stops moving, change the alpha value back to opaque
            clusterManager.markerCollection.markers.forEach { it.alpha = 1.0f }
            clusterManager.clusterMarkerCollection.markers.forEach { it.alpha = 1.0f }

            // Call clusterManager.onCameraIdle() when the camera stops moving so that re-clustering
            // can be performed when the camera stops moving
            clusterManager.onCameraIdle()
        }
    }

    override fun onClick(view: View) {
    }

    override fun onStart() {
        super.onStart()
        refreshPermissions(true)
    }

    private fun refreshPermissions(request: Boolean) {
        val statusText: String
        val statusColor: Int
        if (permitted(request)) {
            statusText = "Permissions: Granted"
            statusColor = Color.GREEN
        } else {
            statusText = "Permissions: Missing"
            statusColor = Color.RED
        }
        println("Permissions: $statusText $statusColor")
    }

    private fun permitted(request: Boolean): Boolean {
        val list: MutableList<String> = mutableListOf()
        var granted = true
        for (item: VersionedPermission in PERMISSIONS) {
            list.add(item.permission)
            if (Build.VERSION.SDK_INT >= item.version && ContextCompat.checkSelfPermission(
                    requireActivity().applicationContext,
                    item.permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                granted = false
            }
        }
        if (!granted) {
            if (request) {
                requestPermissions.launch(list.toTypedArray())
            }
        }
        return granted
    }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var granted = true
            for (permission in permissions) {
                granted = granted && permission.value
            }
            if (!granted) {
                Guardian.say(
                    requireActivity().applicationContext,
                    android.util.Log.ERROR,
                    TAG,
                    "ERROR: Permissions were not granted"
                )
            }
            refreshPermissions(false)
        }

    override fun onDestroyView() {
        super.onDestroyView()
        locationTracker.stopListening()
        binding = null
    }

    class VersionedPermission(val permission: String, val version: Int)

    companion object {
        private val PERMISSIONS: Array<VersionedPermission> = arrayOf(
            VersionedPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.CALL_PHONE,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.CHANGE_WIFI_STATE,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.INTERNET,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.READ_CONTACTS,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.READ_PHONE_STATE,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.READ_CALL_LOG,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                "android.permission.ANSWER_PHONE_CALLS",
                Build.VERSION_CODES.O
            ),
            VersionedPermission(
                Manifest.permission.RECEIVE_BOOT_COMPLETED,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.RECEIVE_SMS,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                Manifest.permission.SEND_SMS,
                Build.VERSION_CODES.JELLY_BEAN
            ),
            VersionedPermission(
                "android.permission.FOREGROUND_SERVICE",
                Build.VERSION_CODES.P
            ),
        )

        private val TAG: String = Home::class.java.simpleName
        const val REQUEST_CODE_LOCATION = 123

        private val locationTracker: LocationTracker = LocationTracker(
            minTimeBetweenUpdates = 1 * 60 * 1000.toLong(), minDistanceBetweenUpdates = 1f
        );

        @SuppressLint("MissingPermission")
        internal fun setLocationTrackingEnabled(context: Context) {
            locationTracker.startListening(context)
        }

        internal fun setLocationTrackingDisabled() {
            locationTracker.stopListening()
        }
    }

    val locationList : MutableList<LatLng> = mutableListOf()


    override fun onLocationFound(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        if (locationList.size > 0) {
            val prevLatLng = locationList.get(locationList.size -1)
            if (getDistanceBetweenPoints(prevLatLng, latLng) > 1.0f) {
                locationList.add(latLng)
            }
        } else {
            locationList.add(latLng)
        }

        println("Location found: $location")
        println("location found List: ${locationList.size}")
        val markerOptions =
            MarkerOptions().title("Current Position").icon(
            BitmapHelper.vectorToBitmap(requireContext(), R.drawable.ic_directions_bike_black_24dp, Color.BLACK)
        )
        markerOptions.position(latLng)
        mCurrLocationMarker = mMap.addMarker(markerOptions)
        polylineOptions.addAll(locationList.toList())
        mMap.addPolyline(polylineOptions)
        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(200f));
    }

    // distance in meters
    fun getDistanceBetweenPoints(prevLoc: LatLng, currLoc: LatLng): Float {
        val location1 = Location("location1")
        location1.latitude = prevLoc.latitude
        location1.longitude = prevLoc.longitude

        val location2 = Location("location2")
        location2.latitude = currLoc.latitude
        location2.longitude = currLoc.longitude

        return location1.distanceTo(location2)
    }

    override fun onProviderError(providerError: ProviderError) {
        println("Provider error: $providerError")
    }

}