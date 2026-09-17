package com.indodax.signal.indicator

import com.indodax.signal.data.Ohlcv
import kotlin.math.max

object Indicators {
    fun ema(values: List<Double>, period: Int): List<Double> {
        require(period > 0) { "EMA period harus positif" }
        require(values.all { it.isFinite() }) { "Nilai EMA harus finite" }
        if (values.isEmpty()) return emptyList()
        val alpha = 2.0 / (period + 1)
        return values.fold(mutableListOf()) { acc, value ->
            acc += if (acc.isEmpty()) value else alpha * value + (1 - alpha) * acc.last()
            acc
        }
    }

    fun rsi(values: List<Double>, period: Int = 14): Double {
        require(period > 0) { "RSI period harus positif" }
        require(values.all { it.isFinite() }) { "Nilai RSI harus finite" }
        if (values.size <= period) return 50.0
        var gains = 0.0; var losses = 0.0
        for (i in 1..period) { val d = values[i] - values[i - 1]; gains += max(d, 0.0); losses += max(-d, 0.0) }
        var avgGain = gains / period; var avgLoss = losses / period
        for (i in (period + 1) until values.size) { val d = values[i] - values[i - 1]; avgGain = (avgGain * (period - 1) + max(d, 0.0)) / period; avgLoss = (avgLoss * (period - 1) + max(-d, 0.0)) / period }
        return when {
            avgGain == 0.0 && avgLoss == 0.0 -> 50.0
            avgLoss == 0.0 -> 100.0
            avgGain == 0.0 -> 0.0
            else -> 100.0 - 100.0 / (1 + avgGain / avgLoss)
        }
    }

    data class MacdSnapshot(val line: Double, val signal: Double, val previousLine: Double?, val previousSignal: Double?, val crossUp: Boolean, val crossDown: Boolean)
    fun macd(values: List<Double>): MacdSnapshot {
        require(values.all { it.isFinite() }) { "Nilai MACD harus finite" }
        require(values.size >= MACD_MIN_CANDLES) { "Histori MACD belum mencukupi" }
        val line = ema(values, 12).indices.map { i -> ema(values, 12)[i] - ema(values, 26)[i] }
        val signal = ema(line, 9)
        val n = line.lastIndex
        val prev = n - 1
        val currentLine = line[n]; val currentSignal = signal[n]
        val previousLine = line[prev]; val previousSignal = signal[prev]
        return MacdSnapshot(currentLine, currentSignal, previousLine, previousSignal, currentLine > currentSignal && previousLine <= previousSignal, currentLine < currentSignal && previousLine >= previousSignal)
    }

    fun calculate(candles: List<Ohlcv>): Snapshot {
        require(candles.size >= MIN_CANDLES) { "Histori candle belum mencukupi" }
        require(candles.all { it.close.isFinite() && it.volume.isFinite() && it.close > 0 && it.volume >= 0 }) { "Input candle tidak valid" }
        val closes = candles.map { it.close }; val volumes = candles.map { it.volume }
        val e20 = ema(closes, 20).last(); val e50 = ema(closes, 50).last(); val rsi = rsi(closes)
        val macd = macd(closes); val window = volumes.takeLast(20); require(window.size == 20)
        return Snapshot(e20, e50, rsi, macd.line, macd.signal, macd.crossUp, macd.crossDown, volumes.last(), window.average())
    }

    const val MACD_MIN_CANDLES = 35
    const val MIN_CANDLES = 60
}

data class Snapshot(val ema20: Double, val ema50: Double, val rsi: Double, val macd: Double, val macdSignal: Double, val macdCrossUp: Boolean, val macdCrossDown: Boolean, val volume: Double, val volumeAverage: Double)
