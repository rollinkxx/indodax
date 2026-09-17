package com.indodax.signal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.indodax.signal.signal.Signal
import com.indodax.signal.viewmodel.SignalViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MainScreen(vm: SignalViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pairs = listOf("btc_idr", "eth_idr", "xrp_idr", "sol_idr", "doge_idr")
    var expanded by remember { mutableStateOf(false) }
    val resultColor = when (state.result?.signal) { Signal.BUY -> Color(0xFF16803C); Signal.SELL -> Color(0xFFC62828); else -> Color(0xFF68707C) }
    Surface(color = Color(0xFFF7F9FC), modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("IndodaxSignal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Sinyal teknikal crypto berbasis OHLC", color = Color(0xFF5F6875))
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(state.pair.uppercase().replace("_", " / ")) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { pairs.forEach { p -> DropdownMenuItem(text = { Text(p.uppercase().replace("_", " / ")) }, onClick = { vm.setPair(p); expanded = false }) } }
            }
            Button(onClick = vm::analyze, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { if (state.loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Analisa") }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.result?.signal?.name ?: "HOLD", color = resultColor, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                    Text("Keyakinan ${state.result?.confidence ?: 0}%", fontWeight = FontWeight.SemiBold)
                    Text("Harga terakhir: ${state.price?.let { NumberFormat.getNumberInstance(Locale.US).format(it) } ?: "-"} IDR")
                    Text(state.result?.reason ?: "Tekan Analisa untuk mengambil data dan menghitung sinyal.", color = Color(0xFF5F6875))
                }
            }
            OutlinedButton(onClick = vm::analyze, modifier = Modifier.fillMaxWidth()) { Text("Refresh") }
            Spacer(Modifier.weight(1f))
            Text("Bukan nasihat finansial. Gunakan sebagai informasi tambahan.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A8491))
        }
    }
}
