package app.balancebeacon.mobileandroid.feature.dashboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
import app.balancebeacon.mobileandroid.ui.components.MonthNavigator
import app.balancebeacon.mobileandroid.ui.components.SkeletonLine
import app.balancebeacon.mobileandroid.ui.util.sanitizeError
import app.balancebeacon.mobileandroid.ui.theme.Emerald400
import app.balancebeacon.mobileandroid.ui.theme.GlassBorder
import app.balancebeacon.mobileandroid.ui.theme.GlassPanel
import app.balancebeacon.mobileandroid.ui.theme.GlassSurface
import app.balancebeacon.mobileandroid.ui.theme.Rose400
import app.balancebeacon.mobileandroid.ui.theme.Sky300
import app.balancebeacon.mobileandroid.ui.theme.SkyBlue
import app.balancebeacon.mobileandroid.ui.theme.Slate200
import app.balancebeacon.mobileandroid.ui.theme.Slate700
import java.text.NumberFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardOverviewScreen(
    viewModel: DashboardViewModel,
    navToAccounts: () -> Unit = {},
    navToAssistant: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val data = state.data
    var expandedStatLabel by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.initializeAccounts()
        if (state.data == null && !state.isLoading) {
            viewModel.loadDashboard()
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            viewModel.loadDashboard()
        },
        modifier = modifier.fillMaxSize()
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.accounts.isNotEmpty()) {
                        Text("Account", style = MaterialTheme.typography.labelLarge)
                        AccountSelectorRow(
                            accountNames = state.accounts.map { it.id to it.name },
                            selectedAccountId = state.accountId,
                            onSelect = viewModel::selectAccount
                        )
                    }

                    MonthNavigator(
                        monthKey = state.monthKey,
                        onPreviousMonth = viewModel::previousMonth,
                        onNextMonth = viewModel::nextMonth,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (state.isLoading) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SkeletonLine(width = 200.dp, height = 14.dp)
                            SkeletonLine(width = 140.dp, height = 14.dp)
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
                            text = sanitizeError(error),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (data == null && !state.isLoading && state.accounts.isEmpty()) {
            item {
                WelcomeEmptyState(navToAccounts = navToAccounts)
            }
        }

        data?.let { dashboard ->
            val currencyCode = dashboard.preferredCurrency?.ifBlank { null } ?: "USD"

            item {
                SummarySection(
                    totalIncome = parseAmount(dashboard.summary.totalIncome),
                    totalExpenses = parseAmount(dashboard.summary.totalExpenses),
                    netResult = parseAmount(dashboard.summary.netResult),
                    netHistory = dashboard.history.map { it.net },
                    currencyCode = currencyCode,
                    transactionCount = dashboard.recentTransactions.size,
                    streakDays = computeStreak(dashboard.recentTransactions)
                )
            }
            state.insightText?.let { insight ->
                item {
                    AiInsightCard(
                        insightText = insight,
                        onAskAi = navToAssistant
                    )
                }
            }
            item {
                MonthlyRecapCard(
                    totalIncome = parseAmount(dashboard.summary.totalIncome),
                    totalExpenses = parseAmount(dashboard.summary.totalExpenses),
                    stats = dashboard.stats,
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
                    ExchangeRateSection(
                        updatedAt = updatedAt,
                        isRefreshing = state.isRefreshingRates,
                        onRefresh = viewModel::refreshExchangeRates
                    )
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
    } // PullToRefreshBox
}

@Composable
private fun WelcomeEmptyState(navToAccounts: () -> Unit) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = SkyBlue
            )
            Text(
                text = "Welcome to Balance Beacon",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Track your spending, manage budgets, and understand your finances.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureHighlightChip(
                    icon = Icons.Default.Receipt,
                    label = "Track Spending",
                    modifier = Modifier.weight(1f)
                )
                FeatureHighlightChip(
                    icon = Icons.Default.AccountBalance,
                    label = "Set Budgets",
                    modifier = Modifier.weight(1f)
                )
                FeatureHighlightChip(
                    icon = Icons.Default.AutoAwesome,
                    label = "AI Insights",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Create your first account to begin tracking",
                style = MaterialTheme.typography.titleSmall
            )
            Button(
                onClick = navToAccounts,
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlue)
            ) {
                Text("Create Account")
            }
            Text(
                text = "Already have an account? Your data will appear here automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AiInsightCard(
    insightText: String,
    onAskAi: () -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = SkyBlue.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = SkyBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "AI Insight",
                    style = MaterialTheme.typography.titleSmall,
                    color = SkyBlue
                )
                Text(
                    insightText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate200
                )
                TextButton(onClick = onAskAi) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Ask Balance AI")
                }
            }
        }
    }
}

@Composable
private fun MonthlyRecapCard(
    totalIncome: Double,
    totalExpenses: Double,
    stats: List<DashboardStatDto>,
    currencyCode: String
) {
    var expanded by remember { mutableStateOf(false) }
    val net = totalIncome - totalExpenses

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = SkyBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Monthly Recap", style = MaterialTheme.typography.titleMedium)
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RecapMetric(
                    label = "Income",
                    value = formatCurrency(totalIncome, currencyCode),
                    color = Emerald400
                )
                RecapMetric(
                    label = "Expenses",
                    value = formatCurrency(totalExpenses, currencyCode),
                    color = Rose400
                )
                RecapMetric(
                    label = "Net",
                    value = formatCurrency(net, currencyCode),
                    color = if (net >= 0) Emerald400 else Rose400
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = GlassBorder)
                    stats.forEach { stat ->
                        val color = when (stat.variant) {
                            "positive" -> Emerald400
                            "negative" -> Rose400
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stat.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate200
                            )
                            Text(
                                formatCurrency(stat.amount, currencyCode),
                                style = MaterialTheme.typography.bodySmall,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecapMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, color = color)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeatureHighlightChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = GlassSurface,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Sky300
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Slate200
            )
        }
    }
}

