package com.example.todomap.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todomap.databinding.ItemTodoBinding
import com.example.todomap.model.TodoItem

class TodoAdapter(
    private val onItemClick: (TodoItem) -> Unit,
    private val onItemLongClick: (TodoItem) -> Unit = {},
    private val onCheckChanged: (TodoItem, Boolean) -> Unit,
    private val onSelectionChanged: (selectedCount: Int) -> Unit = {}
) : ListAdapter<TodoItem, TodoAdapter.TodoViewHolder>(TodoDiffCallback()) {

    private val selectedIds = mutableSetOf<Long>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun toggleSelection(todoItem: TodoItem) {
        if (selectedIds.contains(todoItem.id)) selectedIds.remove(todoItem.id)
        else selectedIds.add(todoItem.id)
        val pos = currentList.indexOfFirst { it.id == todoItem.id }
        if (pos >= 0) notifyItemChanged(pos)
        onSelectionChanged(selectedIds.size)
    }

    fun clearSelection() {
        if (selectedIds.isEmpty()) return
        val ids = selectedIds.toSet()
        selectedIds.clear()
        ids.forEach { id ->
            val pos = currentList.indexOfFirst { it.id == id }
            if (pos >= 0) notifyItemChanged(pos)
        }
        onSelectionChanged(0)
    }

    fun getSelectedItems(): List<TodoItem> {
        return currentList.filter { selectedIds.contains(it.id) }
    }

    fun getSelectionCount(): Int = selectedIds.size

    private fun isSelectionMode(): Boolean = selectedIds.isNotEmpty()

    inner class TodoViewHolder(
        private val binding: ItemTodoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val todo = getItem(position)
                    if (isSelectionMode()) {
                        toggleSelection(todo)
                    } else {
                        onItemClick(todo)
                    }
                }
            }
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val todo = getItem(position)
                    toggleSelection(todo)
                    onItemLongClick(todo)
                }
                true
            }
        }

        fun bind(todoItem: TodoItem) {
            binding.apply {
                textViewTitle.text = todoItem.title
                textViewDescription.text = todoItem.description
                textViewLocation.text = todoItem.locationName.ifEmpty {
                    binding.root.context.getString(com.example.todomap.R.string.no_location_set)
                }

                val isSelected = selectedIds.contains(todoItem.id)
                val card = root as? com.google.android.material.card.MaterialCardView
                card?.strokeWidth = if (isSelected) {
                    (2 * root.resources.displayMetrics.density).toInt()
                } else {
                    0
                }

                val selectionMode = isSelectionMode()
                checkboxCompleted.isEnabled = !selectionMode

                checkboxCompleted.setOnCheckedChangeListener(null)
                checkboxCompleted.isChecked = todoItem.isCompleted

                checkboxCompleted.setOnCheckedChangeListener { _, isChecked ->
                    if (selectionMode) return@setOnCheckedChangeListener
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onCheckChanged(getItem(pos), isChecked)
                    }
                }

                if (todoItem.latitude != 0.0 || todoItem.longitude != 0.0) {
                    imageViewLocationIcon.visibility = View.VISIBLE
                } else {
                    imageViewLocationIcon.visibility = View.GONE
                }
            }
        }
    }

    class TodoDiffCallback : DiffUtil.ItemCallback<TodoItem>() {
        override fun areItemsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TodoItem, newItem: TodoItem): Boolean {
            return oldItem == newItem
        }
    }
}
