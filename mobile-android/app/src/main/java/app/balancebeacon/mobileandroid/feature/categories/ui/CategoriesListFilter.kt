package app.balancebeacon.mobileandroid.feature.categories.ui

import app.balancebeacon.mobileandroid.feature.categories.model.CategoryDto

internal const val DEFAULT_VISIBLE_COUNT = 12

internal fun filterCategories(
    items: List<CategoryDto>,
    searchQuery: String
): List<CategoryDto> {
    val normalizedQuery = searchQuery.trim().lowercase()
    if (normalizedQuery.isBlank()) {
        return items
    }

    return items.filter { category ->
        val haystack = buildString {
            append(category.name)
            append(' ')
            append(category.type)
            append(' ')
            append(category.color.orEmpty())
        }.lowercase()
        haystack.contains(normalizedQuery)
    }
}

internal fun nextVisibleCategoryCount(
    visibleCount: Int,
    increment: Int = DEFAULT_VISIBLE_COUNT
): Int {
    return visibleCount + increment
}

internal fun remainingCategoryCount(
    filteredCount: Int,
    visibleCount: Int
): Int {
    return (filteredCount - visibleCount).coerceAtLeast(0)
}
