package com.dbraueraudio.grooverandomizer.timing

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToLong
import android.util.Log

class MidiClock(
    private val musicalTime: MusicalTime,
    private val coroutineScope: CoroutineScope
) {
    private var clockJob: Job? = null
    
    // States
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    
    private val _currentBpm = MutableStateFlow(120.0)
    val currentBpm: StateFlow<Double> = _currentBpm
    
    private val _currentTick = MutableStateFlow(0)
    val currentTick: StateFlow<Int> = _currentTick
    
    // Callbacks
    var onClockTick: ((Int) -> Unit)? = null
    var onQuarterNote: ((Int) -> Unit)? = null
    var onBarStart: (() -> Unit)? = null
    
    fun start() {
        if (_isPlaying.value) return
        
        clockJob = coroutineScope.launch(Dispatchers.Default) {
            _isPlaying.value = true
            var tickCount = 0
            
            while (isActive) {
                val microsecondsPerPulse = musicalTime.bpmToMicroseconds(_currentBpm.value) / 24
                
                // Send MIDI clock pulse (0xF8)
                onClockTick?.invoke(tickCount)
                
                // Check for quarter note boundaries
                if (tickCount % 24 == 0) {
                    onQuarterNote?.invoke(tickCount / 24)
                }
                
                // Check for bar boundaries (assuming 4/4 time)
                if (tickCount % 96 == 0) {
                    onBarStart?.invoke()
                }
                
                _currentTick.value = tickCount
                tickCount++
                
                // Wait for next pulse
                delay(microsecondsPerPulse / 1000) // Convert to milliseconds
            }
        }
    }
    
    fun stop() {
        clockJob?.cancel()
        clockJob = null
        _isPlaying.value = false
        _currentTick.value = 0
    }
    
    fun setBpm(bpm: Double) {
        if (bpm in 20.0..300.0) {  // Common BPM range
            _currentBpm.value = bpm
        } else {
            Log.w("MidiClock", "BPM value out of range: $bpm")
        }
    }
    
    fun cleanup() {
        stop()
    }
} 