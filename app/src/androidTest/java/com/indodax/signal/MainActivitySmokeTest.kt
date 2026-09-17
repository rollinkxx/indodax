package com.indodax.signal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainScreenShowsAppIdentityAndAnalyzeAction() {
        composeRule.onNodeWithText("IndodaxSignal").assertIsDisplayed()
        composeRule.onNodeWithText("Analisa").assertIsDisplayed()
    }
}