@Composable
private fun MiniSparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    strokeColor: Color = Color.White,
    fillAlpha: Float = 0.15f
) {
    if (values.size < 2) {
        Box(modifier = modifier)
        return
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val maxValue = values.max()
        val minValue = values.min()
        val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
        val chartHeight = size.height
        val chartWidth = size.width
        val stepX = chartWidth / (values.size - 1).toFloat()

        fun yFor(value: Double): Float {
            val normalized = ((maxValue - value) / range).toFloat()
            return normalized * chartHeight
        }

        val linePath = Path().apply {
            values.forEachIndexed { index, value ->
                val x = stepX * index
                val y = yFor(value)
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
                    strokeColor.copy(alpha = fillAlpha),
                    strokeColor.copy(alpha = 0.02f)
                )
            )
        )

        drawPath(
            path = linePath,
            color = strokeColor,
            style = Stroke(
                width = 2f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
private fun SummarySection(
    totalIncome: Double,
    totalExpenses: Double,
    netResult: Double,
    netHistory: List<Double>,
    currencyCode: String,
    transactionCount: Int = 0,
    streakDays: Int = 0
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cashflow Snapshot", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (streakDays > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GlassSurface,
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "\uD83D\uDD25",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "$streakDays day streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate200
                                )
                            }
                        }
                    }
                    if (transactionCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GlassSurface,
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$transactionCount txns",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate200
                                )
                            }
                        }
                    }
                }
            }
            MiniSparkline(values = netHistory)
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
                    modifier = Modifier.weight(1f),
                    trailing = if (netResult > 0) {
                        {
                            val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
                            val sparkleAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "sparkle_alpha"
                            )
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Emerald400.copy(alpha = sparkleAlpha)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    GlassPanel(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(value, style = MaterialTheme.typography.titleMedium)
                trailing?.invoke()
            }
        }
    }
}

private fun resolveStatIcon(label: String): ImageVector {
    val n = label.lowercase()
    return when {
        listOf("net", "saved", "income", "inflow").any { n.contains(it) } -> Icons.Default.AccountBalanceWallet
        listOf("spend", "expense", "outflow").any { n.contains(it) } -> Icons.Default.CreditCard
        listOf("target", "goal", "budget", "track").any { n.contains(it) } -> Icons.Default.Layers
        else -> Icons.Default.TrendingUp
    }
}

private fun variantChipColor(variant: String?): Color = when (variant) {
    "positive" -> Emerald400
    "negative" -> Rose400
    else -> Slate700
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

                val chevronRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    animationSpec = tween(200),
                    label = "chevron_${stat.label}"
                )

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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = variantChipColor(stat.variant).copy(alpha = 0.2f)
                                ) {
                                    Icon(
                                        imageVector = resolveStatIcon(stat.label),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(16.dp),
                                        tint = variantChipColor(stat.variant)
                                    )
                                }
                                Text(
                                    text = stat.label,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = formatCurrency(stat.amount, currencyCode),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = variantColor
                                )
                                if (hasBreakdown) {
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        modifier = Modifier
                                            .size(20.dp)
                                            .rotate(chevronRotation),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
private fun ExchangeRateSection(
    updatedAt: String,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val formatted = runCatching {
        val parsed = OffsetDateTime.parse(updatedAt)
        parsed.format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"))
    }.getOrNull() ?: updatedAt

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Exchange rate data", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Last update: $formatted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = SkyBlue
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh exchange rates",
                        tint = Color.White
                    )
                }
            }
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

@Composable
private fun AccountSelectorRow(
    accountNames: List<Pair<String, String>>,
    selectedAccountId: String,
    onSelect: (String) -> Unit
) {
    if (accountNames.isEmpty()) {
        Text(
            text = "No accounts available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        accountNames.forEach { (id, name) ->
            FilterChip(
                selected = selectedAccountId == id,
                onClick = { onSelect(id) },
                label = { Text(name) }
            )
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

/**
 * Compute consecutive-day streak ending today (or yesterday) from transaction dates.
 * Parses date strings (ISO format) to LocalDate, deduplicates by day, and counts
 * how many consecutive days going backwards have at least one transaction.
 */
private fun computeStreak(transactions: List<DashboardRecentTransactionDto>): Int {
    if (transactions.isEmpty()) return 0

    val dates = transactions.mapNotNull { tx ->
        runCatching {
            LocalDate.parse(tx.date.take(10))
        }.getOrNull()
    }.toSortedSet(compareByDescending { it })

    if (dates.isEmpty()) return 0

    val today = LocalDate.now()
    // Start from today or yesterday (allow a grace period for timezone differences)
    val startDate = if (dates.first() == today) today else if (dates.first() == today.minusDays(1)) today.minusDays(1) else return 0

    var streak = 0
    var current = startDate
    while (current in dates) {
        streak++
        current = current.minusDays(1)
    }
    return streak
}
