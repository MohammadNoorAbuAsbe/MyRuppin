package com.example.myruppin.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myruppin.data.TokenManager
import com.example.myruppin.data.models.EventInfo
import com.example.myruppin.data.models.UpcomingEvent
import com.example.myruppin.data.repository.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException

class HomeViewModel(
    private val repository: HomeRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    // Current Event State
    private val _currentEvent = MutableStateFlow<EventInfo?>(null)
    val currentEvent: StateFlow<EventInfo?> = _currentEvent.asStateFlow()

    private val _nextEvent = MutableStateFlow<EventInfo?>(null)
    val nextEvent: StateFlow<EventInfo?> = _nextEvent.asStateFlow()

    private val _isLoadingEvent = MutableStateFlow(true)
    val isLoadingEvent: StateFlow<Boolean> = _isLoadingEvent.asStateFlow()

    // Upcoming Events State
    private val _upcomingEvents = MutableStateFlow<List<UpcomingEvent>>(emptyList())
    val upcomingEvents: StateFlow<List<UpcomingEvent>> = _upcomingEvents.asStateFlow()

    private val _isLoadingUpcoming = MutableStateFlow(true)
    val isLoadingUpcoming: StateFlow<Boolean> = _isLoadingUpcoming.asStateFlow()

    // Error State
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Logout completion state
    private val _logoutComplete = MutableStateFlow(false)
    val logoutComplete: StateFlow<Boolean> = _logoutComplete.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    val userName: StateFlow<String?> = _userName.asStateFlow()


    init {
        loadHomeData()
    }

    /**
     * Loads all home screen data
     */
    private fun loadHomeData() {
        viewModelScope.launch {
            tokenManager.token.collectLatest { token ->
                token?.let { currentToken ->
                    loadCurrentEvent(currentToken)
                    loadUpcomingEvents(currentToken)
                    loadUserName(currentToken)
                }
            }
        }
    }

    /**
     * Loads the current event
     */
    private fun loadCurrentEvent(token: String) {
        viewModelScope.launch {
            try {
                _isLoadingEvent.value = true
                val (currentEvents, upcomingEvents) = repository.fetchCurrentEvents(token) ?: Pair(emptyList(), emptyList())
                _currentEvent.value = currentEvents.firstOrNull()
                _nextEvent.value = upcomingEvents.firstOrNull()
                println(_currentEvent.value) // Debugging line
            } catch (e: IOException) {
                _error.value = "Network error: ${e.message}"
            } catch (e: Exception) {
                _error.value = "Error loading current event: ${e.message}"
            } finally {
                _isLoadingEvent.value = false
            }
        }
    }

    private fun loadUserName(token: String) {
        viewModelScope.launch {
            try {
                _userName.value = repository.fetchUserName(token)
            } catch (e: IOException) {
                _error.value = "Network error: ${e.message}"
            } catch (e: Exception) {
                _error.value = "Error loading user name: ${e.message}"
            } finally {

            }
        }
    }

    /**
     * Loads upcoming events
     */
    private fun loadUpcomingEvents(token: String) {
        viewModelScope.launch {
            try {
                _isLoadingUpcoming.value = true
                _upcomingEvents.value = repository.fetchUpcomingEvents(token)
            } catch (e: IOException) {
                _error.value = "Network error: ${e.message}"
            } catch (e: Exception) {
                _error.value = "Error loading upcoming events: ${e.message}"
            } finally {
                _isLoadingUpcoming.value = false
            }
        }
    }

    /**
     * Refreshes all home data
     */
    fun refreshData() {
        loadHomeData()
    }

    /**
     * Logs out the user
     */
    fun logout() {
        viewModelScope.launch {
            tokenManager.clearAll()
            _logoutComplete.value = true // Indicate that logout is complete
        }
    }
}