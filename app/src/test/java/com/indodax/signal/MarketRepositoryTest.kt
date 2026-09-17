package com.indodax.signal

import com.indodax.signal.data.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketRepositoryTest {
    private val valid = List(60) { i -> CandleRow(990_000L + i * 60L, 100.0, 101.0, 99.0, 100.0, "10") }

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
}
