package com.indodax.signal

import com.indodax.signal.data.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketRepositoryTest {
    private val valid = List(60) { i -> CandleRow(990_000L + i * 3_600L, 100.0, 101.0, 99.0, 100.0, "10") }

    private val validHourlyHistory = List(180) { i ->
        CandleRow(994_600L - (179 - i) * 3_600L, 100.0, 101.0, 99.0, 100.0, "10")
    }

    @Test fun noDataDoesNotBecomeSyntheticCandles() = runTest {
        val api = object : IndodaxApi {
            override suspend fun ticker(pairId: String) = TickerResponse(Ticker(last = "100"))
            override suspend fun candles(symbol: String, timeframe: String, from: Long, to: Long): List<CandleRow> = emptyList()
        }
        val result = MarketRepository(api, { 1_000_000L }).fetch("btc_idr")
        assertEquals(FailureKind.INSUFFICIENT_DATA, (result as MarketResult.Failure).kind)
    }

    @Test fun tickerUsesCompactPublicPairId() = runTest {
        var requested = ""
        val api = object : IndodaxApi {
            override suspend fun ticker(pairId: String): TickerResponse { requested = pairId; return TickerResponse(Ticker(last = "100")) }
            override suspend fun candles(symbol: String, timeframe: String, from: Long, to: Long) = valid
        }
        MarketRepository(api, { 1_000_000L }).fetch("btc_idr")
        assertEquals("btcidr", requested)
    }

    @Test fun futureCandleIsRejected() = runTest {
        val api = object : IndodaxApi {
            override suspend fun ticker(pairId: String) = TickerResponse(Ticker(last = "100"))
            override suspend fun candles(symbol: String, timeframe: String, from: Long, to: Long) = valid.dropLast(1) + CandleRow(1_000_001L, 100.0, 101.0, 99.0, 100.0, "10")
        }
        val result = MarketRepository(api, { 1_000_000L }).fetch("btc_idr")
        assertEquals(FailureKind.MALFORMED, (result as MarketResult.Failure).kind)
    }

    @Test fun unsupportedPairIsRejected() = runTest {
        val api = object : IndodaxApi {
            override suspend fun ticker(pairId: String) = TickerResponse()
            override suspend fun candles(symbol: String, timeframe: String, from: Long, to: Long) = valid
        }
        val result = MarketRepository(api).fetch("bad_idr")
        assertEquals(FailureKind.MALFORMED, (result as MarketResult.Failure).kind)
    }

    @Test fun historicalCandlesMayBeOlderThanFreshnessWindowWhenLatestIsFresh() = runTest {
        val api = object : IndodaxApi {
            override suspend fun ticker(pairId: String) = TickerResponse(Ticker(last = "100"))
            override suspend fun candles(symbol: String, timeframe: String, from: Long, to: Long) = validHourlyHistory
        }

        val result = MarketRepository(api, { 1_000_000L }).fetch("btc_idr")

        assertEquals(180, (result as MarketResult.Success).candles.size)
    }

    @Test fun staleLatestCandleIsRejectedEvenWhenHistoryIsComplete() = runTest {
        val staleHistory = validHourlyHistory.map { it.copy(timestamp = it.timestamp - 5 * 3_600L) }
        val api = object : IndodaxApi {
            override suspend fun ticker(pairId: String) = TickerResponse(Ticker(last = "100"))
            override suspend fun candles(symbol: String, timeframe: String, from: Long, to: Long) = staleHistory
        }

        val result = MarketRepository(api, { 1_000_000L }).fetch("btc_idr")

        assertEquals(FailureKind.MALFORMED, (result as MarketResult.Failure).kind)
    }

    @Test fun nonHourlyHistoryIsRejected() = runTest {
        val api = object : IndodaxApi {
            override suspend fun ticker(pairId: String) = TickerResponse(Ticker(last = "100"))
            override suspend fun candles(symbol: String, timeframe: String, from: Long, to: Long) =
                List(60) { i -> CandleRow(900_000L + i * 60L, 100.0, 101.0, 99.0, 100.0, "10") }
        }

        val result = MarketRepository(api, { 1_000_000L }).fetch("btc_idr")

        assertEquals(FailureKind.MALFORMED, (result as MarketResult.Failure).kind)
    }

    @Test fun historyWithInternalGapIsRejected() = runTest {
        val rows = List(60) { i ->
            val gap = if (i >= 30) 3_600L else 0L
            CandleRow(700_000L + i * 3_600L + gap, 100.0, 101.0, 99.0, 100.0, "10")
        }
        val api = object : IndodaxApi {
            override suspend fun ticker(pairId: String) = TickerResponse(Ticker(last = "100"))
            override suspend fun candles(symbol: String, timeframe: String, from: Long, to: Long) = rows
        }

        val result = MarketRepository(api, { 1_000_000L }).fetch("btc_idr")

        assertEquals(FailureKind.MALFORMED, (result as MarketResult.Failure).kind)
    }

    @Test fun openLatestCandleIsExcludedFromCompletedHistory() = runTest {
        val rows = validHourlyHistory + CandleRow(1_000_000L - 1_800L, 100.0, 101.0, 99.0, 100.0, "10")
        val api = object : IndodaxApi {
            override suspend fun ticker(pairId: String) = TickerResponse(Ticker(last = "100"))
            override suspend fun candles(symbol: String, timeframe: String, from: Long, to: Long) = rows
        }

        val result = MarketRepository(api, { 1_000_000L }).fetch("btc_idr")

        assertEquals(180, (result as MarketResult.Success).candles.size)
    }
}
