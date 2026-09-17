package com.indodax.signal.signal

import com.indodax.signal.indicator.Snapshot

enum class Signal { BUY, SELL, HOLD }
data class SignalResult(val signal: Signal, val confidence: Int, val reason: String)

object SignalEngine {
    // BUY/SELL require all four specified confirmations; HOLD confidence is always 0.
    fun evaluate(s: Snapshot): SignalResult {
        require(listOf(s.ema20, s.ema50, s.rsi, s.macd, s.macdSignal, s.volume, s.volumeAverage).all { it.isFinite() })
        val bullish = s.ema20 > s.ema50
        val bearish = s.ema20 < s.ema50
        val volume = s.volume > s.volumeAverage && s.volumeAverage > 0
        val buyChecks = listOf(bullish, s.rsi < 70, s.macdCrossUp, volume)
        val sellChecks = listOf(bearish, s.rsi > 30, s.macdCrossDown, volume)
        val signal = when { buyChecks.all { it } -> Signal.BUY; sellChecks.all { it } -> Signal.SELL; else -> Signal.HOLD }
        val confidence = if (signal == Signal.HOLD) 0 else 100
        val trend = if (bullish) "EMA bullish" else if (bearish) "EMA bearish" else "EMA flat"
        val rsi = if (s.rsi > 70) "RSI overbought" else if (s.rsi < 30) "RSI oversold" else "RSI neutral"
        val macd = when { s.macdCrossUp -> "MACD cross up"; s.macdCrossDown -> "MACD cross down"; else -> "MACD no cross" }
        val vol = if (volume) "volume above average" else "volume below average"
        return SignalResult(signal, confidence, "$trend, $rsi, $macd, $vol")
    }
}
