package com.example.dreyassist.util

import java.util.Calendar
import java.util.Locale

enum class Category {
    TRANSAKSI,
    JURNAL,
    PENGINGAT
}

object VoiceParser {

    private val numberWords = mapOf(
        "satu" to 1, "dua" to 2, "tiga" to 3, "empat" to 4, "lima" to 5,
        "enam" to 6, "tujuh" to 7, "delapan" to 8, "sembilan" to 9, "sepuluh" to 10,
        "sebelas" to 11, "dua belas" to 12, "tiga belas" to 13, "empat belas" to 14,
        "lima belas" to 15, "enam belas" to 16, "tujuh belas" to 17, "delapan belas" to 18,
        "sembilan belas" to 19, "dua puluh" to 20, "seratus" to 100, "seribu" to 1000
    )

    private val monthNames = mapOf(
        "januari" to 0, "februari" to 1, "maret" to 2, "april" to 3,
        "mei" to 4, "juni" to 5, "juli" to 6, "agustus" to 7,
        "september" to 8, "oktober" to 9, "november" to 10, "desember" to 11
    )

    private val totalKeywords = listOf("total", "seharga", "senilai", "harga", "sebesar", "nominal")
    private val keteranganKeywords = listOf("rincian", "keterangan", "detail", "isi", "untuk", "yaitu", "berupa")

    private val journalKeywords = listOf(
        "hari ini mengerjakan", "hari ini aku mengerjakan", "hari ini kami mengerjakan",
        "hari ini saya mengerjakan", "hari ini melakukan", "hari ini aku melakukan",
        "hari ini kami melakukan", "hari ini saya melakukan"
    )

    private val reminderKeywords = listOf(
        "ingatkan aku", "ingatkan saya", "remind me", "pengingat"
    )

    fun parse(text: String): ParsedResult {
        val lowercasedText = text.lowercase(Locale.ROOT)
        
        // Check reminder first
        val isReminder = reminderKeywords.any { lowercasedText.startsWith(it) }
        if (isReminder) {
            return parseReminder(text, lowercasedText)
        }

        // Check journal
        val isJournal = journalKeywords.any { lowercasedText.startsWith(it) }
        if (isJournal) {
            return parseJournal(text, lowercasedText)
        }
        
        return parseTransaction(text, lowercasedText)
    }

    private fun parseReminder(originalText: String, lowercasedText: String): ParsedResult {
        var content = originalText
        
        // Remove reminder prefix
        for (keyword in reminderKeywords) {
            if (lowercasedText.startsWith(keyword)) {
                content = originalText.substring(keyword.length).trim()
                break
            }
        }

        // Parse date and time
        val calendar = Calendar.getInstance()
        var foundDate = false
        var foundTime = false

        // Parse "tanggal X bulan tahun"
        val dateRegex = Regex("""tanggal\s+(\d{1,2})\s+(\w+)(?:\s+(\d{4}))?""")
        val dateMatch = dateRegex.find(lowercasedText)
        if (dateMatch != null) {
            val day = dateMatch.groupValues[1].toIntOrNull() ?: 1
            val monthName = dateMatch.groupValues[2]
            val year = dateMatch.groupValues[3].toIntOrNull() ?: calendar.get(Calendar.YEAR)
            
            val month = monthNames[monthName] ?: calendar.get(Calendar.MONTH)
            
            calendar.set(Calendar.DAY_OF_MONTH, day)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.YEAR, year)
            foundDate = true
            
            // Remove date part from content
            content = content.replace(dateRegex, "").trim()
        }

