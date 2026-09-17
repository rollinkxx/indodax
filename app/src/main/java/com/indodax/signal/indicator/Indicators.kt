package com.indodax.signal.indicator

import com.indodax.signal.data.Ohlcv
import kotlin.math.max

object Indicators {
    // Exponential moving average gives more weight to recent prices.
    fun ema(values: List<Double>, period: Int): List<Double> {
        if (values.isEmpty()) return emptyList()
        val alpha = 2.0 / (period + 1)
        return values.fold(mutableListOf()) { acc, value ->
            acc += if (acc.isEmpty()) value else alpha * value + (1 - alpha) * acc.last()
            acc
        }
    }

    // Wilder RSI: values above 70 are overbought, below 30 oversold.
    fun rsi(values: List<Double>, period: Int = 14): Double {
        if (values.size <= period) return 50.0
        var gains = 0.0; var losses = 0.0
        for (i in 1..period) { val d = values[i] - values[i - 1]; gains += max(d, 0.0); losses += max(-d, 0.0) }
        var avgGain = gains / period; var avgLoss = losses / period
        for (i in (period + 1) until values.size) { val d = values[i] - values[i - 1]; avgGain = (avgGain * (period - 1) + max(d, 0.0)) / period; avgLoss = (avgLoss * (period - 1) + max(-d, 0.0)) / period }
        return if (avgLoss == 0.0) 100.0 else 100.0 - 100.0 / (1 + avgGain / avgLoss)
    }

    fun macd(values: List<Double>): Triple<Double, Double, Boolean> {
        val fast = ema(values, 12); val slow = ema(values, 26)
        val line = values.indices.map { fast[it] - slow[it] }
        val signal = ema(line, 9)
        val n = line.lastIndex
        return Triple(line.getOrElse(n) { 0.0 }, signal.getOrElse(n) { 0.0 }, n > 0 && line[n] > signal[n] && line[n - 1] <= signal[n - 1])
    }

    fun calculate(candles: List<Ohlcv>): Snapshot {
        val closes = candles.map { it.close }; val volumes = candles.map { it.volume }
        val e20 = ema(closes, 20).lastOrNull() ?: 0.0; val e50 = ema(closes, 50).lastOrNull() ?: 0.0
        val rsi = rsi(closes); val (macd, signal, crossUp) = macd(closes)
        val previous = if (closes.size > 1) closes.dropLast(1) else closes
        val (_, previousSignal, previousCrossUp) = macd(previous)
        val crossDown = macd < signal && (previousCrossUp || previousSignal >= macd)
        val avgVol = volumes.takeLast(20).average().takeIf { !it.isNaN() } ?: 0.0
        return Snapshot(e20, e50, rsi, macd, signal, crossUp, crossDown, volumes.lastOrNull() ?: 0.0, avgVol)
    }
}

data class Snapshot(val ema20: Double, val ema50: Double, val rsi: Double, val macd: Double, val macdSignal: Double, val macdCrossUp: Boolean, val macdCrossDown: Boolean, val volume: Double, val volumeAverage: Double)
