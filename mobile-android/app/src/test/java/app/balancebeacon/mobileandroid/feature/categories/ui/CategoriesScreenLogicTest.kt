package app.balancebeacon.mobileandroid.feature.categories.ui

import app.balancebeacon.mobileandroid.feature.categories.model.CategoryDto
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoriesScreenLogicTest {
    @Test
    fun filterCategories_matchesNameTypeAndColorCaseInsensitively() {
        val items = listOf(
            CategoryDto(
                id = "cat_food",
                name = "Food & Dining",
                type = "EXPENSE",
                color = "#FFAA00"
            ),
            CategoryDto(
                id = "cat_salary",
                name = "Salary",
                type = "INCOME",
                color = "#00AAFF"
            )
        )

        assertEquals(listOf("cat_food"), filterCategories(items, "dining").map { it.id })
        assertEquals(listOf("cat_salary"), filterCategories(items, "income").map { it.id })
        assertEquals(listOf("cat_food"), filterCategories(items, "ffaa").map { it.id })
    }

    @Test
    fun visibleCountHelpers_expandByDefaultPageSizeAndClampRemaining() {
        assertEquals(24, nextVisibleCategoryCount(DEFAULT_VISIBLE_COUNT))
        assertEquals(3, remainingCategoryCount(filteredCount = 15, visibleCount = DEFAULT_VISIBLE_COUNT))
        assertEquals(0, remainingCategoryCount(filteredCount = 5, visibleCount = DEFAULT_VISIBLE_COUNT))
    }
}
