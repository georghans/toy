package com.gh.toy.budget

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class Category(val id: String, val name: String, val icon: String)

data class Expense(
    val categoryId: String,
    val amount: BigDecimal,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class CategoryTotal(val category: Category, val total: BigDecimal)

@Service
class BudgetService {
    private val categories = listOf(
        Category("eat-out", "Eat Outside", "\uD83C\uDF74"),
        Category("coffee", "Coffee & Drinks", "\u2615\uFE0F"),
        Category("groceries", "Groceries", "\uD83C\uDF45"),
        Category("car", "Car & Gas", "\uD83D\uDE97"),
        Category("fun", "Fun", "\u2728"),
        Category("fitness", "Fitness", "\uD83C\uDFCB\uFE0F"),
        Category("home", "Home", "\uD83C\uDFE0"),
        Category("other", "Other", "\u26A1\uFE0F")
    )

    private val expenses = ConcurrentHashMap<String, MutableList<Expense>>()

    fun allCategories(): List<Category> = categories

    fun findCategory(id: String): Category? = categories.firstOrNull { it.id == id }

    fun addExpense(categoryId: String, amount: BigDecimal): Expense {
        require(amount > BigDecimal.ZERO) { "Amount must be positive" }
        val category = findCategory(categoryId) ?: throw IllegalArgumentException("Unknown category: $categoryId")
        val expense = Expense(category.id, amount.setScale(2, RoundingMode.HALF_UP))
        expenses.computeIfAbsent(category.id) { mutableListOf() }.add(expense)
        return expense
    }

    fun totalsForMonth(month: YearMonth = YearMonth.now()): List<CategoryTotal> {
        val start = month.atDay(1)
        val end = month.atEndOfMonth()

        return categories.map { category ->
            val total = expenses[category.id]
                ?.asSequence()
                ?.filter { it.createdAt.toLocalDate().isBetween(start, end) }
                ?.fold(BigDecimal.ZERO) { acc, e -> acc + e.amount }
                ?: BigDecimal.ZERO
            CategoryTotal(category, total)
        }.sortedByDescending { it.total }
    }

    fun totalSpent(month: YearMonth = YearMonth.now()): BigDecimal =
        totalsForMonth(month).fold(BigDecimal.ZERO) { acc, ct -> acc + ct.total }

    fun monthLabel(month: YearMonth = YearMonth.now()): String {
        val monthName = month.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.US)
        return "$monthName ${month.year}"
    }

    private fun LocalDate.isBetween(start: LocalDate, end: LocalDate): Boolean =
        !isBefore(start) && !isAfter(end)
}
