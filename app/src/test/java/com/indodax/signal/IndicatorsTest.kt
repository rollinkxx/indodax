package com.indodax.signal

import com.indodax.signal.data.Ohlcv
import com.indodax.signal.indicator.Indicators
import com.indodax.signal.indicator.Snapshot
import com.indodax.signal.signal.Signal
import com.indodax.signal.signal.SignalEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IndicatorsTest {
    @Test fun flatSeriesRsiIsNeutral() {
        assertEquals(50.0, Indicators.rsi(List(60) { 100.0 }), 0.0001)
    }

    @Test fun risingSeriesRsiIsOverbought() {
        assertTrue(Indicators.rsi((0 until 60).map { 100.0 + it }) > 70.0)
    }

    @Test fun fallingSeriesRsiIsOversold() {
        assertTrue(Indicators.rsi((0 until 60).map { 200.0 - it }) < 30.0)
    }

    @Test fun calculateRequiresMinimumHistory() {
        val candles = List(20) { Ohlcv(it.toLong(), 100.0, 101.0, 99.0, 100.0, 10.0) }
        try { Indicators.calculate(candles); error("expected validation failure") } catch (_: IllegalArgumentException) { }
    }

    @Test fun holdHasZeroConfidence() {
        val snapshot = Snapshot(100.0, 100.0, 50.0, 0.0, 0.0, false, false, 10.0, 10.0)
        val result = SignalEngine.evaluate(snapshot)
        assertEquals(Signal.HOLD, result.signal)
        assertEquals(0, result.confidence)
    }
}
