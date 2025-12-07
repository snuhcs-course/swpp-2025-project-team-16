package com.fitquest.app.ui.fragments.shared.exercise

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.fitquest.app.util.ActivityUtils

class ExerciseSpinnerManager(
    private val context: Context,
    private val spinner: Spinner
) {
    private val activityKeys = ActivityUtils.activityMetadataMap.keys.toList()
    private val exerciseListWithEmoji = ActivityUtils.activityMetadataMap.values.map { metadata ->
        "${metadata.emoji} ${metadata.label}"
    }

    var selectedExercise: String = activityKeys.firstOrNull() ?: "squat"
        private set

    private var onExerciseSelected: ((String) -> Unit)? = null

    fun setup(
        initialExercise: String? = null,
        onSelected: (String) -> Unit
    ) {
        this.onExerciseSelected = onSelected

        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            exerciseListWithEmoji
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                v: View?,
                pos: Int,
                id: Long
            ) {
                if (!spinner.isEnabled) return

                val key = activityKeys.getOrNull(pos) ?: "squat"
                selectedExercise = key.lowercase()
                onExerciseSelected?.invoke(selectedExercise)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedExercise = activityKeys.firstOrNull() ?: "squat"
            }
        }

        if (initialExercise != null) {
            setExercise(initialExercise)
        }
    }

    fun setExercise(exerciseKey: String) {
        val index = activityKeys.indexOf(exerciseKey)
        if (index >= 0) {
            spinner.setSelection(index)
            selectedExercise = exerciseKey.lowercase()
        }
    }

    fun setEnabled(enabled: Boolean) {
        spinner.isEnabled = enabled
        spinner.alpha = if (enabled) 1.0f else 0.5f
    }

    fun getCurrentExercise(): String = selectedExercise
}