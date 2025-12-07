package com.fitquest.app.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fitquest.app.MainDispatcherRule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainActivityViewModelTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule
    val main = MainDispatcherRule()
    @Before
    fun setUp() {

    }

    @Test
    fun getProgressState() {
    }

    @Test
    fun isInitialized() {
    }

    @Test
    fun completeInitFlow() {
    }

}