package com.indodax.signal.data

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Flexible models match Indodax's public JSON payloads while keeping parsing resilient.
data class TickerResponse(val ticker: Ticker? = null)
data class Ticker(val last: String? = null, val high: String? = null, val low: String? = null, val vol: String? = null)
data class SummaryResponse(val tickers: Map<String, SummaryTicker> = emptyMap())
data class SummaryTicker(val last: String? = null, val high: String? = null, val low: String? = null, val vol_idr: String? = null, val vol_btc: String? = null)
data class CandleResponse(val s: String? = null, val t: List<Long> = emptyList(), val o: List<Double> = emptyList(), val h: List<Double> = emptyList(), val l: List<Double> = emptyList(), val c: List<Double> = emptyList(), val v: List<Double> = emptyList())
data class Ohlcv(val close: Double, val volume: Double)

interface IndodaxApi {
    @GET("api/summaries") suspend fun summaries(): SummaryResponse
    @GET("api/ticker/{pair}") suspend fun ticker(@Path("pair") pair: String): TickerResponse
    @GET("tradingview/history") suspend fun candles(@Query("symbol") symbol: String, @Query("resolution") resolution: String = "60", @Query("from") from: Long, @Query("to") to: Long): CandleResponse
}

class MarketRepository(private val api: IndodaxApi) {
    suspend fun fetch(pair: String): Pair<List<Ohlcv>, Ticker> {
        val ticker = api.ticker(pair).ticker ?: Ticker()
        val now = System.currentTimeMillis() / 1000
        val candles = runCatching { api.candles(pair.uppercase().replace("_", ""), "60", now - 60L * 60 * 120, now) }.getOrNull()
        val ohlcv = if (candles != null && candles.c.size >= 2) {
            candles.c.indices.map { i -> Ohlcv(candles.c[i], candles.v.getOrElse(i) { 0.0 }) }
        } else {
            val last = ticker.last?.toDoubleOrNull() ?: 0.0
            val volume = ticker.vol?.toDoubleOrNull() ?: 0.0
            List(60) { Ohlcv(last, volume) }
        }
        return ohlcv to ticker
    }
}
