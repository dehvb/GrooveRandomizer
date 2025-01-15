package com.dbraueraudio.grooverandomizer.midi

import android.util.Log
import dev.atsushieno.ktmidi.*
import kotlinx.coroutines.CoroutineScope

interface MidiHandler {
    fun handleMessage(data: ByteArray)
    fun sendMessage(data: ByteArray)
}

class KtMidiHandler(
    private val midiAccess: MidiAccess,
    private val coroutineScope: CoroutineScope,
    private val onMessageReceived: (ByteArray) -> Unit
) : MidiHandler {
    private var input: MidiInput? = null
    private var output: MidiOutput? = null
    
    suspend fun initialize(): Boolean {
        return try {
            Log.d("MidiHandler", "Available outputs: ${midiAccess.outputs.joinToString(", ") { it.name ?: "unnamed" }}")
            Log.d("MidiHandler", "Available inputs: ${midiAccess.inputs.joinToString(", ") { it.name ?: "unnamed" }}")
            
            // Try to open the first available ports if they exist
            val outputPort = midiAccess.outputs.firstOrNull()
            val inputPort = midiAccess.inputs.firstOrNull()
            
            if (outputPort == null || inputPort == null) {
                Log.e("MidiHandler", "No MIDI ports available")
                return false
            }
            
            output = midiAccess.openOutput(outputPort.id)
            input = midiAccess.openInput(inputPort.id)
            
            // Verify initialization
            if (output == null || input == null) {
                Log.e("MidiHandler", "Failed to open MIDI ports")
                return false
            }
            
            input?.onMidiMessage = { _, data ->
                handleMessage(data)
            }
            
            Log.d("MidiHandler", "MIDI initialization successful")
            true
        } catch (e: Exception) {
            Log.e("MidiHandler", "Failed to initialize MIDI", e)
            false
        }
    }
    
    override fun handleMessage(data: ByteArray) {
        onMessageReceived(data)
    }
    
    override fun sendMessage(data: ByteArray) {
        Log.d("MidiHandler", "Sending MIDI message: ${data.joinToString { it.toString() }}")
        output?.let { out ->
            try {
                out.send(data, 0, data.size, 0L)
                Log.d("MidiHandler", "Message sent successfully")
            } catch (e: Exception) {
                Log.e("MidiHandler", "Failed to send MIDI message", e)
            }
        } ?: Log.e("MidiHandler", "Output is null!")
    }
    
    fun cleanup() {
        input?.close()
        output?.close()
    }
} 