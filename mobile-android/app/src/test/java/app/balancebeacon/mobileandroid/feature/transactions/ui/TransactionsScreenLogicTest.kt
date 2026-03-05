package app.balancebeacon.mobileandroid.feature.transactions.ui

import app.balancebeacon.mobileandroid.feature.transactions.model.TransactionDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionsScreenLogicTest {
    @Test
    fun filterTransactions_matchesQueryAcrossDescriptionCategoryAndDate() {
        val items = listOf(
            TransactionDto(
                id = "tx_1",
                amount = "14.50",
                type = "EXPENSE",
                description = "Coffee beans",
                categoryId = "groceries",
                accountId = "main-checking",
                date = "2026-03-05"
            ),
            TransactionDto(
                id = "tx_2",
                amount = "1200.00",
                type = "INCOME",
                description = "Salary",
                categoryId = "payroll",
                accountId = "savings",
                date = "2026-03-01"
            )
        )

        assertEquals(
            listOf("tx_1"),
            filterTransactions(
                items = items,
                searchQuery = "coffee",
                listTypeFilter = "ALL",
                listAccountFilter = ""
            ).map { it.id }
        )
        assertEquals(
            listOf("tx_1"),
            filterTransactions(
                items = items,
                searchQuery = "grocer",
                listTypeFilter = "ALL",
                listAccountFilter = ""
            ).map { it.id }
        )
        assertEquals(
            listOf("tx_2"),
            filterTransactions(
                items = items,
                searchQuery = "2026-03-01",
                listTypeFilter = "ALL",
                listAccountFilter = ""
            ).map { it.id }
        )
    }

    @Test
    fun filterTransactions_appliesTypeAndAccountFiltersCaseInsensitively() {
        val items = listOf(
            TransactionDto(
                id = "expense",
                amount = "20.00",
                type = "expense",
                accountId = "Primary-Checking",
                date = "2026-03-02"
            ),
            TransactionDto(
                id = "income",
                amount = "90.00",
                type = "INCOME",
                accountId = "Brokerage",
                date = "2026-03-03"
            )
        )

        val result = filterTransactions(
            items = items,
            searchQuery = "",
            listTypeFilter = "expense",
            listAccountFilter = "checking"
        )

        assertEquals(listOf("expense"), result.map { it.id })
    }

    @Test
    fun isShareEligible_onlyAllowsExpenseTransactionsWhenShareHandlerExists() {
        assertTrue(isShareEligible(transactionType = "EXPENSE", hasShareHandler = true))
        assertTrue(isShareEligible(transactionType = "expense", hasShareHandler = true))
        assertFalse(isShareEligible(transactionType = "INCOME", hasShareHandler = true))
        assertFalse(isShareEligible(transactionType = "EXPENSE", hasShareHandler = false))
    }
}
