package com.example.todomap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todomap.adapter.TodoAdapter
import com.example.todomap.databinding.FragmentMainBinding
import com.example.todomap.model.TodoItem
import com.example.todomap.repository.TodoRepository
import com.example.todomap.ui.AddEditTodoFragment
import kotlinx.coroutines.launch

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TodoRepository
    private lateinit var todoAdapter: TodoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = (requireActivity().application as ToDoMapApp).todoRepository

        setupRecyclerView()
        setupFab()
        observeTodos()
    }

    private fun setupRecyclerView() {
        todoAdapter = TodoAdapter(
            onItemClick = { todo ->
                navigateToEdit(todo)
            },
            onItemLongClick = { todo ->
                showDeleteDialog(todo)
            },
            onCheckChanged = { todo, isChecked ->
                val updatedTodo = todo.copy(isCompleted = isChecked)
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.saveTodo(updatedTodo)
                }
            }
        )

        binding.recyclerViewTodos.apply {
            adapter = todoAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupFab() {
        binding.fabAddTodo.setOnClickListener {
            findNavController().navigate(R.id.navigation_add_edit_todo)
        }
    }

    private fun observeTodos() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeAllTodos().collect { todos ->
                    todoAdapter.submitList(todos)

                    if (todos.isEmpty()) {
                        binding.textViewEmptyState.visibility = View.VISIBLE
                        binding.recyclerViewTodos.visibility = View.GONE
                    } else {
                        binding.textViewEmptyState.visibility = View.GONE
                        binding.recyclerViewTodos.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun navigateToEdit(todo: TodoItem) {
        val bundle = Bundle().apply {
            putLong(AddEditTodoFragment.ARG_TODO_ID, todo.id)
        }
        findNavController().navigate(R.id.navigation_add_edit_todo, bundle)
    }

    private fun showDeleteDialog(todo: TodoItem) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_todo_title)
            .setMessage(R.string.delete_todo_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.deleteTodo(todo)
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}