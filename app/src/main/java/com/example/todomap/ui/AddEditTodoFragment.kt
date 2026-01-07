package com.example.todomap.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

    companion object {
        const val ARG_TODO_ID = "todo_id"
        private const val TAG = "AddEditTodoFragment"
    }

    private var _binding: FragmentAddEditTodoBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TodoRepository
    private lateinit var geofenceHelper: GeofenceHelper

    private var editingTodoId: Long = -1L
    private var isEditMode = false

    // Location (null = not selected)
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null

    private var pendingEnableNotifications = false

    // -------------------- SPEECH RECOGNITION --------------------

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val recordAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startDictation()
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.permission_audio_required),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    // -------------------- PERMISSIONS --------------------

    private val fineLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (granted && pendingEnableNotifications) {
                ensureBackgroundLocationPermissionIfNeeded()
            } else {
                disableLocationNotifications(
                    getString(R.string.permission_location_required)
                )
            }
        }

    private val backgroundLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                disableLocationNotifications(
                    getString(R.string.permission_background_location_required)
                )
            }
        }

    // -------------------- SWITCH LISTENER --------------------

    private val notifySwitchListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->

            if (isChecked) {
                if (selectedLatitude == null || selectedLongitude == null) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.select_location_first),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.switchNotifyOnLocation.isChecked = false
                    return@OnCheckedChangeListener
                }

                pendingEnableNotifications = true
                ensureFineLocationPermission()
            } else {
                pendingEnableNotifications = false
            }
        }

    // -------------------- LIFECYCLE --------------------

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

        arguments?.let {
            editingTodoId = it.getLong(ARG_TODO_ID, -1L)
            isEditMode = editingTodoId != -1L
        }

        Log.d(TAG, "onViewCreated: isEditMode=$isEditMode editingTodoId=$editingTodoId")

        setupUI()
        setupLocationPickerResultListener()
        setupSpeechRecognizerIfAvailable()

        if (isEditMode) {
            loadTodoForEditing(editingTodoId)
        }
    }

    override fun onStop() {
        // Stop listening when leaving screen to avoid leaks and crashes
        stopDictation(resetUi = true)
        super.onStop()
    }

    override fun onDestroyView() {
        stopDictation(resetUi = true)
        speechRecognizer?.destroy()
        speechRecognizer = null

        super.onDestroyView()
        _binding = null
    }

    // -------------------- UI --------------------

    private fun setupUI() {
        binding.textViewFormTitle.text =
            getString(if (isEditMode) R.string.edit_todo else R.string.add_todo)

        binding.buttonSave.setOnClickListener { saveTodo() }
        binding.buttonCancel.setOnClickListener { findNavController().navigateUp() }
        binding.buttonSelectLocation.setOnClickListener { navigateToLocationPicker() }

        binding.buttonDictate.setOnClickListener {
            if (isListening) stopDictation(resetUi = true) else ensureRecordAudioPermissionAndStart()
        }

        binding.sliderRadius.addOnChangeListener { _, value, _ ->
            binding.textViewRadiusValue.text =
                getString(R.string.radius_meters, value.toInt())
        }

        binding.switchNotifyOnLocation.setOnCheckedChangeListener(notifySwitchListener)

        updateLocationDisplay()
    }

    // -------------------- SPEECH --------------------

    private fun setupSpeechRecognizerIfAvailable() {
        val context = requireContext()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            binding.buttonDictate.isEnabled = false
            Toast.makeText(context, getString(R.string.speech_not_available), Toast.LENGTH_LONG)
                .show()
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Speech: onReadyForSpeech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Speech: onBeginningOfSpeech")
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d(TAG, "Speech: onEndOfSpeech")
                    stopDictation(resetUi = true)
                }

                override fun onError(error: Int) {
                    Log.e(TAG, "Speech: onError=$error")
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.speech_error, error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    stopDictation(resetUi = true)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull().orEmpty()

                    Log.d(TAG, "Speech: results='$spokenText'")

                    if (spokenText.isNotBlank()) {
                        val current = binding.editTextDescription.text?.toString().orEmpty()
                        val newText = if (current.isBlank()) spokenText else "$current\n$spokenText"
                        binding.editTextDescription.setText(newText)
                        binding.editTextDescription.setSelection(newText.length)
                    }

                    stopDictation(resetUi = true)
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun ensureRecordAudioPermissionAndStart() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startDictation()
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startDictation() {
        val recognizer = speechRecognizer ?: return

        if (isListening) return

        isListening = true
        binding.buttonDictate.text = getString(R.string.speech_stop)

        Toast.makeText(requireContext(), getString(R.string.speech_start), Toast.LENGTH_SHORT).show()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        try {
            recognizer.startListening(intent)
        } catch (t: Throwable) {
            Log.e(TAG, "Speech: startListening failed", t)
            Toast.makeText(
                requireContext(),
                getString(R.string.speech_not_available),
                Toast.LENGTH_SHORT
            ).show()
            stopDictation(resetUi = true)
        }
    }

    private fun stopDictation(resetUi: Boolean) {
        if (!isListening) {
            if (resetUi && _binding != null) {
                binding.buttonDictate.text = ""
            }
            return
        }

        isListening = false

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Throwable) {
        }

        if (resetUi && _binding != null) {
            // keep icon-only button, no text
            binding.buttonDictate.text = ""
        }
    }

    // -------------------- LOCATION PICKER --------------------

    private fun setupLocationPickerResultListener() {
        setFragmentResultListener(LocationPickerFragment.REQUEST_KEY) { _, bundle ->
            val newLat = bundle.getDouble(LocationPickerFragment.RESULT_LATITUDE)
            val newLng = bundle.getDouble(LocationPickerFragment.RESULT_LONGITUDE)

            Log.d(
                TAG,
                "LocationPicker result: newLat=$newLat newLng=$newLng (oldLat=$selectedLatitude oldLng=$selectedLongitude)"
            )

            selectedLatitude = newLat
            selectedLongitude = newLng
            updateLocationDisplay()
        }
    }

    private fun navigateToLocationPicker() {
        val hasInitialLocation = selectedLatitude != null && selectedLongitude != null
        val initLat = (selectedLatitude ?: 0.0).toFloat()
        val initLng = (selectedLongitude ?: 0.0).toFloat()
        Log.d(
            TAG,
            "navigateToLocationPicker: hasInitialLocation=$hasInitialLocation initial_lat=$initLat initial_lng=$initLng"
        )

        val args = Bundle().apply {
            putFloat("initial_lat", initLat)
            putFloat("initial_lng", initLng)
            putBoolean("has_initial_location", hasInitialLocation)
        }
        findNavController().navigate(R.id.action_add_edit_to_location_picker, args)
    }

    private fun updateLocationDisplay() {
        if (selectedLatitude != null && selectedLongitude != null) {
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

    // -------------------- LOAD / SAVE --------------------

    private fun loadTodoForEditing(todoId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val todo = repository.getTodoById(todoId) ?: return@launch

            Log.d(
                TAG,
                "loadTodoForEditing: loaded id=${todo.id} lat=${todo.latitude} lng=${todo.longitude} notifyOnLocation=${todo.notifyOnLocation}"
            )

            selectedLatitude = todo.latitude
            selectedLongitude = todo.longitude

            binding.apply {
                editTextTitle.setText(todo.title)
                editTextDescription.setText(todo.description)
                editTextLocationName.setText(todo.locationName)

                switchNotifyOnLocation.setOnCheckedChangeListener(null)
                switchNotifyOnLocation.isChecked = todo.notifyOnLocation
                switchNotifyOnLocation.setOnCheckedChangeListener(notifySwitchListener)

                sliderRadius.value = todo.radiusMeters.toFloat()
                textViewRadiusValue.text =
                    getString(R.string.radius_meters, todo.radiusMeters)
            }

            updateLocationDisplay()
        }
    }

    private fun saveTodo() {
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val locationName = binding.editTextLocationName.text.toString().trim()
        val notifyOnLocation = binding.switchNotifyOnLocation.isChecked
        val radius = binding.sliderRadius.value.toInt()

        if (title.isEmpty()) {
            binding.textInputLayoutTitle.error =
                getString(R.string.error_title_required)
            return
        }
        binding.textInputLayoutTitle.error = null

        if (notifyOnLocation && selectedLatitude == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_location_required),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val latToSave = selectedLatitude ?: 0.0
        val lngToSave = selectedLongitude ?: 0.0

        Log.d(
            TAG,
            "saveTodo: isEditMode=$isEditMode id=$editingTodoId title='$title' lat=$latToSave lng=$lngToSave notifyOnLocation=$notifyOnLocation"
        )

        // Use applicationContext so callbacks remain safe even if fragment gets detached
        val appContext = requireContext().applicationContext

        viewLifecycleOwner.lifecycleScope.launch {

            if (isEditMode) {
                Log.d(TAG, "saveTodo: removing old geofence for id=$editingTodoId")
                geofenceHelper.removeGeofence(editingTodoId)
            }

            val todoItem = if (isEditMode) {
                TodoItem(
                    id = editingTodoId,
                    title = title,
                    description = description,
                    latitude = latToSave,
                    longitude = lngToSave,
                    locationName = locationName,
                    notifyOnLocation = notifyOnLocation,
                    radiusMeters = radius
                )
            } else {
                TodoItem(
                    title = title,
                    description = description,
                    latitude = latToSave,
                    longitude = lngToSave,
                    locationName = locationName,
                    notifyOnLocation = notifyOnLocation,
                    radiusMeters = radius
                )
            }

            Log.d(
                TAG,
                "saveTodo: saving to Room id=${todoItem.id} lat=${todoItem.latitude} lng=${todoItem.longitude}"
            )
            repository.saveTodo(todoItem)

            // Verify persisted values (debug)
            val persisted = repository.getTodoById(todoItem.id)
            Log.d(
                TAG,
                "saveTodo: persisted in Room id=${persisted?.id} lat=${persisted?.latitude} lng=${persisted?.longitude}"
            )

            if (notifyOnLocation && selectedLatitude != null) {
                Log.d(
                    TAG,
                    "saveTodo: adding geofence id=${todoItem.id} lat=${todoItem.latitude} lng=${todoItem.longitude} radius=${todoItem.radiusMeters}"
                )

                geofenceHelper.addGeofence(
                    todoItem,
                    onSuccess = {
                        Log.d(TAG, "saveTodo: geofence add SUCCESS for id=${todoItem.id}")
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                appContext,
                                appContext.getString(R.string.geofence_added),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onFailure = { msg ->
                        Log.e(TAG, "saveTodo: geofence add FAILURE for id=${todoItem.id}: $msg")
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                appContext,
                                appContext.getString(R.string.geofence_add_failed, msg),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )
            } else {
                Log.d(
                    TAG,
                    "saveTodo: geofence not added (notifyOnLocation=$notifyOnLocation selectedLatitude=$selectedLatitude)"
                )
            }

            Toast.makeText(
                requireContext(),
                getString(if (isEditMode) R.string.todo_updated else R.string.todo_added),
                Toast.LENGTH_SHORT
            ).show()

            Log.d(TAG, "saveTodo: navigateUp()")
            findNavController().navigateUp()
        }
    }

    // -------------------- HELPERS --------------------

    private fun ensureFineLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            ensureBackgroundLocationPermissionIfNeeded()
        } else {
            fineLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun ensureBackgroundLocationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            backgroundLocationPermissionLauncher.launch(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    private fun disableLocationNotifications(message: String) {
        pendingEnableNotifications = false
        binding.switchNotifyOnLocation.isChecked = false
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}
