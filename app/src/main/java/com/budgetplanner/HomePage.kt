package com.budgetplanner

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.budgetplanner.ui.theme.BudgetPlannerTheme
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth

// Home screen of the app
class HomePage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BudgetPlannerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    HomeContent(
                        // Ensuring content inside the Scaffold does not overlap with system UI
                        modifier = Modifier.padding(innerPadding),
                        // Define action for when user wants to navigate to Budget screen
                        onNavigateToSetupBudget = {
                            startActivity(SetupBudget.createIntent(this))
                        },
                        // Define action for when user wants to navigate to Transactions screen
                        onNavigateToViewTransactions = {
                            startActivity(ViewTransactions.createIntent(this))
                        },
                        // Define action for when user wants to navigate to Upload screen
                        onNavigateToUploadReceipts = {
                            startActivity(UploadReceipts.createIntent(this))
                        },
                        // Define action for when user wants to navigate back (logout)
                        onLogout = {
                            logout()
                        }
                    )
                }
            }
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(LoginPage.createIntent(this))
        finish()
    }

    // Create intent everytime the user navigates to the HomePage
    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, HomePage::class.java)
        }
    }
}

// Define layout and functionality of the home screen
@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    onNavigateToSetupBudget: () -> Unit,
    onNavigateToViewTransactions: () -> Unit,
    onNavigateToUploadReceipts: () -> Unit,
    onLogout: () -> Unit
) {
    // Organize child elements vertically
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Budget Planner",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onNavigateToSetupBudget()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Setup Budget")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                onNavigateToViewTransactions()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "View Transactions")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                onNavigateToUploadReceipts()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Upload Receipts")
        }

        HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))

        Button(
            onClick = onLogout, // Logout button
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
        ) {
            Text(text = "Logout", color = MaterialTheme.colorScheme.onError)
        }
    }
}

// Preview for Compose Preview
@Preview(showBackground = true)
@Composable
fun HomeContentPreview() {
    BudgetPlannerTheme {
        HomeContent(
            modifier = Modifier,
            onNavigateToSetupBudget = {},
            onNavigateToViewTransactions = {},
            onNavigateToUploadReceipts = {},
            onLogout = {}
        )
    }
}
