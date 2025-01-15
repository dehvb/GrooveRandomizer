package com.dbraueraudio.grooverandomizer.midi

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import dev.atsushieno.ktmidi.*
import com.dbraueraudio.grooverandomizer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import com.dbraueraudio.grooverandomizer.timing.MusicalTime
import com.dbraueraudio.grooverandomizer.timing.MidiClock
import kotlinx.coroutines.launch

class MidiManager(private val context: Context) {
    private val midiAccess: MidiAccess = AndroidMidiAccess(context)
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var midiHandler: KtMidiHandler
    private var kickPlayer: MediaPlayer? = null
    private var snarePlayer: MediaPlayer? = null
    private var airhornPlayer: MediaPlayer? = null
    
    // Add timing components
    private val musicalTime = MusicalTime(ppq = 960)
    val midiClock: MidiClock
    
    init {
        midiClock = MidiClock(musicalTime, managerScope)
        
        midiHandler = KtMidiHandler(
            midiAccess = midiAccess,
            coroutineScope = managerScope,
            onMessageReceived = ::handleMidiMessage
        )
        
        setupComponents()
    }
    
    private fun setupComponents() {
        managerScope.launch {
            val midiInitialized = midiHandler.initialize()
            if (!midiInitialized) {
                Log.e("MidiManager", "Failed to initialize MIDI system")
                return@launch
            }
            setupAudioPlayers()
            setupClockCallbacks()
        }
    }
    
    private fun setupClockCallbacks() {
        midiClock.onClockTick = { tick ->
            // Send MIDI clock message (0xF8)
            val clockMessage = byteArrayOf(0xF8.toByte())
            midiHandler.sendMessage(clockMessage)
        }
        
        midiClock.onQuarterNote = { quarterNote ->
            // Could add metronome sound here
        }
        
        midiClock.onBarStart = {
            // Could add bar start indication here
        }
    }
    
    fun startClock() = midiClock.start()
    fun stopClock() = midiClock.stop()
    fun setBpm(bpm: Double) = midiClock.setBpm(bpm)
    
    private fun setupAudioPlayers() {
        try {
            kickPlayer = MediaPlayer.create(context, R.raw.kick)
            snarePlayer = MediaPlayer.create(context, R.raw.snare)
            airhornPlayer = MediaPlayer.create(context, R.raw.airhorn)
            
            if (kickPlayer == null || snarePlayer == null || airhornPlayer == null) {
                Log.e("MidiManager", "Failed to create one or more MediaPlayers")
            }
        } catch (e: Exception) {
            Log.e("MidiManager", "Error setting up audio players", e)
        }
    }
    
    private fun handleMidiMessage(data: ByteArray) {
        if (data.size < 3) {
            Log.d("MidiManager", "Message too short: ${data.size} bytes")
            return
        }
        
        val status = data[0].toInt() and 0xF0 // Remove channel info
        val note = data[1].toInt()
        val velocity = data[2].toInt()
        
        Log.d("MidiManager", "MIDI Message - Status: $status, Note: $note, Velocity: $velocity")
        
        // Only handle Note On messages with velocity > 0
        if (status == 0x90 && velocity > 0) {
            when (note) {
                36 -> {
                    Log.d("MidiManager", "Playing Kick")
                    playKick()
                }
                38 -> {
                    Log.d("MidiManager", "Playing Snare")
                    playSnare()
                }
                1 -> {
                    Log.d("MidiManager", "Playing Airhorn")
                    playAirhorn()
                }
            }
        }
    }
    
    fun sendNote(pitch: Int, velocity: Int) {
        val noteOnStatus = 0x90.toByte()
        val message = byteArrayOf(noteOnStatus, pitch.toByte(), velocity.toByte())
        midiHandler.sendMessage(message)
    }
    
    private fun playKick() {
        kickPlayer?.apply {
            Log.d("MidiManager", "Kick player exists, playing...")
            seekTo(0)
            start()
        } ?: Log.e("MidiManager", "Kick player is null!")
    }
    
    private fun playSnare() {
        snarePlayer?.apply {
            seekTo(0)
            start()
        }
    }
    
    private fun playAirhorn() {
        airhornPlayer?.apply {
            seekTo(0)
            start()
        }
    }
    
    fun cleanup() {
        kickPlayer?.release()
        snarePlayer?.release()
        airhornPlayer?.release()
        midiHandler.cleanup()
        midiClock.cleanup()
    }
} 