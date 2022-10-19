package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragmentDirections
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*


class SelectLocationFragment : BaseFragment() , OnMapReadyCallback ,
    LocationListener {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map :GoogleMap
    private val REQUEST_LOCATION_PERMISSION = 1
    private  val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
    private  val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
    private  val TAG = "HuntMainActivity"
    private  val LOCATION_PERMISSION_INDEX = 0
    private  val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
    private var markerCheck : Boolean = false
    private var item: PointOfInterest? = null
    private  var marker :Marker? = null

    private  val REQUEST_TURN_DEVICE_LOCATION_ON = 12433
    private  val REQUEST_CODE_BACKGROUND = 102929
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)



//        TODO: zoom to the user location after taking his permission

//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location
        binding.button.setOnClickListener {
            onLocationSelected()
        }


        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map

        val sydney = LatLng(35.0, 31.297460)

    //    this.map.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney , 20f))

        enableMyLocation()

        setMapStyle(map)
        HandlePOI()
        HandleLoc()

    }
private fun HandlePOI(){

        map.setOnPoiClickListener {
            if (isPermissionGranted()) {
            map.clear()
            item = it
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        it.latLng.latitude,
                        it.latLng.longitude
                    ), 20f
                )
            )

            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f , Long : %2$.5f",
                it.latLng.latitude, it.latLng.longitude
            )


            marker = map.addMarker(
                MarkerOptions()
                    .position(it.latLng)
                    .title(it.name)
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )!!
            markerCheck = true
        }
            else {
                notifyError()
                //enableMyLocation()
            }
    }

}
private fun HandleLoc(){

        map.setOnMapLongClickListener {
            if (isPermissionGranted()) {
            map.clear()

            val address =
                Geocoder(context, Locale.getDefault()).getFromLocation(it.latitude, it.longitude, 1)
            if (address.isNotEmpty()) {
                val customPOI = PointOfInterest(it, "Custom", address[0].getAddressLine(0))
                item = customPOI
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            customPOI.latLng.latitude,
                            customPOI.latLng.longitude
                        ), 20f
                    )
                )

                val snippet = String.format(
                    Locale.getDefault(),
                    "Lat: %1$.5f , Long : %2$.5f",
                    customPOI.latLng.latitude, customPOI.latLng.longitude
                )


                marker = map.addMarker(
                    MarkerOptions()
                        .position(customPOI.latLng)
                        .title(customPOI.name)
                        .snippet(snippet)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )!!
                markerCheck = true
            }
        }
            else {
                notifyError()

            }
    }

}
    private fun onLocationSelected() {
        if(item!= null){
            _viewModel.selectedPOI.value = item

            _viewModel.navigationCommand.postValue(NavigationCommand.Back)
            }

        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }

    private fun isPermissionGranted() : Boolean {
        return  PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if location permissions are granted and if so enable the
        // location data layer.

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
           notifyError()
        }

    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem)= when(item.itemId){
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


    override fun onLocationChanged(location: Location) {
       map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude , location.longitude) , 20f) )
    }
    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }


    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val requestBuilder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(requestBuilder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                notifyError()
            }

        }
    }

    fun notifyError(){
        Snackbar.make(
            requireActivity().findViewById(android.R.id.content),
            R.string.permission_denied_explanation,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
       //Forground
        if (isPermissionGranted()) {
            map.isMyLocationEnabled = true
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                checkDeviceLocationSettings()
            } else {
                requestQPermission()
            }

        } else {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
        //        DONE: zoom to the user location after taking his permission


        map.moveCamera(CameraUpdateFactory.zoomIn())
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun requestQPermission() {
        val hasForegroundPermission = ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasForegroundPermission) {
            val hasBackgroundPermission = ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasBackgroundPermission) {
                checkDeviceLocationSettings()
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    REQUEST_CODE_BACKGROUND
                )
            }
        }
    }

}
