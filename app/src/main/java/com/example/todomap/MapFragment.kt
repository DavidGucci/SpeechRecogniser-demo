package com.example.todomap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.todomap.databinding.FragmentMapBinding
import com.example.todomap.location.LocationHelper
import com.example.todomap.model.TodoItem
import com.example.todomap.repository.TodoRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private lateinit var repository: TodoRepository
    private lateinit var locationHelper: LocationHelper

    private val defaultLocation = LatLng(46.0569, 14.5058)
    private val defaultZoom = 13f

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            enableMyLocation()
            moveToCurrentLocation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = (requireActivity().application as ToDoMapApp).todoRepository
        locationHelper = LocationHelper(requireContext())

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.onResume()
        binding.mapView.getMapAsync(this)

        binding.fabMyLocation.setOnClickListener {
            checkLocationPermissionAndMove()
        }
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

        checkLocationPermissionAndMove()
        observeTodos()
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

    private fun observeTodos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeAllTodos().collect { todos ->
                    displayTodosOnMap(todos)
                }
            }
        }
    }

    private fun displayTodosOnMap(todos: List<TodoItem>) {
        googleMap?.clear()

        todos.filter { it.latitude != 0.0 || it.longitude != 0.0 }.forEach { todo ->
            val position = LatLng(todo.latitude, todo.longitude)

            val markerColor = if (todo.isCompleted) {
                BitmapDescriptorFactory.HUE_GREEN
            } else {
                BitmapDescriptorFactory.HUE_RED
            }

            googleMap?.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(todo.title)
                    .snippet(todo.description.ifEmpty { todo.locationName })
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            )

            if (todo.notifyOnLocation && !todo.isCompleted) {
                googleMap?.addCircle(
                    CircleOptions()
                        .center(position)
                        .radius(todo.radiusMeters.toDouble())
                        .strokeColor(0x550000FF)
                        .fillColor(0x220000FF)
                        .strokeWidth(2f)
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        binding.mapView.onDestroy()
        super.onDestroyView()
        _binding = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
}