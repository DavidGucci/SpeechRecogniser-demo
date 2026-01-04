package com.example.todomap.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.todomap.R
import com.example.todomap.ToDoMapApp
import com.example.todomap.databinding.FragmentAddEditTodoBinding
import com.example.todomap.geofence.GeofenceHelper
import com.example.todomap.model.TodoItem
import com.example.todomap.repository.TodoRepository
import kotlinx.coroutines.launch

class AddEditTodoFragment : Fragment() {

    private var _binding: FragmentAddEditTodoBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TodoRepository
    private lateinit var geofenceHelper: GeofenceHelper
    private var editingTodoId: Long = -1L
    private var isEditMode: Boolean = false

    // Selected location coordinates
    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0

    companion object {
        const val ARG_TODO_ID = "todo_id"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditTodoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = (requireActivity().application as ToDoMapApp).todoRepository
        geofenceHelper = GeofenceHelper(requireContext())

        arguments?.let { args ->
            editingTodoId = args.getLong(ARG_TODO_ID, -1L)
            if (editingTodoId != -1L) {
                isEditMode = true
                loadTodoForEditing(editingTodoId)
            }
        }

        setupUI()
        setupLocationPickerResultListener()
    }

    private fun setupUI() {
        binding.textViewFormTitle.text = if (isEditMode) {
            getString(R.string.edit_todo)
        } else {
            getString(R.string.add_todo)
        }

        binding.buttonSave.setOnClickListener {
            saveTodo()
        }

        binding.buttonCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.buttonSelectLocation.setOnClickListener {
            navigateToLocationPicker()
        }

        binding.sliderRadius.addOnChangeListener { _, value, _ ->
            binding.textViewRadiusValue.text = getString(R.string.radius_meters, value.toInt())
        }

        updateLocationDisplay()
    }

    private fun setupLocationPickerResultListener() {
        setFragmentResultListener(LocationPickerFragment.REQUEST_KEY) { _, bundle ->
            selectedLatitude = bundle.getDouble(LocationPickerFragment.RESULT_LATITUDE, 0.0)
            selectedLongitude = bundle.getDouble(LocationPickerFragment.RESULT_LONGITUDE, 0.0)
            updateLocationDisplay()
        }
    }

    private fun navigateToLocationPicker() {
        val bundle = Bundle().apply {
            putFloat("initial_lat", selectedLatitude.toFloat())
            putFloat("initial_lng", selectedLongitude.toFloat())
        }
        findNavController().navigate(R.id.action_add_edit_to_location_picker, bundle)
    }

    private fun updateLocationDisplay() {
        if (selectedLatitude != 0.0 || selectedLongitude != 0.0) {
            binding.textViewSelectedCoordinates.visibility = View.VISIBLE
            binding.textViewSelectedCoordinates.text = getString(
                R.string.coordinates_format,
                selectedLatitude,
                selectedLongitude
            )
        } else {
            binding.textViewSelectedCoordinates.visibility = View.GONE
        }
    }

    private fun loadTodoForEditing(todoId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val todo = repository.getTodoById(todoId)
            todo?.let {
                selectedLatitude = it.latitude
                selectedLongitude = it.longitude

                binding.apply {
                    editTextTitle.setText(it.title)
                    editTextDescription.setText(it.description)
                    editTextLocationName.setText(it.locationName)
                    switchNotifyOnLocation.isChecked = it.notifyOnLocation
                    sliderRadius.value = it.radiusMeters.toFloat()
                    textViewRadiusValue.text = getString(R.string.radius_meters, it.radiusMeters)
                }

                updateLocationDisplay()
            }
        }
    }

    private fun saveTodo() {
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val locationName = binding.editTextLocationName.text.toString().trim()
        val notifyOnLocation = binding.switchNotifyOnLocation.isChecked
        val radius = binding.sliderRadius.value.toInt()

        if (title.isEmpty()) {
            binding.textInputLayoutTitle.error = getString(R.string.error_title_required)
            return
        }

        binding.textInputLayoutTitle.error = null

        viewLifecycleOwner.lifecycleScope.launch {
            val todoItem = if (isEditMode) {
                // Remove old geofence before updating
                geofenceHelper.removeGeofence(editingTodoId)

                repository.getTodoById(editingTodoId)?.copy(
                    title = title,
                    description = description,
                    latitude = selectedLatitude,
                    longitude = selectedLongitude,
                    locationName = locationName,
                    notifyOnLocation = notifyOnLocation,
                    radiusMeters = radius
                ) ?: return@launch
            } else {
                TodoItem(
                    title = title,
                    description = description,
                    latitude = selectedLatitude,
                    longitude = selectedLongitude,
                    locationName = locationName,
                    notifyOnLocation = notifyOnLocation,
                    radiusMeters = radius
                )
            }

            repository.saveTodo(todoItem)

            if (notifyOnLocation && (selectedLatitude != 0.0 || selectedLongitude != 0.0)) {
                geofenceHelper.addGeofence(todoItem)
            }

            Toast.makeText(
                requireContext(),
                if (isEditMode) getString(R.string.todo_updated) else getString(R.string.todo_added),
                Toast.LENGTH_SHORT
            ).show()

            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
