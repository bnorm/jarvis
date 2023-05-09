package bnorm

import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import org.jetbrains.skiko.wasm.onWasmReady

fun main() {
    onWasmReady {
        Window {
            var text by remember { mutableStateOf("Hello, World") }
            LaunchedEffect(Unit) {
                text = BattleData.get().toString()
            }
            Text(text)
        }
    }
}
