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

    // The narrator lives as long as the process: shutting it down with the
    // activity would leave the app silent for good after any recreation.
}
