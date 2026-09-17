package com.indodax.signal.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indodax.signal.R
import com.indodax.signal.signal.Signal
import com.indodax.signal.viewmodel.SignalViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MainScreen(vm: SignalViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pairs = listOf("btc_idr", "eth_idr", "xrp_idr", "sol_idr", "doge_idr")
    var expanded by remember { mutableStateOf(false) }
    val dark = isSystemInDarkTheme()
    val resultColor = when (state.result?.signal) { Signal.BUY -> if (dark) Color(0xFF8FE388) else Color(0xFF146C2E); Signal.SELL -> if (dark) Color(0xFFFF9A9A) else Color(0xFFB3261E); else -> if (dark) Color.LightGray else Color(0xFF68707C) }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), enabled = !state.loading) { Text(pairLabel(state.pair)) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { pairs.forEach { p -> DropdownMenuItem(text = { Text(pairLabel(p)) }, onClick = { vm.setPair(p); expanded = false }) } }
            }
            Button(onClick = vm::analyze, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                if (state.loading) {
                    val loadingDescription = stringResource(R.string.loading)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp).semantics { contentDescription = loadingDescription }, strokeWidth = 2.dp)
                        Text(loadingDescription)
                    }
                } else Text(stringResource(R.string.analyze))
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val signalText = when (state.result?.signal) {
                        Signal.BUY -> stringResource(R.string.buy)
                        Signal.SELL -> stringResource(R.string.sell)
                        Signal.HOLD -> stringResource(R.string.hold)
                        null -> stringResource(R.string.not_analyzed)
                    }
                    Text(signalText, color = resultColor, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(R.string.confidence, state.result?.confidence ?: 0), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.last_price, state.price?.let { NumberFormat.getNumberInstance(Locale("id", "ID")).format(it) } ?: "-"))
                    Text(state.result?.reason ?: stringResource(R.string.analyze_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.fetchedAt?.let { Text(stringResource(R.string.data_source, stringResource(R.string.data_source_history)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
            OutlinedButton(onClick = vm::analyze, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.refresh)) }
            Text(stringResource(R.string.disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun pairLabel(pair: String) = pair.uppercase().replace("_", " / ")
@Composable
private fun signalLabel(signal: Signal) = when (signal) { Signal.BUY -> stringResource(R.string.buy); Signal.SELL -> stringResource(R.string.sell); Signal.HOLD -> stringResource(R.string.hold) }
