package com.example.data

import java.util.Locale

data class ParsedQuestionItem(
    val id: String,
    val detectedNumber: Int,
    val question: TestQuestion,
    val rawAnswerText: String = "",
    val validationErrors: List<String> = emptyList(),
    val isDuplicate: Boolean = false
) {
    val isValid: Boolean
        get() = validationErrors.isEmpty() && !isDuplicate
}

data class BulkParseResult(
    val items: List<ParsedQuestionItem>,
    val maxLimit: Int,
    val truncatedCount: Int = 0
) {
    val totalCount: Int get() = items.size
    val validCount: Int get() = items.count { it.isValid }
    val invalidCount: Int get() = items.count { !it.isValid }
    val duplicateCount: Int get() = items.count { it.isDuplicate }
    val validQuestions: List<TestQuestion> get() = items.filter { it.isValid }.map { it.question }
}

object BulkQuestionParser {
    const val MAX_CHAPTER_BULK_QUESTIONS = 50
    const val MAX_WEEKLY_MEGA_QUESTIONS = 200

    private val QUESTION_START_REGEX = Regex(
        """^\s*(?:Q(?:uestion)?\.?\s*|प्रश्न\s*)?(\d{1,3})\s*[.):\-]\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val OPTION_REGEX = Regex(
        """^\s*\(?([A-Da-d])\)?\s*[.):\-]\s*(.*)$"""
    )
    private val ANSWER_REGEX = Regex(
        """^\s*(?:ANSWER|ANS|CORRECT\s*ANSWER|उत्तर)\s*[:=\-.]?\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val EXPLANATION_REGEX = Regex(
        """^\s*(?:EXPLANATION|EXP|SOLUTION|व्याख्या)\s*[:=\-.]\s*(.*)$""",
        RegexOption.IGNORE_CASE
    )

    fun normalizeQuestionKey(text: String): String {
        return text
            .lowercase(Locale.ROOT)
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trimEnd('?', '.', '।', ':', ';')
    }

    fun parse(rawText: String, maxLimit: Int): BulkParseResult {
        if (rawText.isBlank()) {
            return BulkParseResult(items = emptyList(), maxLimit = maxLimit, truncatedCount = 0)
        }

        val lines = rawText.replace("\r\n", "\n").replace("\r", "\n").lines()
        val blocks = mutableListOf<RawBlock>()
        var current: RawBlock? = null
        var activeSection = Section.NONE

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            // Check Answer line first (so "ANSWER: B" never matches option or question)
            val ansMatch = ANSWER_REGEX.matchEntire(trimmed)
            if (ansMatch != null && current != null) {
                current.rawAnswer = ansMatch.groupValues[1].trim()
                activeSection = Section.ANSWER
                continue
            }

            // Check Explanation line
            val expMatch = EXPLANATION_REGEX.matchEntire(trimmed)
            if (expMatch != null && current != null) {
                val content = expMatch.groupValues[1].trim()
                if (content.isNotEmpty()) {
                    current.explanation.appendLine(content)
                }
                activeSection = Section.EXPLANATION
                continue
            }

            // Check Option A/B/C/D line
            val optMatch = OPTION_REGEX.matchEntire(trimmed)
            if (optMatch != null && current != null) {
                val letter = optMatch.groupValues[1].uppercase(Locale.US)
                val optText = optMatch.groupValues[2].trim()
                when (letter) {
                    "A" -> {
                        current.optionA = appendOptionText(current.optionA, optText)
                        activeSection = Section.OPT_A
                    }
                    "B" -> {
                        current.optionB = appendOptionText(current.optionB, optText)
                        activeSection = Section.OPT_B
                    }
                    "C" -> {
                        current.optionC = appendOptionText(current.optionC, optText)
                        activeSection = Section.OPT_C
                    }
                    "D" -> {
                        current.optionD = appendOptionText(current.optionD, optText)
                        activeSection = Section.OPT_D
                    }
                }
                continue
            }

            // Check Question Start line (e.g., "Q1. ...", "1. ...", "Q 12) ...")
            val qMatch = QUESTION_START_REGEX.matchEntire(trimmed)
            if (qMatch != null) {
                val detectedNum = qMatch.groupValues[1].toIntOrNull() ?: (blocks.size + 1)
                val qText = qMatch.groupValues[2].trim()
                val newBlock = RawBlock(detectedNumber = detectedNum)
                newBlock.questionText.append(qText)
                blocks.add(newBlock)
                current = newBlock
                activeSection = Section.QUESTION
                continue
            }

            // Continuation line for the current section
            if (current != null) {
                when (activeSection) {
                    Section.QUESTION -> {
                        if (current.questionText.isNotEmpty()) current.questionText.append(" ")
                        current.questionText.append(trimmed)
                    }
                    Section.OPT_A -> current.optionA = appendOptionText(current.optionA, trimmed)
                    Section.OPT_B -> current.optionB = appendOptionText(current.optionB, trimmed)
                    Section.OPT_C -> current.optionC = appendOptionText(current.optionC, trimmed)
                    Section.OPT_D -> current.optionD = appendOptionText(current.optionD, trimmed)
                    Section.EXPLANATION -> current.explanation.appendLine(trimmed)
                    Section.ANSWER, Section.NONE -> {
                        // Start a new implicit question if an unnumbered block appears after an ANSWER
                        if (activeSection == Section.ANSWER) {
                            val nextBlock = RawBlock(detectedNumber = blocks.size + 1)
                            nextBlock.questionText.append(trimmed)
                            blocks.add(nextBlock)
                            current = nextBlock
                            activeSection = Section.QUESTION
                        }
                    }
                }
            } else {
                // First block didn't start with Q1. — capture it gracefully so admin sees validation feedback
                val firstBlock = RawBlock(detectedNumber = 1)
                firstBlock.questionText.append(trimmed)
                blocks.add(firstBlock)
                current = firstBlock
                activeSection = Section.QUESTION
            }
        }

