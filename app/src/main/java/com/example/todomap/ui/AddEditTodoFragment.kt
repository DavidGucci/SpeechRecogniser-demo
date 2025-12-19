package com.example.todomap.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.todomap.R
import com.example.todomap.databinding.FragmentAddEditTodoBinding
import com.example.todomap.model.TodoItem
import com.example.todomap.repository.TodoRepository

class AddEditTodoFragment : Fragment() {

    private var _binding: FragmentAddEditTodoBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TodoRepository
    private var editingTodoId: Long = -1L
    private var isEditMode: Boolean = false

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

        repository = TodoRepository(requireContext())

        arguments?.let { args ->
            editingTodoId = args.getLong(ARG_TODO_ID, -1L)
            if (editingTodoId != -1L) {
                isEditMode = true
                loadTodoForEditing(editingTodoId)
            }
        }

        setupUI()
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

        }
    }

    private fun loadTodoForEditing(todoId: Long) {
        val todo = repository.getTodoById(todoId)
        todo?.let {
            binding.apply {
                editTextTitle.setText(it.title)
                editTextDescription.setText(it.description)
                editTextLocationName.setText(it.locationName)
                switchNotifyOnLocation.isChecked = it.notifyOnLocation
                sliderRadius.value = it.radiusMeters.toFloat()
                textViewRadiusValue.text = getString(R.string.radius_meters, it.radiusMeters)
            }
        }
    }

    private fun saveTodo() {
        val title = binding.editTextTitle.text.toString().trim()
        val description = binding.editTextDescription.text.toString().trim()
        val locationName = binding.editTextLocationName.text.toString().trim()
        val notifyOnLocation = binding.switchNotifyOnLocation.isChecked
        val radius = binding.sliderRadius.value.toInt()

        // Validation
        if (title.isEmpty()) {
            binding.textInputLayoutTitle.error = getString(R.string.error_title_required)
            return
        }

        binding.textInputLayoutTitle.error = null

        val todoItem = if (isEditMode) {
            repository.getTodoById(editingTodoId)?.copy(
                title = title,
                description = description,
                locationName = locationName,
                notifyOnLocation = notifyOnLocation,
                radiusMeters = radius
            ) ?: return
        } else {
            TodoItem(
                title = title,
                description = description,
                locationName = locationName,
                notifyOnLocation = notifyOnLocation,
                radiusMeters = radius
            )
        }

        repository.saveTodo(todoItem)

        Toast.makeText(
            requireContext(),
            if (isEditMode) getString(R.string.todo_updated) else getString(R.string.todo_added),
            Toast.LENGTH_SHORT
        ).show()

        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