        // Parse "besok"
        if (lowercasedText.contains("besok")) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            foundDate = true
            content = content.replace(Regex("""besok""", RegexOption.IGNORE_CASE), "").trim()
        }

        // Parse "lusa"
        if (lowercasedText.contains("lusa")) {
            calendar.add(Calendar.DAY_OF_MONTH, 2)
            foundDate = true
            content = content.replace(Regex("""lusa""", RegexOption.IGNORE_CASE), "").trim()
        }

        // Parse "jam X" - supports 12:52, 12.52, 12 52
        val timeRegex = Regex("""jam\s+(\d{1,2})(?:[:.\s](\d{1,2}))?(?:\s+(pagi|siang|sore|malam))?""")
        val timeMatch = timeRegex.find(lowercasedText)
        if (timeMatch != null) {
            var hour = timeMatch.groupValues[1].toIntOrNull() ?: 9
            val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
            val period = timeMatch.groupValues[3]
            
            // Adjust for pagi/siang/sore/malam
            when (period) {
                "pagi" -> if (hour == 12) hour = 0
                "siang" -> if (hour < 12) hour += 12
                "sore" -> if (hour < 12) hour += 12
                "malam" -> if (hour < 12) hour += 12
            }
            
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            foundTime = true
            
            // Remove time part from content
            content = content.replace(timeRegex, "").trim()
        }

        // Clean up content - remove "pada", extra spaces
        content = content
            .replace(Regex("""pada\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        // If no time specified, default to 9:00 AM
        if (!foundTime) {
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        return ParsedResult(
            category = Category.PENGINGAT,
            keperluan = content.capitalizeWords(),
            total = 0,
            keterangan = "",
            reminderTime = calendar.timeInMillis
        )
    }

    private fun parseJournal(originalText: String, lowercasedText: String): ParsedResult {
        var kegiatan = originalText
        
        for (keyword in journalKeywords) {
            if (lowercasedText.startsWith(keyword)) {
                kegiatan = originalText.substring(keyword.length).trim()
                break
            }
        }
        
        return ParsedResult(
            category = Category.JURNAL,
            keperluan = kegiatan.capitalizeWords(),
            total = 0,
            keterangan = "",
            reminderTime = 0
        )
    }

    private fun parseTransaction(originalText: String, lowercasedText: String): ParsedResult {
        var keperluan = ""
        var total = 0
        var keterangan = ""
        var transactionDate = System.currentTimeMillis()

        // Parse date first - "tanggal X bulan tahun"
        val dateRegex = Regex("""tanggal\s+(\d{1,2})\s+(\w+)(?:\s+(\d{4}))?""")
        val dateMatch = dateRegex.find(lowercasedText)
        var textWithoutDate = lowercasedText
        
        if (dateMatch != null) {
            val calendar = Calendar.getInstance()
            val day = dateMatch.groupValues[1].toIntOrNull() ?: 1
            val monthName = dateMatch.groupValues[2]
            val year = dateMatch.groupValues[3].toIntOrNull() ?: calendar.get(Calendar.YEAR)
            
            val month = monthNames[monthName] ?: calendar.get(Calendar.MONTH)
            
            calendar.set(Calendar.DAY_OF_MONTH, day)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.HOUR_OF_DAY, 12)
            calendar.set(Calendar.MINUTE, 0)
            
            transactionDate = calendar.timeInMillis
            textWithoutDate = lowercasedText.replace(dateRegex, "").trim()
        }

        var totalKeyword = ""
        var totalStartIndex = -1
        for (keyword in totalKeywords) {
            val index = textWithoutDate.indexOf(keyword)
            if (index != -1 && (totalStartIndex == -1 || index < totalStartIndex)) {
                totalStartIndex = index
                totalKeyword = keyword
            }
        }

        var keteranganKeyword = ""
        var keteranganStartIndex = -1
        for (keyword in keteranganKeywords) {
            val index = textWithoutDate.indexOf(keyword)
            if (index != -1) {
                if (totalStartIndex == -1 || index > totalStartIndex) {
                    if (keteranganStartIndex == -1 || index < keteranganStartIndex) {
                        keteranganStartIndex = index
                        keteranganKeyword = keyword
                    }
                }
            }
        }

        when {
            totalStartIndex != -1 && keteranganStartIndex != -1 -> {
                keperluan = textWithoutDate.substring(0, totalStartIndex).trim()
                val totalEndIndex = keteranganStartIndex
                val totalText = textWithoutDate.substring(totalStartIndex + totalKeyword.length, totalEndIndex).trim()
                total = extractNumber(totalText)
                keterangan = textWithoutDate.substring(keteranganStartIndex + keteranganKeyword.length).trim()
            }
            totalStartIndex != -1 -> {
                keperluan = textWithoutDate.substring(0, totalStartIndex).trim()
                val totalText = textWithoutDate.substring(totalStartIndex + totalKeyword.length).trim()
                total = extractNumber(totalText)
            }
            keteranganStartIndex != -1 -> {
                val beforeKeterangan = textWithoutDate.substring(0, keteranganStartIndex).trim()
                val numberMatch = extractNumberAndPosition(beforeKeterangan)
                if (numberMatch != null) {
                    keperluan = beforeKeterangan.substring(0, numberMatch.second).trim()
                    total = numberMatch.first
                } else {
                    keperluan = beforeKeterangan
                }
                keterangan = textWithoutDate.substring(keteranganStartIndex + keteranganKeyword.length).trim()
            }
            else -> {
                val numberMatch = extractNumberAndPosition(textWithoutDate)
                if (numberMatch != null) {
                    keperluan = textWithoutDate.substring(0, numberMatch.second).trim()
                    total = numberMatch.first
                    val afterNumber = textWithoutDate.substring(numberMatch.third).trim()
                    if (afterNumber.isNotEmpty()) {
                        keterangan = afterNumber
                    }
                } else {
                    keperluan = originalText
                }
            }
        }

        keperluan = keperluan.trim()
        if (keperluan.isEmpty() && total == 0) {
            keperluan = originalText
        }

        return ParsedResult(
            category = Category.TRANSAKSI,
            keperluan = keperluan.capitalizeWords(),
            total = total,
            keterangan = keterangan.capitalizeWords(),
            reminderTime = 0,
            transactionDate = transactionDate
        )
    }

    private fun extractNumber(text: String): Int {
        val numberWithSeparatorRegex = Regex("""(\d{1,3}(?:[.,]\d{3})+|\d+)\s*(ribu|rb|juta|jt)?""")
        val match = numberWithSeparatorRegex.find(text)
        
        if (match != null) {
            val numberPart = match.groupValues[1].replace(".", "").replace(",", "")
            val suffix = match.groupValues[2].lowercase()
            var number = numberPart.toIntOrNull() ?: 0
            
            when (suffix) {
                "ribu", "rb" -> number *= 1000
                "juta", "jt" -> number *= 1000000
            }
            
            return number
        }
        
        return convertNumberWordsToInt(text)
    }

    private fun extractNumberAndPosition(text: String): Triple<Int, Int, Int>? {
        val numberRegex = Regex("""(\d{1,3}(?:[.,]\d{3})+|\d+)\s*(ribu|rb|juta|jt)?""")
        val match = numberRegex.find(text)
        
        if (match != null) {
            val numberPart = match.groupValues[1].replace(".", "").replace(",", "")
            val suffix = match.groupValues[2].lowercase()
            var number = numberPart.toIntOrNull() ?: return null
            
            when (suffix) {
                "ribu", "rb" -> number *= 1000
                "juta", "jt" -> number *= 1000000
            }
            
            return Triple(number, match.range.first, match.range.last + 1)
        }
        return null
    }

    private fun convertNumberWordsToInt(text: String): Int {
        val words = text.split(" ", "-").filter { it.isNotBlank() }
        var total = 0
        var currentNumber = 0

        for (word in words) {
            val cleanedWord = word.replace(".", "").replace(",", "")
            val directNumber = cleanedWord.toIntOrNull()
            if (directNumber != null) {
                currentNumber += directNumber
                continue
            }

            val number = numberWords[word]
            if (number != null) {
                if (number >= 1000) {
                    currentNumber = if (currentNumber == 0) 1 else currentNumber
                    total += currentNumber * number
                    currentNumber = 0
                } else if (number >= 100) {
                    currentNumber = if (currentNumber == 0) 1 else currentNumber
                    currentNumber *= number
                } else {
                    currentNumber += number
                }
            } else when (word) {
                "ribu", "rb" -> {
                    currentNumber = if (currentNumber == 0) 1 else currentNumber
                    total += currentNumber * 1000
                    currentNumber = 0
                }
                "juta", "jt" -> {
                    currentNumber = if (currentNumber == 0) 1 else currentNumber
                    total += currentNumber * 1_000_000
                    currentNumber = 0
                }
                "puluh" -> currentNumber *= 10
                "ratus" -> currentNumber *= 100
            }
        }
        total += currentNumber
        return total
    }
    
    private fun String.capitalizeWords(): String = 
        split(' ').joinToString(" ") { 
            it.replaceFirstChar { char -> 
                if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString() 
            }
        }
}

data class ParsedResult(
    val category: Category,
    val keperluan: String,
    val total: Int,
    val keterangan: String,
    val reminderTime: Long = 0,
    val transactionDate: Long = System.currentTimeMillis()
)
