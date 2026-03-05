package app.balancebeacon.mobileandroid.feature.transactions.ui

import app.balancebeacon.mobileandroid.feature.transactions.model.TransactionDto

internal fun filterTransactions(
    items: List<TransactionDto>,
    searchQuery: String,
    listTypeFilter: String,
    listAccountFilter: String
): List<TransactionDto> {
    val normalizedQuery = searchQuery.trim().lowercase()
    val normalizedTypeFilter = listTypeFilter.trim().uppercase()
    val normalizedAccountFilter = listAccountFilter.trim().lowercase()

    return items.filter { transaction ->
        val matchesType = normalizedTypeFilter.isBlank() ||
            normalizedTypeFilter == "ALL" ||
            transaction.type.equals(normalizedTypeFilter, ignoreCase = true)
        val matchesAccount = normalizedAccountFilter.isBlank() ||
            transaction.accountId.lowercase().contains(normalizedAccountFilter)
        val matchesQuery = normalizedQuery.isBlank() ||
            transactionSearchableText(transaction).contains(normalizedQuery)
        matchesType && matchesAccount && matchesQuery
    }
}

internal fun isShareEligible(
    transactionType: String,
    hasShareHandler: Boolean
): Boolean {
    return hasShareHandler && transactionType.equals("EXPENSE", ignoreCase = true)
}

private fun transactionSearchableText(transaction: TransactionDto): String {
    return buildString {
        append(transaction.type)
        append(' ')
        append(transaction.amount)
        append(' ')
        append(transaction.accountId)
        append(' ')
        append(transaction.date)
        transaction.description?.let {
            append(' ')
            append(it)
        }
        transaction.categoryId?.let {
            append(' ')
            append(it)
        }
    }.lowercase()
}
