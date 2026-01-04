package com.example.todomap.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.todomap.R
import com.example.todomap.databinding.FragmentLocationPickerBinding
import com.example.todomap.location.LocationHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class LocationPickerFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentLocationPickerBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private var selectedLocation: LatLng? = null
    private lateinit var locationHelper: LocationHelper

    private val defaultLocation = LatLng(46.0569, 14.5058)
    private val defaultZoom = 13f

    companion object {
        const val REQUEST_KEY = "location_picker_request"
        const val RESULT_LATITUDE = "latitude"
        const val RESULT_LONGITUDE = "longitude"
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineLocationGranted) {
            enableMyLocation()
            moveToCurrentLocation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationHelper = LocationHelper(requireContext())

        binding.mapViewPicker.onCreate(savedInstanceState)
        binding.mapViewPicker.onResume()
        binding.mapViewPicker.getMapAsync(this)

        binding.buttonConfirmLocation.setOnClickListener {
            confirmLocation()
        }

        binding.fabMyLocationPicker.setOnClickListener {
            checkLocationPermissionAndMove()
        }

        // Initially disable confirm button
        binding.buttonConfirmLocation.isEnabled = false
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(defaultLocation, defaultZoom)
        )

        googleMap?.apply {
            uiSettings.apply {
                isZoomControlsEnabled = true
                isCompassEnabled = true
                isMyLocationButtonEnabled = false
            }
        }

        googleMap?.setOnMapClickListener { latLng ->
            selectLocation(latLng)
        }

        checkLocationPermissionAndMove()

        arguments?.let { args ->
            val lat = args.getDouble("initial_lat", 0.0)
            val lng = args.getDouble("initial_lng", 0.0)
            if (lat != 0.0 || lng != 0.0) {
                selectLocation(LatLng(lat, lng))
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f))
            }
        }
    }

    private fun selectLocation(latLng: LatLng) {
        selectedLocation = latLng

        googleMap?.clear()
        googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(getString(R.string.location_selected))
        )

        binding.textViewSelectedLocation.text = getString(R.string.location_selected)
        binding.buttonConfirmLocation.isEnabled = true
    }

    private fun confirmLocation() {
        selectedLocation?.let { location ->
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(
                    RESULT_LATITUDE to location.latitude,
                    RESULT_LONGITUDE to location.longitude
                )
            )
            findNavController().navigateUp()
        }
    }

    private fun checkLocationPermissionAndMove() {
        if (hasLocationPermission()) {
            enableMyLocation()
            moveToCurrentLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun enableMyLocation() {
        try {
            googleMap?.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun moveToCurrentLocation() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val location = locationHelper.getLastLocation()
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapViewPicker.onResume()
    }

    override fun onPause() {
        binding.mapViewPicker.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.mapViewPicker.onDestroy()
        super.onDestroyView()
        _binding = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapViewPicker.onLowMemory()
    }
}

