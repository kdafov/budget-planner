package com.budgetplanner

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetplanner.ui.theme.BudgetPlannerTheme
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class UploadReceipts : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BudgetPlannerTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Upload Receipts") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    UploadReceiptsContent(Modifier.padding(innerPadding))
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(context, UploadReceipts::class.java)
        }
    }
}

@Composable
fun UploadReceiptsContent(modifier: Modifier = Modifier) {
    var selectedTransactionId by remember { mutableStateOf<String?>(null) }
//    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf("") } // Temporary
//    var uploadProgress by remember { mutableStateOf(0f) }
//    var isUploading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val transactions = remember { mutableStateOf(emptyList<Transaction>()) }

    LaunchedEffect(Unit) {
        fetchTransactions(context) { fetchedTransactions ->
            transactions.value = fetchedTransactions
        }
    }

//    val launcher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent(),
//        onResult = { uri ->
//            selectedImageUri = uri
//        }
//    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Select a transaction and upload a receipt",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(transactions.value) { transaction ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Transaction: ${transaction.name}")
                            Text("Amount: $${transaction.amount}")
                        }
                        IconButton(
                            onClick = { selectedTransactionId = transaction.id },
                            enabled = selectedTransactionId != transaction.id
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select Transaction",
                                tint = if (selectedTransactionId == transaction.id) Color.Gray else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        /* START Temporary */
        TextField(
            value = imageUrl,
            onValueChange = { imageUrl = it },
            label = { Text("Image URL") },
            modifier = Modifier.fillMaxWidth()
        )
        /* END Temporary */

//        Button(
//            onClick = { launcher.launch("image/*") },
//            modifier = Modifier.fillMaxWidth(),
//            enabled = selectedTransactionId != null
//        ) {
//            Icon(
//                imageVector = Icons.Default.Receipt,
//                contentDescription = "Choose Receipt",
//                modifier = Modifier.padding(end = 8.dp)
//            )
//            Text(text = "Choose Receipt")
//        }

        Spacer(modifier = Modifier.height(16.dp))

//        Button(
//            onClick = {
//                isUploading = true
//                val storage = FirebaseStorage.getInstance()
//                val storageRef = storage.reference.child("receipts/$selectedTransactionId/${selectedImageUri?.lastPathSegment}")
//                selectedImageUri?.let { uri ->
//                    uploadReceiptToFirebase(context, uri, selectedTransactionId!!, storageRef) { progress ->
//                        uploadProgress = progress
//                        isUploading = progress < 1f
//                    }
//                }
//            },
//            modifier = Modifier.fillMaxWidth(),
//            enabled = selectedTransactionId != null && selectedImageUri != null && !isUploading
//        ) {
//            Icon(
//                imageVector = Icons.Default.FileUpload,
//                contentDescription = "Upload Receipt",
//                modifier = Modifier.padding(end = 8.dp)
//            )
//            Text(if (isUploading) "Uploading..." else "Upload Receipt")
//        }

        Button(
            onClick = {
                selectedTransactionId?.let {
                    uploadReceiptToFirebase(context, selectedTransactionId!!, imageUrl)
                } ?: Toast.makeText(context, "Please select a transaction", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedTransactionId != null && imageUrl.isNotEmpty()
        ) {
            Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = "Upload Receipt",
                modifier = Modifier.padding(end = 8.dp)
           )
            Text("Upload Receipt URL")
        }

        Spacer(modifier = Modifier.height(16.dp))

//        if (uploadProgress > 0 && isUploading) {
//            LinearProgressIndicator(
//                progress = { uploadProgress },
//                modifier = Modifier.fillMaxWidth(),
//            )
//        }
//
//        selectedImageUri?.let { uri ->
//            Text(text = "Selected File: ${uri.lastPathSegment}", style = MaterialTheme.typography.bodySmall)
//        }
    }
}

fun fetchTransactions(context: Context, onComplete: (List<Transaction>) -> Unit) {
    val userId = getUserIdFromPreferences(context)
    val database = FirebaseDatabase.getInstance()
    val transactionsRef = database.getReference("users").child(userId).child("transactions")

    transactionsRef.get().addOnSuccessListener { snapshot ->
        val transactionList = mutableListOf<Transaction>()
        for (child in snapshot.children) {
            val name = child.child("name").value as? String ?: "Unknown"
            val amount = child.child("amount").value.toString().toDoubleOrNull() ?: 0.0
            val id = child.key ?: ""
            transactionList.add(Transaction(name, amount, id))
        }
        onComplete(transactionList)
    }.addOnFailureListener {
        Log.e("UploadReceipts", "Failed to fetch transactions: ${it.message}")
    }
}

/*
fun uploadReceiptToFirebase(
    context: Context,
    uri: Uri,
    transactionId: String,
    storageRef: StorageReference,
    onProgress: (Float) -> Unit
) {
    val userId = getUserIdFromPreferences(context)
    val database = FirebaseDatabase.getInstance()
    val transactionRef = database.getReference("users").child(userId).child("transactions").child(transactionId)

    storageRef.putFile(uri)
        .addOnProgressListener { snapshot ->
            val progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount.toFloat()
            onProgress(progress)
        }
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                transactionRef.child("image").setValue(downloadUri.toString())
            }
            onProgress(1f)
        }
        .addOnFailureListener { e ->
            Log.e("UploadReceipts", "Failed to upload receipt: ${e.message}")
            onProgress(0f)
        }
}
*/

fun uploadReceiptToFirebase(
    context: Context,
    transactionId: String,
    imageUrl: String
) {
    val userId = getUserIdFromPreferences(context)
    val database = FirebaseDatabase.getInstance()
    val transactionRef = database.getReference("users").child(userId).child("transactions").child(transactionId)
    Log.d("TRACK THIS", transactionId)
    if (imageUrl.isEmpty()) {
        Toast.makeText(context, "Please enter a valid image URL", Toast.LENGTH_SHORT).show()
        return
    }

    transactionRef.child("image").setValue(imageUrl)
        .addOnSuccessListener {
            Toast.makeText(context, "Receipt URL uploaded successfully", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to upload receipt URL", Toast.LENGTH_SHORT).show()
        }
}

