package com.dbraueraudio.grooverandomizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dbraueraudio.grooverandomizer.ui.theme.GrooveRandomizerTheme
import com.dbraueraudio.grooverandomizer.midi.MidiManager

class MainActivity : ComponentActivity() {
    private lateinit var midiManager: MidiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        midiManager = MidiManager(this)
        
        setContent {
            GrooveRandomizerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isPlaying: Boolean by midiManager.midiClock.isPlaying.collectAsState(initial = false)
                    val currentBpm: Double by midiManager.midiClock.currentBpm.collectAsState(initial = 120.0)
                    
                    DrumPadScreen(
                        onFootTap = { midiManager.sendNote(36, 127) },
                        onHandTap = { midiManager.sendNote(38, 127) },
                        onFunTap = { midiManager.sendNote(1, 127) },
                        onPlayPause = { 
                            if (isPlaying) midiManager.stopClock() 
                            else midiManager.startClock() 
                        },
                        onBpmChange = { midiManager.setBpm(it) },
                        isPlaying = isPlaying,
                        currentBpm = currentBpm
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        midiManager.cleanup()
    }
}

@Composable
fun DrumPadScreen(
    onFootTap: () -> Unit,
    onHandTap: () -> Unit,
    onFunTap: () -> Unit,
    onPlayPause: () -> Unit,
    onBpmChange: (Double) -> Unit,
    isPlaying: Boolean,
    currentBpm: Double
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Transport controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onPlayPause,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(text = if (isPlaying) "Stop" else "Play")
            }
            
            // BPM Slider
            Slider(
                value = currentBpm.toFloat(),
                onValueChange = { onBpmChange(it.toDouble()) },
                valueRange = 40f..208f,
                modifier = Modifier
                    .width(200.dp)
                    .padding(horizontal = 16.dp)
            )
            Text(text = "${currentBpm.toInt()} BPM")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onFootTap,
            modifier = Modifier
                .size(width = 200.dp, height = 100.dp)
                .padding(8.dp)
        ) {
            Text(text = "FOOT", style = MaterialTheme.typography.headlineMedium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onHandTap,
            modifier = Modifier
                .size(width = 200.dp, height = 100.dp)
                .padding(8.dp)
        ) {
            Text(text = "HAND", style = MaterialTheme.typography.headlineMedium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onFunTap,
            modifier = Modifier
                .size(width = 200.dp, height = 100.dp)
                .padding(8.dp)
        ) {
            Text(text = "FUN!", style = MaterialTheme.typography.headlineMedium)
        }
    }
}