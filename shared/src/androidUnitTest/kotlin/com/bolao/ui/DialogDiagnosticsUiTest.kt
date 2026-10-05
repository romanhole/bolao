package com.bolao.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.window.DialogProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bolao.presentation.theme.BolaoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * TEMPORÁRIO — diagnóstico do AppNotIdleException nos diálogos de criar/entrar em liga.
 * Isola: diálogo sem campo, campo com/sem label, e usePlatformDefaultWidth.
 * Será removido assim que a causa for identificada.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [UiTestConfig.SDK], qualifiers = UiTestConfig.PHONE_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DialogDiagnosticsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun showDialog(platformWidth: Boolean, body: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent {
            BolaoTheme {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = { TextButton(onClick = {}) { Text("OK") } },
                    title = { Text("Título") },
                    text = body,
                    properties = DialogProperties(usePlatformDefaultWidth = platformWidth),
                )
            }
        }
    }

    @Test
    fun d1_dialogWithTextOnly() {
        showDialog(platformWidth = true) { Text("Só texto", modifier = Modifier.testTag("probe")) }
        composeRule.onNodeWithTag("probe").assertExists()
    }

    @Test
    fun d2_dialogWithLabeledField() {
        showDialog(platformWidth = true) {
            OutlinedTextField(value = "", onValueChange = {}, label = { Text("Seu Apelido nesta Liga") }, modifier = Modifier.testTag("probe"))
        }
        composeRule.onNodeWithTag("probe").assertExists()
    }

    @Test
    fun d3_dialogWithFieldWithoutLabel() {
        showDialog(platformWidth = true) {
            OutlinedTextField(value = "", onValueChange = {}, modifier = Modifier.testTag("probe"))
        }
        composeRule.onNodeWithTag("probe").assertExists()
    }

    @Test
    fun d4_dialogWithLabeledField_noPlatformWidth() {
        showDialog(platformWidth = false) {
            OutlinedTextField(value = "", onValueChange = {}, label = { Text("Seu Apelido nesta Liga") }, modifier = Modifier.testTag("probe"))
        }
        composeRule.onNodeWithTag("probe").assertExists()
    }
}
