package com.indodax.signal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indodax.signal.data.IndodaxApi
import com.indodax.signal.data.MarketRepository
import com.indodax.signal.indicator.Indicators
import com.indodax.signal.signal.SignalResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private val api: IndodaxApi = Retrofit.Builder().baseUrl("https://indodax.com/").addConverterFactory(GsonConverterFactory.create()).build().create(IndodaxApi::class.java)
private val repository = MarketRepository(api)
data class SignalUiState(val pair: String = "btc_idr", val result: SignalResult? = null, val price: Double? = null, val loading: Boolean = false, val error: String? = null)

class SignalViewModel : ViewModel() {
    private val _state = MutableStateFlow(SignalUiState())
    val state: StateFlow<SignalUiState> = _state.asStateFlow()

    fun setPair(pair: String) { _state.value = _state.value.copy(pair = pair) }
    fun analyze() {
        val pair = _state.value.pair
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val (candles, ticker) = repository.fetch(pair)
                SignalResultHolder(Indicators.calculate(candles), ticker.last?.toDoubleOrNull())
            }.onSuccess { data -> _state.value = _state.value.copy(result = com.indodax.signal.signal.SignalEngine.evaluate(data.snapshot), price = data.price, loading = false) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "Gagal mengambil data Indodax") }
        }
    }
    private data class SignalResultHolder(val snapshot: com.indodax.signal.indicator.Snapshot, val price: Double?)
}
