package com.androidharness.app.professor

/** Conservative conflict hints; it never chooses a winner or silently changes stored rules. */
object ProfessorTrainingConflictDetector {
    private val positive = setOf("always", "do", "use", "include", "reply", "formal", "short", "বাংলায়", "বাংলা")
    private val negative = setOf("never", "don't", "do not", "avoid", "exclude", "no", "informal", "long", "ইংরেজিতে", "ইংরেজি")

    fun findConflicts(rules: List<String>): List<TrainingRuleConflict> {
        val result = mutableListOf<TrainingRuleConflict>()
        for (left in rules.indices) for (right in left + 1 until rules.size) {
            val a = normalize(rules[left])
            val b = normalize(rules[right])
            val shared = a.first intersect b.first
            if (shared.isNotEmpty() && a.second != b.second) {
                result += TrainingRuleConflict(rules[left], rules[right], shared)
            }
        }
        return result
    }

    private fun normalize(rule: String): Pair<Set<String>, Boolean> {
        val words = rule.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter(String::isNotBlank).toSet()
        val negated = words.any { it in negative } || "না" in words || rule.contains("করবে না") || rule.contains("করবেন না")
        return words - positive - negative to negated
    }
}

data class TrainingRuleConflict(
    val firstRule: String,
    val secondRule: String,
    val overlappingTerms: Set<String>,
)
