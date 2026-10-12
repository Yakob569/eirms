package com.eirspay.domain.usecase

data class PasswordRule(val label: String, val satisfied: Boolean)

data class PasswordCheck(val rules: List<PasswordRule>, val score: Int) {
    val strengthLabel: String get() = listOf("", "Weak", "Fair", "Good", "Strong")[score]
    val acceptable: Boolean get() = score >= 3 && rules.first().satisfied
}

/** Same rules the web console enforces: 8+ chars, mixed case, a number, a symbol; 3 of 4 is acceptable. */
object PasswordPolicy {
    fun evaluate(password: String): PasswordCheck {
        val rules = listOf(
            PasswordRule("8+ characters", password.length >= 8),
            PasswordRule("Upper & lowercase", password.any { it.isLowerCase() } && password.any { it.isUpperCase() }),
            PasswordRule("A number", password.any { it.isDigit() }),
            PasswordRule("A symbol", password.any { !it.isLetterOrDigit() }),
        )
        return PasswordCheck(rules, rules.count { it.satisfied })
    }
}
