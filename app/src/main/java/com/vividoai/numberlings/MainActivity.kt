package com.vividoai.numberlings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.vividoai.numberlings.ui.AppRoot
import com.vividoai.numberlings.ui.theme.NumberlingsTheme

/**
 * One Activity for everything: kids tap, villagers answer, nothing phones home.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameHub.wire(this)
        setContent {
            NumberlingsTheme {
                AppRoot()
            }
        }
    }

    override fun onDestroy() {
        GameHub.shutter()
        super.onDestroy()
    }
}
