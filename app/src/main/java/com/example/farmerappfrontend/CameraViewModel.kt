package com.example.farmerappfrontend

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CameraViewModel : ViewModel() {
    private val _scannedIds = MutableStateFlow<Set<String>>(emptySet())
    val scannedIds = _scannedIds.asStateFlow()

    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId = _sessionId.asStateFlow()

    private val _sessionName = MutableStateFlow<String?>(null)
    val sessionName = _sessionName.asStateFlow()

    private val _initialScannedIds = MutableStateFlow<Set<String>>(emptySet())
    val initialScannedIds = _initialScannedIds.asStateFlow()

    fun addScannedId(id: String) {
        if (id.isNotBlank()) {
            _scannedIds.update { it + id.trim() }
        }
    }

    fun startNewSession() {
        _scannedIds.value = emptySet()
        _sessionId.value = null
        _sessionName.value = null
        _initialScannedIds.value = emptySet()
    }

    fun loadSession(session: CountingSession) {
        _scannedIds.value = session.readAnimalIds.toSet()
        _sessionId.value = session.id
        _sessionName.value = session.name
        _initialScannedIds.value = session.readAnimalIds.toSet()
    }

    fun updateInitialState() {
        _initialScannedIds.value = _scannedIds.value
    }

    fun setSessionIdAndName(id: String, name: String) {
        _sessionId.value = id
        _sessionName.value = name
    }
} 