package com.example.todomap

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.todomap.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val PREFS_SETTINGS = "settings_prefs"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_DEFAULT_RADIUS = "default_radius"
        private const val KEY_DARK_MODE = "dark_mode"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSettings()
        setupListeners()
    }

    private fun getPrefs() = requireContext().getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)

    private fun loadSettings() {
        val prefs = getPrefs()

        binding.switchNotifications.isChecked = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        binding.sliderDefaultRadius.value = prefs.getInt(KEY_DEFAULT_RADIUS, 100).toFloat()
        binding.switchDarkMode.isChecked = prefs.getBoolean(KEY_DARK_MODE, false)

        updateRadiusText(binding.sliderDefaultRadius.value.toInt())
    }

    private fun setupListeners() {
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            getPrefs().edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked).apply()
        }

        binding.sliderDefaultRadius.addOnChangeListener { _, value, _ ->
            val radius = value.toInt()
            updateRadiusText(radius)
            getPrefs().edit().putInt(KEY_DEFAULT_RADIUS, radius).apply()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            getPrefs().edit().putBoolean(KEY_DARK_MODE, isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun updateRadiusText(radius: Int) {
        binding.textViewRadiusValue.text = getString(R.string.radius_meters, radius)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
