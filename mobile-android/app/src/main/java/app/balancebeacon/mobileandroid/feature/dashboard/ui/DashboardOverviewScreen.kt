package app.balancebeacon.mobileandroid.feature.dashboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardBudgetProgressDto
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardComparisonDto
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardHistoryPointDto
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardPaymentHistoryItemDto
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardRecentTransactionDto
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardStatBreakdownDto
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardStatCategoryDto
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardStatDto
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardTransactionRequestDto
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Currency
import java.util.Locale

@Composable
fun DashboardOverviewScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val data = state.data
    var expandedStatLabel by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (state.data == null && !state.isLoading) {
            viewModel.loadDashboard()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Overview", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Load dashboard summary, budgets, recent activity, and sharing signals.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = state.accountId,
                        onValueChange = viewModel::onAccountIdChanged,
                        label = { Text("Account ID (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.monthKey,
                        onValueChange = viewModel::onMonthKeyChanged,
                        label = { Text("Month (YYYY-MM, optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.loadDashboard() },
                            enabled = !state.isLoading
                        ) {
                            Text(if (state.isLoading) "Loading..." else "Load Overview")
                        }
                    }

                    if (state.isLoading) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator()
                            Text("Fetching dashboard data...")
                        }
                    }
                    state.statusMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    state.error?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        data?.let { dashboard ->
            val currencyCode = dashboard.preferredCurrency?.ifBlank { null } ?: "USD"

            item {
                SummarySection(
                    totalIncome = parseAmount(dashboard.summary.totalIncome),
                    totalExpenses = parseAmount(dashboard.summary.totalExpenses),
                    netResult = parseAmount(dashboard.summary.netResult),
                    currencyCode = currencyCode
                )
            }
            if (dashboard.stats.isNotEmpty()) {
                item {
                    StatsSection(
                        stats = dashboard.stats,
                        expandedStatLabel = expandedStatLabel,
                        currencyCode = currencyCode,
                        onSelectStat = { statLabel ->
                            expandedStatLabel = if (expandedStatLabel == statLabel) null else statLabel
                        }
                    )
                }
            }
            item {
                DashboardTrendCard(
                    history = dashboard.history,
                    comparison = dashboard.comparison,
                    currencyCode = currencyCode
                )
            }
            item {
                MonthAtGlanceSection(
                    comparison = dashboard.comparison,
                    budgetProgress = dashboard.budgetProgress,
                    currencyCode = currencyCode
                )
            }
            dashboard.exchangeRateLastUpdate?.let { updatedAt ->
                item {
                    ExchangeRateSection(updatedAt = updatedAt)
                }
            }
            if (dashboard.paymentHistory.isNotEmpty()) {
                item {
                    PaymentHistorySection(
                        paymentHistory = dashboard.paymentHistory,
                        defaultCurrencyCode = currencyCode
                    )
                }
            }
            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Pending shared expenses: ${dashboard.pendingSharedExpenses}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            item {
                RequestsSection(
                    items = dashboard.transactionRequests,
                    defaultCurrencyCode = currencyCode,
                    requestActionInProgressId = state.requestActionInProgressId,
                    onApproveRequest = viewModel::approveTransactionRequest,
                    onRejectRequest = viewModel::rejectTransactionRequest
                )
            }

            val highlightedBudgets = dashboard.budgetProgress
                .sortedByDescending { it.percentUsed }
                .take(3)
                .filter { it.percentUsed > 0 }
            if (highlightedBudgets.isNotEmpty()) {
                item {
                    Text("Highlighted Budgets", style = MaterialTheme.typography.titleMedium)
                }
                items(
                    items = highlightedBudgets,
                    key = { "highlight:${it.categoryId}:${it.categoryName}" }
                ) { budget ->
                    BudgetProgressItem(
                        budget = budget,
                        currencyCode = currencyCode
                    )
                }
            }

            item {
                Text("Budget Progress", style = MaterialTheme.typography.titleMedium)
            }
            if (dashboard.budgetProgress.isEmpty()) {
                item { Text("No budget progress for this month") }
            } else {
                items(
                    items = dashboard.budgetProgress,
                    key = { "${it.categoryId}:${it.categoryName}" }
                ) { budget ->
                    BudgetProgressItem(
                        budget = budget,
                        currencyCode = currencyCode
                    )
                }
            }

            item {
                Text("Recent Transactions", style = MaterialTheme.typography.titleMedium)
            }
            if (dashboard.recentTransactions.isEmpty()) {
                item { Text("No recent transactions") }
            } else {
                items(
                    items = dashboard.recentTransactions,
                    key = { it.id }
                ) { transaction ->
                    RecentTransactionItem(
                        transaction = transaction,
                        defaultCurrencyCode = currencyCode
                    )
                }
            }
        }
    }
}

