package com.gh.toy.budget

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

@Controller
class BudgetController(
    private val budgetService: BudgetService
) {

    private val quickAmounts = listOf(5, 10, 15, 20, 50)

    @GetMapping("/")
    fun dashboard(model: Model): String {
        val currentMonth = YearMonth.now()
        model.addAttribute("categories", budgetService.allCategories())
        model.addAttribute("summary", budgetService.totalsForMonth(currentMonth))
        model.addAttribute("totalSpent", budgetService.totalSpent(currentMonth))
        model.addAttribute("monthLabel", budgetService.monthLabel(currentMonth))
        return "index"
    }

    @GetMapping("/summary")
    fun summary(model: Model): String {
        val currentMonth = YearMonth.now()
        model.addAttribute("summary", budgetService.totalsForMonth(currentMonth))
        model.addAttribute("totalSpent", budgetService.totalSpent(currentMonth))
        model.addAttribute("monthLabel", budgetService.monthLabel(currentMonth))
        return "index :: summary"
    }

    @GetMapping("/category/{id}/form")
    fun categoryForm(@PathVariable id: String, model: Model): String {
        val category = budgetService.findCategory(id) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        model.addAttribute("category", category)
        model.addAttribute("amountOptions", quickAmounts)
        model.addAttribute("message", null)
        return "index :: entryForm"
    }

    @PostMapping("/expense")
    fun addExpense(
        @RequestParam categoryId: String,
        @RequestParam amount: BigDecimal,
        response: HttpServletResponse,
        model: Model
    ): String {
        val category = budgetService.findCategory(categoryId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            model.addAttribute("category", category)
            model.addAttribute("amountOptions", quickAmounts)
            model.addAttribute("message", "Enter an amount above 0.")
            return "index :: entryForm"
        }
        budgetService.addExpense(category.id, amount)
        response.setHeader("HX-Trigger", """{"expenseAdded": {"category":"$categoryId"}}""")
        model.addAttribute("category", category)
        model.addAttribute("amountOptions", quickAmounts)
        model.addAttribute("message", "Added $${amount.setScale(2, RoundingMode.HALF_UP)} to ${category.name}")
        return "index :: entryForm"
    }
}
