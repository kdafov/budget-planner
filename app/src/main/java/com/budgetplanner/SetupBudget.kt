package com.budgetplanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetplanner.ui.theme.BudgetPlannerTheme
import com.google.firebase.database.FirebaseDatabase
import android.view.inputmethod.InputMethodManager

class SetupBudget : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BudgetPlannerTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Setup Budget") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    SetupBudgetContent(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, SetupBudget::class.java)
        }
    }
}

@Composable
fun SetupBudgetContent(modifier: Modifier = Modifier) {
    var budget by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var showSnackBar by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Set Your Monthly Budget",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = budget,
            onValueChange = { budget = it },
            label = { Text("Enter Budget (e.g., 5000)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                isSaving = true
                saveBudgetToFirebase(context, budget) { success ->
                    isSaving = false
                    if (success) {
                        showSnackBar = true
                    } else {
                        Toast.makeText(context, "Failed to save budget", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Save Budget")
            }
        }

        // SnackBar for feedback
        if (showSnackBar) {
            SnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Snackbar(
                    action = {
                        Button(onClick = { showSnackBar = false }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text("Budget saved successfully!")
                }
            }
        }
    }
}

fun saveBudgetToFirebase(context: Context, budget: String, onComplete: (Boolean) -> Unit) {
    val budgetValue = budget.toIntOrNull()

    if (budgetValue == null) {
        Toast.makeText(context, "Invalid budget value", Toast.LENGTH_SHORT).show()
        onComplete(false)
        return
    }

    val userId = getUserIdFromPreferences(context)
    val database = FirebaseDatabase.getInstance()
    val userRef = database.getReference("users").child(userId)

    userRef.child("budget").setValue(budget)
        .addOnSuccessListener {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val windowToken = (context as ComponentActivity).currentFocus?.windowToken
            if (windowToken != null) {
                imm.hideSoftInputFromWindow(windowToken, 0)
            }

            Toast.makeText(context, "Budget saved", Toast.LENGTH_SHORT).show()
            onComplete(true)
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to save budget", Toast.LENGTH_SHORT).show()
            onComplete(false)
        }
}

fun getUserIdFromPreferences(context: Context): String {
    val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    return sharedPreferences.getString("USER_ID", "") ?: ""
}
