package app.balancebeacon.mobileandroid.feature.dashboard.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardComparisonDto
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardBudgetProgressDto
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardHistoryPointDto
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardRecentTransactionDto
import app.balancebeacon.mobileandroid.feature.dashboard.model.DashboardTransactionRequestDto
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel
import kotlinx.serialization.json.JsonElement
import java.util.Locale

@Composable
fun DashboardOverviewScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val data = state.data

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
            item {
                SummarySection(
                    totalIncome = dashboard.summary.totalIncome,
                    totalExpenses = dashboard.summary.totalExpenses,
                    netResult = dashboard.summary.netResult
                )
            }
            item {
                DashboardTrendCard(
                    history = dashboard.history,
                    comparison = dashboard.comparison
                )
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
                RequestsSection(items = dashboard.transactionRequests)
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
                    BudgetProgressItem(budget = budget)
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
                    RecentTransactionItem(transaction = transaction)
                }
            }
        }
    }
}

@Composable
private fun SummarySection(
    totalIncome: String,
    totalExpenses: String,
    netResult: String
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
                    value = totalIncome,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Expenses",
                    value = totalExpenses,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Balance",
                    value = netResult,
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
fun DashboardTrendCard(
    history: List<DashboardHistoryPointDto>,
    comparison: DashboardComparisonDto?,
    modifier: Modifier = Modifier
) {
    val points = history.mapNotNull { historyPoint ->
        historyPoint.net.toDoubleOrNull()?.let { netValue ->
            TrendPoint(month = historyPoint.month, net = netValue)
        }
    }

    val latest = points.lastOrNull()?.net
    val change = comparison?.change.toDoubleOrNull()
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
                    Text(points.first().month, style = MaterialTheme.typography.bodySmall)
                    Text(points.last().month, style = MaterialTheme.typography.bodySmall)
                }
            }

            latest?.let {
                Text(
                    text = "Current net: ${formatSignedAmount(it, withPlus = false)}",
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
                    text = "Vs $previousMonthLabel: ${formatSignedAmount(change)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = changeColor
                )
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
                center = androidx.compose.ui.geometry.Offset(stepX * index, yFor(point.net))
            )
        }
    }
}

@Composable
private fun RequestsSection(items: List<DashboardTransactionRequestDto>) {
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
                    Text(
                        text = "$fromName • ${request.amount} ${request.currency} • $categoryName • ${request.date}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    request.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private data class TrendPoint(
    val month: String,
    val net: Double
)

private fun JsonElement?.toDoubleOrNull(): Double? {
    val raw = this?.toString() ?: return null
    return raw.trim().trim('"').toDoubleOrNull()
}

private fun formatSignedAmount(value: Double, withPlus: Boolean = true): String {
    return if (withPlus && value >= 0.0) {
        String.format(Locale.US, "+%.2f", value)
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}

@Composable
private fun BudgetProgressItem(
    budget: DashboardBudgetProgressDto
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(budget.categoryName, style = MaterialTheme.typography.titleSmall)
            Text(
                "${budget.spent} / ${budget.budgeted} (${budget.percentUsed}%)",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Remaining: ${budget.remaining}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RecentTransactionItem(
    transaction: DashboardRecentTransactionDto
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val categoryLabel = transaction.category?.name ?: "Uncategorized"
            val title = transaction.description?.takeIf { it.isNotBlank() } ?: "Transaction"
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text("${transaction.amount} • $categoryLabel", style = MaterialTheme.typography.bodyMedium)
            Text(transaction.date, style = MaterialTheme.typography.bodySmall)
        }
    }
}
