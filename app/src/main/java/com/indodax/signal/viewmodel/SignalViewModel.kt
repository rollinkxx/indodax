package com.indodax.signal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indodax.signal.data.IndodaxApi
import com.indodax.signal.data.MarketRepository
import com.indodax.signal.data.MarketResult
import com.indodax.signal.indicator.Indicators
import com.indodax.signal.signal.SignalResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

data class SignalUiState(
    val pair: String = "btc_idr",
    val result: SignalResult? = null,
    val price: Double? = null,
    val resultPair: String? = null,
    val fetchedAt: Long? = null,
    val dataSource: String? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class SignalViewModel(private val repository: MarketRepository = defaultRepository()) : ViewModel() {
    private val _state = MutableStateFlow(SignalUiState())
    val state: StateFlow<SignalUiState> = _state.asStateFlow()
    private var activeJob: Job? = null
    private var generation = 0L

    fun setPair(pair: String) {
        activeJob?.cancel(); generation++
        _state.update { it.copy(pair = pair, result = null, price = null, resultPair = null, fetchedAt = null, dataSource = null, error = null, loading = false) }
    }

    fun analyze() {
        activeJob?.cancel()
        val requestPair = _state.value.pair
        val requestGeneration = ++generation
        _state.update { it.copy(loading = true, error = null, result = null, price = null, resultPair = null, fetchedAt = null) }
        activeJob = viewModelScope.launch {
            try {
                when (val response = repository.fetch(requestPair)) {
                    is MarketResult.Success -> {
                        val signal = SignalEngineHolder.evaluate(response.candles)
                        if (requestGeneration == generation && _state.value.pair == requestPair) _state.update { it.copy(result = signal, price = response.ticker.last?.toDoubleOrNull(), resultPair = requestPair, fetchedAt = response.fetchedAt, dataSource = response.source.name, loading = false, error = null) }
                    }
                    is MarketResult.Failure -> if (requestGeneration == generation) _state.update { it.copy(loading = false, error = response.message, result = null, price = null, resultPair = null) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                if (requestGeneration == generation) _state.update { it.copy(loading = false, error = "Gagal memproses data Indodax", result = null, price = null, resultPair = null) }
            }
        }
    }

    private object SignalEngineHolder { fun evaluate(candles: List<com.indodax.signal.data.Ohlcv>): SignalResult = com.indodax.signal.signal.SignalEngine.evaluate(Indicators.calculate(candles)) }
    override fun onCleared() { activeJob?.cancel(); super.onCleared() }

    companion object {
        private fun defaultRepository(): MarketRepository {
            val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()
            val api = Retrofit.Builder().baseUrl("https://indodax.com/").client(client).addConverterFactory(GsonConverterFactory.create()).build().create(IndodaxApi::class.java)
            return MarketRepository(api)
        }
    }
}
