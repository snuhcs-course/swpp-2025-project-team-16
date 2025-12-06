package com.fitquest.app.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fitquest.app.MainDispatcherRule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PoseViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule
    val main = MainDispatcherRule()
    @Before
    fun setUp() {

    }

    @Test
    fun getPoseAnalysisResult() {
    }

    @Test
    fun uploadPose() {
    }

    @Test
    fun resetPoseAnalysisResult() {
    }

}