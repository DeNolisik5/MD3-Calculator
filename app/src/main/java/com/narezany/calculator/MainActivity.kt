package com.narezany.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.narezany.calculator.ui.CalculatorRoute
import com.narezany.calculator.ui.theme.CalculatorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the system bars; Compose handles the insets.
        enableEdgeToEdge()

        setContent {
            CalculatorTheme {
                CalculatorRoute()
            }
        }
    }
}
