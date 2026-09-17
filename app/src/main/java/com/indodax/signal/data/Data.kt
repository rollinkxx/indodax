package com.indodax.signal.data

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.Locale

// DTOs follow the documented public Indodax contracts.
data class TickerResponse(val ticker: Ticker? = null)
data class Ticker(val last: String? = null, val high: String? = null, val low: String? = null, val vol: String? = null)
data class CandleRow(
    @SerializedName("Time") val timestamp: Long,
    @SerializedName("Open") val open: Double,
    @SerializedName("High") val high: Double,
    @SerializedName("Low") val low: Double,
    @SerializedName("Close") val close: Double,
    @SerializedName("Volume") val volume: String
)
data class Ohlcv(val timestamp: Long, val open: Double, val high: Double, val low: Double, val close: Double, val volume: Double)

enum class DataSource { INDODAX_HISTORY_V2 }
sealed interface MarketResult {
    data class Success(val candles: List<Ohlcv>, val ticker: Ticker, val fetchedAt: Long, val source: DataSource) : MarketResult
    data class Failure(val kind: FailureKind, val message: String) : MarketResult
}
enum class FailureKind { NETWORK, TIMEOUT, HTTP, MALFORMED, INSUFFICIENT_DATA }

interface IndodaxApi {
    @GET("api/ticker/{pair}") suspend fun ticker(@Path("pair") pairId: String): TickerResponse?
    @GET("tradingview/history_v2") suspend fun candles(@Query("symbol") symbol: String, @Query("tf") timeframe: String = "60", @Query("from") from: Long, @Query("to") to: Long): List<CandleRow>?
}

class MarketRepository(private val api: IndodaxApi, private val nowSeconds: () -> Long = { System.currentTimeMillis() / 1000 }) {
    suspend fun fetch(pair: String): MarketResult {
        val normalized = pair.trim().lowercase(Locale.ROOT)
        if (!TICKER_IDS.containsKey(normalized)) return MarketResult.Failure(FailureKind.MALFORMED, "Pasangan tidak didukung")
        return try {
            withTimeout(20_000) {
                val now = nowSeconds()
                coroutineScope {
                    val tickerDeferred = async { withTimeout(15_000) { api.ticker(TICKER_IDS.getValue(normalized)) } }
                    val candlesDeferred = async {
                        withTimeout(15_000) {
                            api.candles(SYMBOLS.getValue(normalized), "60", now - 60L * 60 * 180, now)
                        }
                    }
                    val tickerResponse = tickerDeferred.await()
                        ?: return@coroutineScope MarketResult.Failure(FailureKind.MALFORMED, "Respons ticker kosong")
                    val ticker = tickerResponse.ticker
                        ?: return@coroutineScope MarketResult.Failure(FailureKind.MALFORMED, "Ticker kosong")
                    validateTicker(ticker)
                    val rows = candlesDeferred.await()
                        ?: return@coroutineScope MarketResult.Failure(FailureKind.MALFORMED, "Respons candle kosong")
                    if (rows.size > MAX_CANDLES) {
                        return@coroutineScope MarketResult.Failure(FailureKind.MALFORMED, "Histori candle terlalu besar")
                    }
                    val candles = rows.toDomain(now)
                    if (candles.size < MIN_CANDLES) MarketResult.Failure(FailureKind.INSUFFICIENT_DATA, "Histori candle belum mencukupi")
                    else MarketResult.Success(candles, ticker, now, DataSource.INDODAX_HISTORY_V2)
                }
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            MarketResult.Failure(FailureKind.TIMEOUT, "Permintaan terlalu lama")
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            MarketResult.Failure(FailureKind.NETWORK, "Tidak dapat terhubung ke Indodax")
        } catch (e: HttpException) {
            MarketResult.Failure(FailureKind.HTTP, "Server Indodax mengembalikan error ${e.code()}")
        } catch (_: com.google.gson.JsonParseException) {
            MarketResult.Failure(FailureKind.MALFORMED, "Format data Indodax tidak valid")
        } catch (_: IllegalArgumentException) {
            MarketResult.Failure(FailureKind.MALFORMED, "Data Indodax tidak valid")
        }
    }

    private fun validateTicker(ticker: Ticker) {
        val last = ticker.last?.toDoubleOrNull()
        require(last != null && last.isFinite() && last > 0) { "Harga ticker tidak valid" }
    }

    private fun List<CandleRow>.toDomain(now: Long): List<Ohlcv> {
        if (isEmpty()) return emptyList()
        require(zipWithNext().all { it.first.timestamp < it.second.timestamp }) { "Timestamp candle tidak terurut" }
        require(zipWithNext().all { it.second.timestamp - it.first.timestamp == CANDLE_INTERVAL_SECONDS }) { "Interval candle bukan hourly" }
        require(all { it.timestamp > 0 && it.timestamp <= now }) { "Timestamp candle tidak valid" }
        val completed = filter { it.timestamp + CANDLE_INTERVAL_SECONDS <= now }
        if (completed.isEmpty()) return emptyList()
        require(now - completed.last().timestamp <= MAX_STALE_SECONDS) { "Candle terbaru terlalu lama" }
        return completed.map { row ->
            val volume = row.volume.toDoubleOrNull()
            require(volume != null && volume.isFinite()) { "Volume candle tidak valid" }
            require(listOf(row.open, row.high, row.low, row.close).all { it.isFinite() && it > 0 } && volume >= 0) { "Rentang candle tidak valid" }
            require(row.high >= maxOf(row.open, row.close) && row.low <= minOf(row.open, row.close) && row.high >= row.low) { "OHLC tidak konsisten" }
            Ohlcv(row.timestamp, row.open, row.high, row.low, row.close, volume)
        }
    }

    companion object {
        const val MIN_CANDLES = 60
        const val MAX_CANDLES = 500
        const val CANDLE_INTERVAL_SECONDS = 60L * 60
        const val MAX_STALE_SECONDS = 60L * 60 * 4
        val TICKER_IDS = mapOf("btc_idr" to "btcidr", "eth_idr" to "ethidr", "xrp_idr" to "xrpidr", "sol_idr" to "solidr", "doge_idr" to "dogeidr")
        val SYMBOLS = mapOf("btc_idr" to "BTCIDR", "eth_idr" to "ETHIDR", "xrp_idr" to "XRPIDR", "sol_idr" to "SOLIDR", "doge_idr" to "DOGEIDR")
    }
}
