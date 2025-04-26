package com.example.pennypilot.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.pennypilot.R
import com.example.pennypilot.data.PreferencesManager
import com.example.pennypilot.data.TransactionRepository
import com.example.pennypilot.databinding.ActivityMainBinding
import com.example.pennypilot.notification.NotificationManager
import com.example.pennypilot.ui.adapters.RecentTransactionsAdapter
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var adapter: RecentTransactionsAdapter
    private var selectedCategory: String? = null

    private val calendar = Calendar.getInstance()

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository(this)
        preferencesManager = PreferencesManager(this)
        notificationManager = NotificationManager(this)

        // Request notification permission
        requestNotificationPermission()

        notificationManager.scheduleDailyReminder()

        setupWelcomeMessage()
        setupBottomNavigation()
        setupMonthDisplay()
        setupSummaryCards()
        setupQuickActions()
        setupSavingsGoal()
        setupCategoryChart()
        setupCategoryFilter()
        setupRecentTransactions()

        binding.btnAddTransaction.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        binding.btnPreviousMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateDashboard()
        }

        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateDashboard()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Notification permission denied. You won't receive budget alerts.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupWelcomeMessage() {
        val userName = preferencesManager.getUserName() ?: "User"
        binding.tvWelcomeMessage.text = getString(R.string.welcome_message, userName)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_budget -> {
                    startActivity(Intent(this, BudgetActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupMonthDisplay() {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvCurrentMonth.text = dateFormat.format(calendar.time)
    }

    private fun setupSummaryCards() {
        val currency = preferencesManager.getCurrency()
        val totalIncome = transactionRepository.getTotalIncomeForMonth(
            calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)
        )
        val totalExpenses = transactionRepository.getTotalExpensesForMonth(
            calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)
        )
        val balance = totalIncome - totalExpenses

        binding.tvIncomeAmount.text = String.format("%s %.2f", currency, totalIncome)
        binding.tvExpenseAmount.text = String.format("%s %.2f", currency, totalExpenses)
        binding.tvBalanceAmount.text = String.format("%s %.2f", currency, balance)

        // Budget progress
        val budget = preferencesManager.getBudget()
        if (budget.month == calendar.get(Calendar.MONTH) &&
            budget.year == calendar.get(Calendar.YEAR) &&
            budget.amount > 0
        ) {
            val percentage = (totalExpenses / budget.amount) * 100
            binding.progressBudget.progress = percentage.toInt().coerceAtMost(100)
            binding.tvBudgetStatus.text = String.format(
                "%.1f%% of %s %.2f", percentage, currency, budget.amount
            )

            when {
                percentage >= 100 -> binding.tvBudgetStatus.setTextColor(resources.getColor(R.color.budget_exceeded, null))
                percentage >= 80 -> binding.tvBudgetStatus.setTextColor(resources.getColor(R.color.budget_warning, null))
                else -> binding.tvBudgetStatus.setTextColor(resources.getColor(R.color.budget_good, null))
            }
        } else {
            binding.progressBudget.progress = 0
            binding.tvBudgetStatus.text = getString(R.string.no_budget_set)
            binding.tvBudgetStatus.setTextColor(resources.getColor(R.color.text_secondary, null))
        }
    }

    private fun setupQuickActions() {
        binding.btnAddIncome.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java).apply {
                putExtra("transaction_type", "income")
            }
            startActivity(intent)
        }

        binding.btnAddExpense.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java).apply {
                putExtra("transaction_type", "expense")
            }
            startActivity(intent)
        }

        binding.btnSetBudget.setOnClickListener {
            startActivity(Intent(this, BudgetActivity::class.java))
        }
    }

    private fun setupSavingsGoal() {
        val savingsGoal = preferencesManager.getSavingsGoal()
        val currentSavings = transactionRepository.getTotalSavings(
            calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)
        )
        val currency = preferencesManager.getCurrency()

        if (savingsGoal > 0) {
            val percentage = (currentSavings / savingsGoal) * 100
            binding.tvSavingsGoalAmount.text = String.format(
                "%s %.2f / %s %.2f", currency, currentSavings, currency, savingsGoal
            )
            binding.progressSavingsGoal.progress = percentage.toInt().coerceAtMost(100)
            binding.tvSavingsGoalStatus.text = String.format("%.1f%% of goal reached", percentage)
            binding.tvSavingsGoalStatus.setTextColor(
                if (percentage >= 100) resources.getColor(R.color.success, null)
                else resources.getColor(R.color.text_secondary, null)
            )
        } else {
            binding.tvSavingsGoalAmount.text = getString(R.string.no_savings_goal_set)
            binding.progressSavingsGoal.progress = 0
            binding.tvSavingsGoalStatus.text = getString(R.string.set_savings_goal)
            binding.tvSavingsGoalStatus.setTextColor(resources.getColor(R.color.text_secondary, null))
        }
    }

    private fun setupCategoryChart() {
        val expensesByCategory = if (selectedCategory == null || selectedCategory == "All Categories") {
            transactionRepository.getExpensesByCategory(
                calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)
            )
        } else {
            transactionRepository.getExpensesByCategory(
                calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)
            ).filterKeys { it == selectedCategory }
        }

        if (expensesByCategory.isEmpty()) {
            binding.pieChart.setNoDataText(getString(R.string.no_expenses_this_month))
            binding.pieChart.invalidate()
            return
        }

        val entries = ArrayList<PieEntry>()
        val colors = listOf(
            R.color.chart_color_1, R.color.chart_color_2, R.color.chart_color_3,
            R.color.chart_color_4, R.color.chart_color_5, R.color.chart_color_6,
            R.color.chart_color_7, R.color.chart_color_8
        ).map { resources.getColor(it, null) }

        // Process each category
        expensesByCategory.entries.forEachIndexed { index, entry ->
            entries.add(PieEntry(entry.value.toFloat(), entry.key))
        }

        // Create dataset
        val dataSet = PieDataSet(entries, getString(R.string.expenses_by_category))
        dataSet.colors = colors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = resources.getColor(R.color.white, null)

        // Create pie data
        val pieData = PieData(dataSet)
        binding.pieChart.data = pieData

        // Chart formatting
        binding.pieChart.description.isEnabled = false
        binding.pieChart.legend.isEnabled = true
        binding.pieChart.legend.textSize = 12f
        binding.pieChart.setEntryLabelColor(resources.getColor(R.color.text_primary, null))
        binding.pieChart.setEntryLabelTextSize(12f)
        binding.pieChart.setUsePercentValues(true)
        binding.pieChart.animateY(1000)
        binding.pieChart.setDrawHoleEnabled(true)
        binding.pieChart.holeRadius = 40f
        binding.pieChart.transparentCircleRadius = 45f

        // Refresh chart
        binding.pieChart.invalidate()
    }

    private fun setupCategoryFilter() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.category_filters,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategoryFilter.adapter = adapter

        binding.spinnerCategoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedCategory = parent.getItemAtPosition(position).toString()
                setupCategoryChart()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedCategory = null
                setupCategoryChart()
            }
        }
    }

    private fun setupRecentTransactions() {
        val transactions = transactionRepository.getTransactionsForMonth(
            calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)
        ).sortedByDescending { it.date }.take(5)

        adapter = RecentTransactionsAdapter(transactions, preferencesManager.getCurrency())
        binding.recyclerRecentTransactions.adapter = adapter

        binding.tvViewAllTransactions.setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private fun updateDashboard() {
        setupWelcomeMessage()
        setupMonthDisplay()
        setupSummaryCards()
        setupSavingsGoal()
        setupCategoryChart()
        setupRecentTransactions()
    }

    override fun onResume() {
        super.onResume()
        val budget = preferencesManager.getBudget()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        if (budget.month == currentMonth && budget.year == currentYear && budget.amount > 0) {
            val totalExpenses = transactionRepository.getTotalExpensesForMonth(currentMonth, currentYear)
            val budgetPercentage = (totalExpenses / budget.amount) * 100
            Log.d("MainActivity", "Budget: $totalExpenses / ${budget.amount} = $budgetPercentage%")
            Log.d("MainActivity", "Notifications enabled: ${preferencesManager.isNotificationEnabled()}")
        }

        updateDashboard()
        notificationManager.checkBudgetAndNotify()
    }
}