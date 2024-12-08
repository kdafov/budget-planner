package com.budgetplanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetplanner.ui.theme.BudgetPlannerTheme
import com.google.firebase.database.FirebaseDatabase
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.MutableLiveData
import coil.compose.AsyncImage

class ViewTransactions : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Declare the state here so it is shared with the composable
        var showAddTransactionDialog by mutableStateOf(false)

        setContent {
            BudgetPlannerTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("View Transactions") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = { showAddTransactionDialog = true }) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Transaction")
                        }
                    }
                ) { innerPadding ->
                    ViewTransactionsContent(
                        modifier = Modifier.padding(innerPadding),
                        showAddTransactionDialog = showAddTransactionDialog,
                        onDismissAddTransactionDialog = { showAddTransactionDialog = false }
                    )
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, ViewTransactions::class.java)
        }
    }
}

@Composable
fun ViewTransactionsContent(
    modifier: Modifier = Modifier,
    showAddTransactionDialog: Boolean,
    onDismissAddTransactionDialog: () -> Unit
) {
    val transactions = remember { MutableLiveData<List<Transaction>>() }
    val totalAmount = remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    // Fetch transactions when the Composable is created
    LaunchedEffect(Unit) {
        fetchTransactionsFromFirebase(context, transactions, totalAmount)
    }

    val transactionList by transactions.observeAsState(emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Total Spent: $${totalAmount.intValue}",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(transactionList) { transaction ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Name: ${transaction.name}")
                            Text("Amount: $${transaction.amount}")
                            if (transaction.image.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                AsyncImage(
                                    model = transaction.image,
                                    contentDescription = "Transaction Receipt",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(8.dp),
                                    placeholder = painterResource(id = R.drawable.no_content),
                                    error = painterResource(id = R.drawable.no_content)
                                )
                            }
                        }
                        IconButton(onClick = {
                            deleteTransactionFromFirebase(context, transaction.id) {
                                fetchTransactionsFromFirebase(context, transactions, totalAmount)
                            }
                        }) {
                            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }

        if (showAddTransactionDialog) {
            AddTransactionDialog(
                onDismiss = { onDismissAddTransactionDialog() },
                onSave = { name, amount ->
                    addTransactionToFirebase(context, Transaction(name, amount)) {
                        onDismissAddTransactionDialog()
                        fetchTransactionsFromFirebase(context, transactions, totalAmount)
                    }
                }
            )
        }
    }
}

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Add Transaction") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Transaction Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amountValue = amount.toDoubleOrNull()
                if (amountValue != null) {
                    onSave(name, amountValue)
                } else {
                    // Use the context to show Toast
                    Toast.makeText(context, "Invalid amount", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        }
    )
}

data class Transaction(
    val name: String = "",
    val amount: Double = 0.0,
    val image: String = "",
    val id: String = ""
)

fun fetchTransactionsFromFirebase(
    context: Context,
    transactionsLiveData: MutableLiveData<List<Transaction>>,
    totalAmount: MutableState<Int>
) {
    val userId = getUserIdFromPreferences(context)
    val database = FirebaseDatabase.getInstance()
    val transactionsRef = database.getReference("users").child(userId).child("transactions")

    transactionsRef.get().addOnSuccessListener { snapshot ->
        val transactionList = mutableListOf<Transaction>()
        var total = 0

        for (child in snapshot.children) {
            val name = child.child("name").value as? String ?: "Unknown"
            val amount = child.child("amount").value.toString().toDoubleOrNull() ?: 0.0
            val image = child.child("image").value as? String ?: ""
            val id = child.key ?: ""

            total += amount.toInt()
            transactionList.add(Transaction(name, amount, image, id))
        }
        transactionsLiveData.value = transactionList
        totalAmount.value = total
    }.addOnFailureListener {
        Toast.makeText(context, "Failed to fetch transactions", Toast.LENGTH_SHORT).show()
    }
}

fun addTransactionToFirebase(context: Context, transaction: Transaction, onComplete: () -> Unit) {
    val userId = getUserIdFromPreferences(context)
    val database = FirebaseDatabase.getInstance()
    val transactionsRef = database.getReference("users").child(userId).child("transactions")

    val newTransactionRef = transactionsRef.push()
    val transactionData = mapOf("name" to transaction.name, "amount" to transaction.amount, "image" to "")

    newTransactionRef.setValue(transactionData)
        .addOnSuccessListener { onComplete() }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to add transaction", Toast.LENGTH_SHORT).show()
        }
}

fun deleteTransactionFromFirebase(context: Context, transactionId: String, onComplete: () -> Unit) {
    val userId = getUserIdFromPreferences(context)
    val database = FirebaseDatabase.getInstance()
    val transactionRef = database.getReference("users").child(userId).child("transactions").child(transactionId)

    transactionRef.removeValue()
        .addOnSuccessListener { onComplete() }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to delete transaction", Toast.LENGTH_SHORT).show()
        }
}