        val truncatedCount = (blocks.size - maxLimit).coerceAtLeast(0)
        val boundedBlocks = blocks.take(maxLimit)

        val rawItems = boundedBlocks.mapIndexed { idx, block ->
            val parsedIndex = extractAnswerIndex(block.rawAnswer)
            ParsedQuestionItem(
                id = "q_${idx + 1}_${block.detectedNumber}_${System.nanoTime()}",
                detectedNumber = block.detectedNumber,
                question = TestQuestion(
                    questionText = block.questionText.toString().trim(),
                    optionA = block.optionA.trim(),
                    optionB = block.optionB.trim(),
                    optionC = block.optionC.trim(),
                    optionD = block.optionD.trim(),
                    correctIndex = parsedIndex ?: -1,
                    explanation = block.explanation.toString().trim()
                ),
                rawAnswerText = block.rawAnswer.trim()
            )
        }

        return revalidateItems(rawItems, maxLimit, truncatedCount)
    }

    fun revalidateItems(
        items: List<ParsedQuestionItem>,
        maxLimit: Int,
        truncatedCount: Int = 0
    ): BulkParseResult {
        val seenNormalized = HashMap<String, Int>()
        val validated = items.mapIndexed { idx, item ->
            val q = item.question
            val errors = mutableListOf<String>()
            var duplicate = false

            if (q.questionText.isBlank()) {
                errors.add("Question text is missing")
            } else if (q.questionText.length > 600) {
                errors.add("Question text exceeds 600 characters")
            }

            if (q.optionA.isBlank()) errors.add("Option A is missing")
            if (q.optionB.isBlank()) errors.add("Option B is missing")
            if (q.optionC.isBlank()) errors.add("Option C is missing")
            if (q.optionD.isBlank()) errors.add("Option D is missing")

            if (q.correctIndex !in 0..3) {
                val hint = if (item.rawAnswerText.isNotBlank()) " ('${item.rawAnswerText}')" else ""
                errors.add("Invalid or missing ANSWER key$hint — must be A, B, C, or D")
            }

            val norm = normalizeQuestionKey(q.questionText)
            if (norm.isNotEmpty()) {
                val firstSeenPos = seenNormalized[norm]
                if (firstSeenPos != null) {
                    duplicate = true
                    errors.add("Duplicate question (same as #${firstSeenPos + 1})")
                } else {
                    seenNormalized[norm] = idx
                }
            }

            item.copy(
                validationErrors = errors,
                isDuplicate = duplicate
            )
        }

        return BulkParseResult(
            items = validated,
            maxLimit = maxLimit,
            truncatedCount = truncatedCount
        )
    }

    fun extractAnswerIndex(rawAnswer: String): Int? {
        val clean = rawAnswer.trim()
        if (clean.isEmpty()) return null

        // Match leading A/B/C/D optionally wrapped in parens/brackets or followed by punctuation/space
        val letterMatch = Regex("""^\(?\[?([A-Da-d])\]?\)?(?:\s*[.):\-].*|\s+.*|$)""").matchEntire(clean)
        if (letterMatch != null) {
            return when (letterMatch.groupValues[1].uppercase(Locale.US)) {
                "A" -> 0
                "B" -> 1
                "C" -> 2
                "D" -> 3
                else -> null
            }
        }

        // Also accept "Option B" / "Opt B"
        val optWordMatch = Regex("""^(?:OPTION|OPT)\s*[:\-]?\s*\(?([A-Da-d])\)?.*$""", RegexOption.IGNORE_CASE)
            .matchEntire(clean)
        if (optWordMatch != null) {
            return when (optWordMatch.groupValues[1].uppercase(Locale.US)) {
                "A" -> 0
                "B" -> 1
                "C" -> 2
                "D" -> 3
                else -> null
            }
        }

        return null
    }

    private fun appendOptionText(existing: String, addition: String): String {
        if (addition.isEmpty()) return existing
        return if (existing.isEmpty()) addition else "$existing $addition"
    }

    private enum class Section {
        NONE, QUESTION, OPT_A, OPT_B, OPT_C, OPT_D, ANSWER, EXPLANATION
    }

    private class RawBlock(val detectedNumber: Int) {
        val questionText = StringBuilder()
        var optionA: String = ""
        var optionB: String = ""
        var optionC: String = ""
        var optionD: String = ""
        var rawAnswer: String = ""
        val explanation = StringBuilder()
    }
}
