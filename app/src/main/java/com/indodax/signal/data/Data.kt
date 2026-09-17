package com.indodax.signal.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException

// DTOs mirror the public Indodax payloads; domain validation happens in MarketRepository.
data class TickerResponse(val ticker: Ticker? = null)
data class Ticker(val last: String? = null, val high: String? = null, val low: String? = null, val vol: String? = null)
data class CandleResponse(val s: String? = null, val t: List<Long> = emptyList(), val o: List<Double> = emptyList(), val h: List<Double> = emptyList(), val l: List<Double> = emptyList(), val c: List<Double> = emptyList(), val v: List<Double> = emptyList())
data class Ohlcv(val timestamp: Long, val open: Double, val high: Double, val low: Double, val close: Double, val volume: Double)

enum class DataSource { INDODAX_CANDLES }
sealed interface MarketResult {
    data class Success(val candles: List<Ohlcv>, val ticker: Ticker, val fetchedAt: Long, val source: DataSource) : MarketResult
    data class Failure(val kind: FailureKind, val message: String) : MarketResult
}
enum class FailureKind { NETWORK, TIMEOUT, HTTP, MALFORMED, INSUFFICIENT_DATA }

interface IndodaxApi {
    @GET("api/ticker/{pair}") suspend fun ticker(@Path("pair") pair: String): TickerResponse
    @GET("tradingview/history") suspend fun candles(@Query("symbol") symbol: String, @Query("resolution") resolution: String = "60", @Query("from") from: Long, @Query("to") to: Long): CandleResponse
}

class MarketRepository(private val api: IndodaxApi, private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 }) {
    suspend fun fetch(pair: String): MarketResult {
        val normalized = pair.lowercase()
        if (!ALLOWED_PAIRS.contains(normalized)) return MarketResult.Failure(FailureKind.MALFORMED, "Pasangan tidak didukung")
        return try {
            val ticker = withTimeout(15_000) { api.ticker(normalized).ticker }
                ?: return MarketResult.Failure(FailureKind.MALFORMED, "Ticker kosong")
            val now = nowSeconds()
            val response = withTimeout(15_000) { api.candles(SYMBOLS.getValue(normalized), "60", now - 60L * 60 * 180, now) }
            val candles = response.toDomain(now)
            if (candles.size < MIN_CANDLES) MarketResult.Failure(FailureKind.INSUFFICIENT_DATA, "Histori candle belum mencukupi")
            else MarketResult.Success(candles, ticker, now, DataSource.INDODAX_CANDLES)
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            MarketResult.Failure(FailureKind.TIMEOUT, "Permintaan terlalu lama")
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            MarketResult.Failure(FailureKind.NETWORK, "Tidak dapat terhubung ke Indodax")
        } catch (e: HttpException) {
            MarketResult.Failure(FailureKind.HTTP, "Server Indodax mengembalikan error ${e.code()}")
        } catch (_: IllegalArgumentException) {
            MarketResult.Failure(FailureKind.MALFORMED, "Data Indodax tidak valid")
        }
    }

    private fun CandleResponse.toDomain(now: Long): List<Ohlcv> {
        require(s.equals("ok", ignoreCase = true) || s.equals("no_data", ignoreCase = true)) { "Status candle tidak dikenal" }
        if (s.equals("no_data", ignoreCase = true)) return emptyList()
        val size = t.size
        require(size >= MIN_CANDLES && listOf(o.size, h.size, l.size, c.size, v.size).all { it == size }) { "Array candle tidak sejajar" }
        require(t.zipWithNext().all { it.first < it.second }) { "Timestamp candle tidak terurut" }
        require(now - t.last() <= MAX_STALE_SECONDS) { "Candle sudah terlalu lama" }
        return t.indices.map { i ->
            val open = o[i]; val high = h[i]; val low = l[i]; val close = c[i]; val volume = v[i]
            require(listOf(open, high, low, close, volume).all { it.isFinite() }) { "Nilai candle bukan angka finite" }
            require(open > 0 && high > 0 && low > 0 && close > 0 && volume >= 0) { "Rentang candle tidak valid" }
            require(high >= maxOf(open, close) && low <= minOf(open, close) && high >= low) { "OHLC tidak konsisten" }
            Ohlcv(t[i], open, high, low, close, volume)
        }
    }

    companion object {
        const val MIN_CANDLES = 60
        const val MAX_STALE_SECONDS = 60L * 60 * 4
        val ALLOWED_PAIRS = setOf("btc_idr", "eth_idr", "xrp_idr", "sol_idr", "doge_idr")
        val SYMBOLS = mapOf("btc_idr" to "BTCIDR", "eth_idr" to "ETHIDR", "xrp_idr" to "XRPIDR", "sol_idr" to "SOLIDR", "doge_idr" to "DOGEIDR")
    }
}
