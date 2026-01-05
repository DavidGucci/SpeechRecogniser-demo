package com.example.todomap

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.todomap.databinding.FragmentSettingsBinding
import com.example.todomap.settings.ThemePreferences

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val PREFS_SETTINGS = "settings_prefs"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_DEFAULT_RADIUS = "default_radius"
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

        loadSettingsIntoUi()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        syncThemeSwitchWithCurrentMode()
    }

    private fun getPrefs() = requireContext().getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)

    private fun loadSettingsIntoUi() {
        val prefs = getPrefs()

        binding.switchNotifications.isChecked = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        binding.sliderDefaultRadius.value = prefs.getInt(KEY_DEFAULT_RADIUS, 100).toFloat()
        updateRadiusText(binding.sliderDefaultRadius.value.toInt())

        ThemePreferences.applyTheme(requireContext())
        syncThemeSwitchWithCurrentMode()
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

        setupThemeSwitchListener()
    }

    private fun setupThemeSwitchListener() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val newMode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }

            ThemePreferences.setThemeMode(requireContext(), newMode)
            AppCompatDelegate.setDefaultNightMode(newMode)

            syncThemeSwitchWithCurrentMode()
        }
    }

    private fun syncThemeSwitchWithCurrentMode() {
        val mode = AppCompatDelegate.getDefaultNightMode()
        val isDark = when (mode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_UNSPECIFIED -> {
                val nightMask = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMask == Configuration.UI_MODE_NIGHT_YES
            }
            else -> {
                val nightMask = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightMask == Configuration.UI_MODE_NIGHT_YES
            }
        }

        if (binding.switchDarkMode.isChecked != isDark) {
            binding.switchDarkMode.setOnCheckedChangeListener(null)
            binding.switchDarkMode.isChecked = isDark
            setupThemeSwitchListener()
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
