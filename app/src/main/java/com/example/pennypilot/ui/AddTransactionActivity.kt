package com.example.pennypilot.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pennypilot.R
import com.example.pennypilot.data.TransactionRepository
import com.example.pennypilot.databinding.ActivityAddTransactionBinding
import com.example.pennypilot.model.Transaction
import com.example.pennypilot.notification.NotificationManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var notificationManager: NotificationManager
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository(this)
        notificationManager = NotificationManager(this)

        setupCategorySpinner()
        setupDatePicker()
        setupButtons()

        // Handle transaction_type from intent
        val transactionType = intent.getStringExtra("transaction_type")
        when (transactionType) {
            "income" -> {
                binding.radioIncome.isChecked = true
                binding.spinnerCategory.setSelection(getCategoryPosition("Income"))
            }
            "expense" -> {
                binding.radioExpense.isChecked = true
                binding.spinnerCategory.setSelection(getCategoryPosition("Food"))
            }
        }
    }

    private fun setupCategorySpinner() {
        val categories = listOf(
            "Food", "Transport", "Bills", "Entertainment", "Shopping",
            "Health", "Education", "Housing", "Savings", "Other", "Income"
        ).toTypedArray()
        val adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, categories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun getCategoryPosition(category: String): Int {
        val categories = listOf(
            "Food", "Transport", "Bills", "Entertainment", "Shopping",
            "Health", "Education", "Housing", "Savings", "Other", "Income"
        )
        return categories.indexOf(category).coerceAtLeast(0)
    }

    private fun setupDatePicker() {
        binding.etDate.setText(dateFormatter.format(calendar.time))

        binding.etDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    binding.etDate.setText(dateFormatter.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                saveTransaction()
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (binding.etTitle.text.toString().trim().isEmpty()) {
            binding.etTitle.error = getString(R.string.field_required)
            isValid = false
        }

        if (binding.etAmount.text.toString().trim().isEmpty()) {
            binding.etAmount.error = getString(R.string.field_required)
            isValid = false
        }

        return isValid
    }

    private fun saveTransaction() {
        try {
            val title = binding.etTitle.text.toString().trim()
            val amount = binding.etAmount.text.toString().toDouble()
            val category = binding.spinnerCategory.selectedItem.toString()
            val date = calendar.time
            val isExpense = binding.radioExpense.isChecked

            val transaction = Transaction(
                title = title,
                amount = amount,
                category = category,
                date = date,
                isExpense = isExpense
            )

            transactionRepository.saveTransaction(transaction)
            notificationManager.checkBudgetAndNotify()

            Toast.makeText(
                this,
                getString(R.string.transaction_added),
                Toast.LENGTH_SHORT
            ).show()

            finish()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.error_adding_transaction),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}