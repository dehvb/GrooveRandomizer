package com.dbraueraudio.grooverandomizer.timing

/**
 * Handles musical time calculations and conversions
 * @param ppq Pulses Per Quarter note (MIDI resolution)
 */
class MusicalTime(private val ppq: Int = 960) {
    // Common note values in terms of quarter notes
    enum class NoteValue(val quarterNoteRatio: Double) {
        WHOLE(4.0),
        HALF(2.0),
        QUARTER(1.0),
        EIGHTH(0.5),
        SIXTEENTH(0.25),
        THIRTY_SECOND(0.125);
        
        fun toTriplet(): Double = quarterNoteRatio * (2.0/3.0)
        fun toDotted(): Double = quarterNoteRatio * 1.5
        fun toQuintuplet(): Double = quarterNoteRatio * (4.0/5.0)
    }
    
    /**
     * Converts BPM to microseconds per quarter note
     */
    fun bpmToMicroseconds(bpm: Double): Long {
        return (60_000_000.0 / bpm).toLong()
    }
    
    /**
     * Converts a note position to MIDI ticks
     * @param position Position in the bar (1-based)
     * @param noteValue Type of note
     * @param isTriplet Whether this is part of a triplet
     * @param isDotted Whether this is a dotted note
     * @param isQuintuplet Whether this is a quintuplet
     */
    fun positionToTicks(
        position: Int,
        noteValue: NoteValue,
        isTriplet: Boolean = false,
        isDotted: Boolean = false,
        isQuintuplet: Boolean = false
    ): Int {
        var ratio = noteValue.quarterNoteRatio
        when {
            isQuintuplet -> ratio = noteValue.toQuintuplet()
            isTriplet -> ratio = noteValue.toTriplet()
            isDotted -> ratio = noteValue.toDotted()
        }
        
        // Position is 1-based, convert to 0-based for calculation
        return ((position - 1) * ppq * ratio).toInt()
    }
    
    /**
     * Converts MIDI ticks to real time in microseconds
     */
    fun ticksToMicroseconds(ticks: Int, bpm: Double): Long {
        val microsecondsPerQuarter = bpmToMicroseconds(bpm)
        return (ticks * microsecondsPerQuarter) / ppq
    }
    
    /**
     * Gets the number of ticks in one bar of 4/4 time
     */
    fun getTicksPerBar(): Int = ppq * 4
    
    /**
     * Calculates if a tick position falls on a specific note value grid
     */
    fun isOnGrid(
        tick: Int,
        noteValue: NoteValue,
        isTriplet: Boolean = false,
        isQuintuplet: Boolean = false
    ): Boolean {
        val gridSize = when {
            isQuintuplet -> (ppq * noteValue.toQuintuplet()).toInt()
            isTriplet -> (ppq * noteValue.toTriplet()).toInt()
            else -> (ppq * noteValue.quarterNoteRatio).toInt()
        }
        return tick % gridSize == 0
    }
} 