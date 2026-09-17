package com.indodax.signal

import com.indodax.signal.data.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketRepositoryTest {
    private val valid = CandleResponse("ok", (1L..60L).toList(), List(60) { 100.0 }, List(60) { 101.0 }, List(60) { 99.0 }, List(60) { 100.0 }, List(60) { 10.0 })
    @Test fun noDataDoesNotBecomeSyntheticCandles() = runTest {
        val api = object : IndodaxApi {
            override suspend fun ticker(pair: String) = TickerResponse(Ticker(last = "100"))
            override suspend fun candles(symbol: String, resolution: String, from: Long, to: Long) = valid.copy(s = "no_data", t = emptyList(), o = emptyList(), h = emptyList(), l = emptyList(), c = emptyList(), v = emptyList())
        }
        val result = MarketRepository(api, { 1_000_000L }).fetch("btc_idr")
        assertEquals(FailureKind.INSUFFICIENT_DATA, (result as MarketResult.Failure).kind)
    }

    @Test fun unsupportedPairIsRejected() = runTest {
        val api = object : IndodaxApi {
            override suspend fun ticker(pair: String) = TickerResponse()
            override suspend fun candles(symbol: String, resolution: String, from: Long, to: Long) = valid
        }
        val result = MarketRepository(api).fetch("bad_idr")
        assertEquals(FailureKind.MALFORMED, (result as MarketResult.Failure).kind)
    }
}