@Composable
private fun SummarySection(
    totalIncome: Double,
    totalExpenses: Double,
    netResult: Double,
    currencyCode: String
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cashflow Snapshot", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    label = "Income",
                    value = formatCurrency(totalIncome, currencyCode),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Expenses",
                    value = formatCurrency(totalExpenses, currencyCode),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Balance",
                    value = formatSignedCurrency(netResult, currencyCode, withPlus = false),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    GlassPanel(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun StatsSection(
    stats: List<DashboardStatDto>,
    expandedStatLabel: String?,
    currencyCode: String,
    onSelectStat: (String) -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Insights", style = MaterialTheme.typography.titleMedium)

            stats.forEach { stat ->
                val isExpanded = expandedStatLabel == stat.label
                val hasBreakdown = stat.breakdown != null
                val variantColor = when (stat.variant) {
                    "positive" -> Color(0xFF4CD97B)
                    "negative" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { base ->
                            if (hasBreakdown) {
                                base.clickable { onSelectStat(stat.label) }
                            } else {
                                base
                            }
                        }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stat.label,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = formatCurrency(stat.amount, currencyCode),
                                style = MaterialTheme.typography.titleSmall,
                                color = variantColor
                            )
                        }
                        stat.helper?.takeIf { it.isNotBlank() }?.let { helper ->
                            Text(
                                text = helper,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isExpanded && stat.breakdown != null) {
                    StatBreakdownSection(
                        breakdown = stat.breakdown,
                        currencyCode = currencyCode
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBreakdownSection(
    breakdown: DashboardStatBreakdownDto,
    currencyCode: String
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Breakdown", style = MaterialTheme.typography.titleSmall)
            when (breakdown.type) {
                "net-this-month" -> {
                    KeyValueRow("Income", formatCurrency(breakdown.income ?: 0.0, currencyCode))
                    KeyValueRow("Expense", formatCurrency(breakdown.expense ?: 0.0, currencyCode))
                    KeyValueRow("Net", formatSignedCurrency(breakdown.net ?: 0.0, currencyCode))
                }

                "on-track-for" -> {
                    KeyValueRow("Actual income", formatCurrency(breakdown.actualIncome ?: 0.0, currencyCode))
                    KeyValueRow("Actual expense", formatCurrency(breakdown.actualExpense ?: 0.0, currencyCode))
                    KeyValueRow(
                        "Expected remaining income",
                        formatCurrency(breakdown.expectedRemainingIncome ?: 0.0, currencyCode)
                    )
                    KeyValueRow(
                        "Remaining budgeted expense",
                        formatCurrency(breakdown.remainingBudgetedExpense ?: 0.0, currencyCode)
                    )
                    KeyValueRow("Projected result", formatSignedCurrency(breakdown.projected ?: 0.0, currencyCode))
                    breakdown.incomeSource?.let { source ->
                        KeyValueRow("Income source", source.replaceFirstChar { it.uppercase() })
                    }
                }

                "left-to-spend" -> {
                    KeyValueRow("Total planned", formatCurrency(breakdown.totalPlanned ?: 0.0, currencyCode))
                    KeyValueRow("Total actual", formatCurrency(breakdown.totalActual ?: 0.0, currencyCode))
                    KeyValueRow("Total remaining", formatCurrency(breakdown.totalRemaining ?: 0.0, currencyCode))
                    if (breakdown.categories.isNotEmpty()) {
                        Text(
                            text = "Top categories",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        breakdown.categories
                            .sortedByDescending { it.actual / (it.planned.takeIf { planned -> planned > 0.0 } ?: 1.0) }
                            .take(3)
                            .forEach { category ->
                                CategoryBreakdownRow(category = category, currencyCode = currencyCode)
                            }
                    }
                }

                "monthly-target" -> {
                    KeyValueRow("Planned income", formatCurrency(breakdown.plannedIncome ?: 0.0, currencyCode))
                    KeyValueRow("Planned expense", formatCurrency(breakdown.plannedExpense ?: 0.0, currencyCode))
                    KeyValueRow("Target", formatSignedCurrency(breakdown.target ?: 0.0, currencyCode))
                    breakdown.incomeSource?.let { source ->
                        KeyValueRow("Income source", source.replaceFirstChar { it.uppercase() })
                    }
                }

                else -> {
                    Text(
                        text = "No structured breakdown available for this insight yet.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    category: DashboardStatCategoryDto,
    currencyCode: String
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = category.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Spent ${formatCurrency(category.actual, currencyCode)} / " +
                    "Planned ${formatCurrency(category.planned, currencyCode)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Remaining ${formatCurrency(category.remaining, currencyCode)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MonthAtGlanceSection(
    comparison: DashboardComparisonDto?,
    budgetProgress: List<DashboardBudgetProgressDto>,
    currencyCode: String
) {
    val expenseRemaining = budgetProgress
        .filter { it.categoryType == "EXPENSE" }
        .sumOf { parseAmount(it.remaining) }
        .coerceAtLeast(0.0)
    val incomeExpected = budgetProgress
        .filter { it.categoryType == "INCOME" }
        .sumOf { parseAmount(it.remaining) }
        .coerceAtLeast(0.0)

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Month at a Glance", style = MaterialTheme.typography.titleMedium)
            comparison?.let {
                KeyValueRow(
                    label = "Net delta vs ${formatMonthLabel(it.previousMonth)}",
                    value = formatSignedCurrency(it.change, currencyCode)
                )
                KeyValueRow(
                    label = "Previous net",
                    value = formatCurrency(it.previousNet, currencyCode)
                )
            }
            KeyValueRow(
                label = "Budgets remaining",
                value = formatCurrency(expenseRemaining, currencyCode)
            )
            KeyValueRow(
                label = "Income still expected",
                value = formatCurrency(incomeExpected, currencyCode)
            )
        }
    }
}

@Composable
private fun ExchangeRateSection(updatedAt: String) {
    val formatted = runCatching {
        val parsed = OffsetDateTime.parse(updatedAt)
        parsed.format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"))
    }.getOrNull() ?: updatedAt

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Exchange rate data", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Last update: $formatted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaymentHistorySection(
    paymentHistory: List<DashboardPaymentHistoryItemDto>,
    defaultCurrencyCode: String
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Recent Settlements", style = MaterialTheme.typography.titleMedium)
            paymentHistory.take(5).forEach { item ->
                val amount = parseAmount(item.amount)
                val sign = if (item.direction == "received") "+" else "-"
                val formattedAmount = formatCurrency(amount, item.currency.ifBlank { defaultCurrencyCode })
                val parsedDate = runCatching {
                    OffsetDateTime.parse(item.paidAt).format(DateTimeFormatter.ofPattern("MMM d"))
                }.getOrNull() ?: item.paidAt
                Text(
                    text = "$parsedDate • ${item.userDisplayName} • $sign$formattedAmount",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun DashboardTrendCard(
    history: List<DashboardHistoryPointDto>,
    comparison: DashboardComparisonDto?,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    val points = history.map { historyPoint ->
        TrendPoint(
            month = historyPoint.month,
            income = historyPoint.income,
            expense = historyPoint.expense,
            net = historyPoint.net
        )
    }

    val latest = points.lastOrNull()?.net
    val change = comparison?.change
    val previousMonthLabel = comparison?.previousMonth?.ifBlank { null }

    GlassPanel(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Trend", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Cashflow trajectory across recent months",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (points.size < 2) {
                Text(
                    text = "Need at least 2 months of data to draw trend.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                TrendChart(points = points)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatMonthLabel(points.first().month), style = MaterialTheme.typography.bodySmall)
                    Text(formatMonthLabel(points.last().month), style = MaterialTheme.typography.bodySmall)
                }
            }

            latest?.let {
                Text(
                    text = "Current net: ${formatSignedCurrency(it, currencyCode, withPlus = false)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (change != null && previousMonthLabel != null) {
                val changeColor = if (change >= 0.0) {
                    Color(0xFF4CD97B)
                } else {
                    MaterialTheme.colorScheme.error
                }
                Text(
                    text = "Vs ${formatMonthLabel(previousMonthLabel)}: ${formatSignedCurrency(change, currencyCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = changeColor
                )
            }

            points.takeLast(6).forEach { point ->
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = formatMonthLabel(point.month), style = MaterialTheme.typography.bodySmall)
                        Column {
                            Text(
                                text = "Income ${formatCurrency(point.income, currencyCode)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Expense ${formatCurrency(point.expense, currencyCode)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Net ${formatSignedCurrency(point.net, currencyCode)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    val accent = Color(0xFF74D6FF)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        if (points.size < 2) return@Canvas

        val maxValue = points.maxOf { it.net }
        val minValue = points.minOf { it.net }
        val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
        val chartHeight = size.height
        val chartWidth = size.width
        val stepX = chartWidth / (points.size - 1).toFloat()

        fun yFor(value: Double): Float {
            val normalized = ((maxValue - value) / range).toFloat()
            return normalized * chartHeight
        }

        val linePath = Path().apply {
            points.forEachIndexed { index, point ->
                val x = stepX * index
                val y = yFor(point.net)
                if (index == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(chartWidth, chartHeight)
            lineTo(0f, chartHeight)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    accent.copy(alpha = 0.35f),
                    accent.copy(alpha = 0.04f)
                )
            )
        )

        drawPath(
            path = linePath,
            color = accent,
            style = Stroke(
                width = 4f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        points.forEachIndexed { index, point ->
            drawCircle(
                color = accent,
                radius = 4.5f,
                center = Offset(stepX * index, yFor(point.net))
            )
        }
    }
}

@Composable
private fun RequestsSection(
    items: List<DashboardTransactionRequestDto>,
    defaultCurrencyCode: String,
    requestActionInProgressId: String?,
    onApproveRequest: (String) -> Unit,
    onRejectRequest: (String) -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Pending Requests", style = MaterialTheme.typography.titleMedium)
            if (items.isEmpty()) {
                Text("No pending requests")
            } else {
                items.take(5).forEach { request ->
                    val fromName = request.from?.name
                        ?: request.from?.email
                        ?: "Unknown sender"
                    val categoryName = request.category?.name ?: "Uncategorized"
                    val amount = parseAmount(request.amount)
                    val formattedAmount = formatCurrency(
                        amount = amount,
                        currencyCode = request.currency.ifBlank { defaultCurrencyCode }
                    )
                    Text(
                        text = "$fromName • $formattedAmount • $categoryName • ${request.date}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    request.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isActionRunning = requestActionInProgressId == request.id
                        Button(
                            onClick = { onApproveRequest(request.id) },
                            enabled = !isActionRunning && requestActionInProgressId == null
                        ) {
                            Text(if (isActionRunning) "Working..." else "Approve")
                        }
                        Button(
                            onClick = { onRejectRequest(request.id) },
                            enabled = !isActionRunning && requestActionInProgressId == null
                        ) {
                            Text(if (isActionRunning) "Working..." else "Reject")
                        }
                    }
                }
            }
        }
    }
}

private data class TrendPoint(
    val month: String,
    val income: Double,
    val expense: Double,
    val net: Double
)

private fun parseAmount(value: String?): Double {
    return value?.trim()?.toDoubleOrNull() ?: 0.0
}

private fun formatCurrency(amount: Double, currencyCode: String): String {
    return runCatching {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        formatter.currency = Currency.getInstance(currencyCode)
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 2
        formatter.format(amount)
    }.getOrElse {
        String.format(Locale.US, "%.2f %s", amount, currencyCode)
    }
}

private fun formatSignedCurrency(
    amount: Double,
    currencyCode: String,
    withPlus: Boolean = true
): String {
    val formatted = formatCurrency(kotlin.math.abs(amount), currencyCode)
    return when {
        amount > 0.0 && withPlus -> "+$formatted"
        amount < 0.0 -> "-$formatted"
        else -> formatted
    }
}

private fun formatMonthLabel(monthKey: String): String {
    return runCatching {
        val month = YearMonth.parse(monthKey, DateTimeFormatter.ofPattern("yyyy-MM"))
        "${month.month.getDisplayName(TextStyle.SHORT, Locale.US)} ${month.year}"
    }.getOrElse { monthKey }
}

@Composable
private fun BudgetProgressItem(
    budget: DashboardBudgetProgressDto,
    currencyCode: String
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(budget.categoryName, style = MaterialTheme.typography.titleSmall)
            Text(
                "${formatCurrency(parseAmount(budget.spent), currencyCode)} / " +
                    "${formatCurrency(parseAmount(budget.budgeted), currencyCode)} (${budget.percentUsed}%)",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Remaining: ${formatCurrency(parseAmount(budget.remaining), currencyCode)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RecentTransactionItem(
    transaction: DashboardRecentTransactionDto,
    defaultCurrencyCode: String
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val categoryLabel = transaction.category?.name ?: "Uncategorized"
            val title = transaction.description?.takeIf { it.isNotBlank() } ?: "Transaction"
            val amount = parseAmount(transaction.amount)
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                "${formatCurrency(amount, defaultCurrencyCode)} • $categoryLabel",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(transaction.date, style = MaterialTheme.typography.bodySmall)
        }
    }
}
