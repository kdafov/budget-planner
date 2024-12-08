package com.budgetplanner

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.budgetplanner.ui.theme.BudgetPlannerTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        setContent {
            BudgetPlannerTheme {
                SplashScreen() // Display the splash screen
            }
        }

        // Check if user is signed in
        lifecycleScope.launch {
            delay(2000) // Simulating a splash screen delay
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d("MainActivity", "User is logged in: ${currentUser.email}")
                navigateToHome()
            } else {
                Log.d("MainActivity", "No user logged in")
                navigateToLogin()
            }
        }
    }

    private fun navigateToHome() {
        startActivity(HomePage.createIntent(this))
        finish()
    }

    private fun navigateToLogin() {
        startActivity(LoginPage.createIntent(this))
        finish()
    }
}

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Budget Planner",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            fontSize = 30.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        CircularProgressIndicator()
    }
}