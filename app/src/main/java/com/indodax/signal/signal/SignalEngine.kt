package com.indodax.signal.signal

import com.indodax.signal.indicator.Snapshot
import kotlin.math.roundToInt

enum class Signal { BUY, SELL, HOLD }
data class SignalResult(val signal: Signal, val confidence: Int, val reason: String)

object SignalEngine {
    // Four equally weighted checks: trend, RSI condition, MACD cross, and volume confirmation.
    fun evaluate(s: Snapshot): SignalResult {
        val bullish = s.ema20 > s.ema50
        val bearish = s.ema20 < s.ema50
        val volume = s.volume > s.volumeAverage && s.volumeAverage > 0
        val buyChecks = listOf(bullish, s.rsi < 70, s.macdCrossUp, volume)
        val sellChecks = listOf(bearish, s.rsi > 30, s.macdCrossDown, volume)
        val buyScore = buyChecks.count { it } * 25
        val sellScore = sellChecks.count { it } * 25
        val result = when {
            buyChecks.all { it } -> Signal.BUY to buyScore
            sellChecks.all { it } -> Signal.SELL to sellScore
            else -> Signal.HOLD to maxOf(buyScore, sellScore)
        }
        val trend = if (bullish) "EMA bullish" else if (bearish) "EMA bearish" else "EMA flat"
        val rsi = if (s.rsi > 70) "RSI overbought" else if (s.rsi < 30) "RSI oversold" else "RSI neutral"
        val macd = when { s.macdCrossUp -> "MACD cross up"; s.macdCrossDown -> "MACD cross down"; else -> "MACD no cross" }
        val vol = if (volume) "volume above average" else "volume below average"
        return SignalResult(result.first, result.second.coerceIn(0, 100), "$trend, $rsi, $macd, $vol")
    }
}
