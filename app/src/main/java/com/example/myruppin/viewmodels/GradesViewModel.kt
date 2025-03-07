package com.example.myruppin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myruppin.data.TokenManager
import com.example.myruppin.data.models.Course
import com.example.myruppin.data.models.GradesData
import com.example.myruppin.data.repository.GradesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException

class GradesViewModel(
    private val repository: GradesRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    // UI State
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Grades Data
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _cumulativeAverage = MutableStateFlow<String>("N/A")
    val cumulativeAverage: StateFlow<String> = _cumulativeAverage.asStateFlow()

    private val _annualAverages = MutableStateFlow<List<String>>(emptyList())
    val annualAverages: StateFlow<List<String>> = _annualAverages.asStateFlow()

    // Filter State
    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _uniqueKrsSnl = MutableStateFlow<List<String>>(emptyList())
    val uniqueKrsSnl: StateFlow<List<String>> = _uniqueKrsSnl.asStateFlow()

    // Error State
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadGradesData()
    }

    /**
     * Loads grades data using the token from TokenManager
     */
    private fun loadGradesData() {
        viewModelScope.launch {
            tokenManager.token.collectLatest { token ->
                token?.let { currentToken ->
                    try {
                        _isLoading.value = true
                        _error.value = null

                        val gradesData = repository.fetchGradesData(currentToken)
                        updateGradesData(gradesData)
                    } catch (e: IOException) {
                        _error.value = "Network error: ${e.message}"
                    } catch (e: Exception) {
                        _error.value = "Error loading grades: ${e.message}"
                    } finally {
                        _isLoading.value = false
                    }
                } ?: run {
                    _error.value = "Authentication token not found"
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Updates the UI state with the fetched grades data
     */
    private fun updateGradesData(gradesData: GradesData) {
        _courses.value = gradesData.courses
        _cumulativeAverage.value = gradesData.averages.cumulativeAverage
        _annualAverages.value = gradesData.averages.annualAverages

        // Extract unique krs_snl values for filtering
        _uniqueKrsSnl.value = gradesData.courses.map { it.krs_snl }.distinct().sorted()
    }

    /**
     * Sets the selected filter
     */
    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    /**
     * Returns the filtered courses based on the selected filter
     */
    fun getFilteredCourses(): List<Course> {
        return if (_selectedFilter.value == "All") {
            _courses.value
        } else {
            _courses.value.filter { it.krs_snl == _selectedFilter.value }
        }
    }

    /**
     * Gets the appropriate average to display based on the selected filter
     */
    fun getDisplayAverage(): String {
        return if (_selectedFilter.value == "All") {
            "Cumulative Average: ${_cumulativeAverage.value}"
        } else {
            val index = _uniqueKrsSnl.value.indexOf(_selectedFilter.value)
            "Annual Average: ${_annualAverages.value.getOrNull(index) ?: "N/A"}"
        }
    }

    /**
     * Refreshes the grades data
     */
    fun refreshData() {
        loadGradesData()
    }
}