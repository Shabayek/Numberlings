package com.vividoai.numberlings.ui.grownups

import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/**
 * The one purchase. Visible only here behind the parental gate; one non-consumable.
 * If Play isn't available, nothing breaks and the Community Unlock always opens
 * everything.
 */
@Composable
fun UnlockButton(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            val activity = context as? android.app.Activity
            val effect = when {
                activity == null -> UnlockEffect.NOT_ON_THIS_DEVICE
                else -> UnlockStore.purchase(context, activity)
            }
            when (effect) {
                UnlockEffect.STARTED ->
                    snackbarHostState.showSnackbar("Follow the Google Play prompt to finish.")
                UnlockEffect.ALREADY_OWNED ->
                    snackbarHostState.showSnackbar("Already yours. Nothing to pay, ever again.")
                UnlockEffect.NOT_ON_THIS_DEVICE ->
                    snackbarHostState.showSnackbar(
                        "Google Play isn't reachable here. The Community Unlock opens everything, no questions asked."
                    )
            }
        }
    }, modifier = Modifier) {
        Text("Unlock all six meadows — one time, no extras")
    }
}
