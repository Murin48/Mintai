package com.example.data.analysis

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.Inflater

object PdfTextExtractor {

    data class ExtractedPdfResult(
        val text: String,
        val charCount: Int,
        val detectedPagesEstimate: Int,
        val isSuccessful: Boolean,
        val message: String
    )

    fun extractTextFromUri(context: Context, uri: Uri): ExtractedPdfResult {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return ExtractedPdfResult("", 0, 0, false, "ไม่สามารถเปิดไฟล์ PDF ได้")
            }

            val buffer = ByteArrayOutputStream()
            val data = ByteArray(8192)
            var nRead: Int
            while (inputStream.read(data, 0, data.size).also { nRead = it } != -1) {
                buffer.write(data, 0, nRead)
            }
            buffer.flush()
            val pdfBytes = buffer.toByteArray()
            inputStream.close()

            extractFromBytes(pdfBytes)
        } catch (e: Exception) {
            ExtractedPdfResult("", 0, 0, false, "เกิดข้อผิดพลาดในการอ่าน PDF: ${e.localizedMessage}")
        }
    }

    fun extractFromBytes(bytes: ByteArray): ExtractedPdfResult {
        try {
            val contentStr = String(bytes, Charsets.ISO_8859_1)
            val extractedTextBuilder = StringBuilder()

            // Count /Type /Page occurrences as rough page count
            val pageCount = Regex("/Type\\s*/Page[^s]").findAll(contentStr).count().coerceAtLeast(1)

            // 1. Scan for text streams between 'stream' and 'endstream'
            val streamRegex = Regex("stream[\\r\\n]+([\\s\\S]*?)[\\r\\n]+endstream")
            val matches = streamRegex.findAll(contentStr)

            for (match in matches) {
                val streamContent = match.groupValues[1]
                val decompressed = tryDecompress(streamContent.toByteArray(Charsets.ISO_8859_1))
                val targetText = decompressed ?: streamContent

                // Parse BT ... ET blocks
                val textFromBlocks = extractTextFromPdfBlock(targetText)
                if (textFromBlocks.isNotBlank()) {
                    extractedTextBuilder.append(textFromBlocks).append("\n")
                }
            }

            // 2. If stream parsing yielded very little text, fallback to pattern search in whole file
            var resultText = extractedTextBuilder.toString().trim()
            if (resultText.length < 50) {
                val fallbackMatches = extractTextFromPdfBlock(contentStr)
                if (fallbackMatches.length > resultText.length) {
                    resultText = fallbackMatches
                }
            }

            // 3. If still short, extract readable Thai/English sequences
            if (resultText.length < 30) {
                val readable = extractReadableStrings(bytes)
                if (readable.isNotBlank()) {
                    resultText = readable
                }
            }

            // Clean up and format
            val cleaned = cleanExtractedText(resultText)
            return if (cleaned.isNotBlank()) {
                ExtractedPdfResult(
                    text = cleaned,
                    charCount = cleaned.length,
                    detectedPagesEstimate = pageCount,
                    isSuccessful = true,
                    message = "อ่านข้อความสำเร็จ ($pageCount หน้า)"
                )
            } else {
                ExtractedPdfResult(
                    text = "",
                    charCount = 0,
                    detectedPagesEstimate = pageCount,
                    isSuccessful = false,
                    message = "ไฟล์ PDF นี้อาจเป็นรูปภาพสแกนล้วน (Scanned Document) หรือมีการเข้ารหัส แนะนำให้พิมพ์หรือวางข้อความสัญญาโดยตรง"
                )
            }
        } catch (e: Exception) {
            return ExtractedPdfResult("", 0, 0, false, "อ่านไฟล์ไม่สำเร็จ: ${e.message}")
        }
    }

    private fun tryDecompress(data: ByteArray): String? {
        return try {
            val inflater = Inflater(false)
            inflater.setInput(data)
            val outputStream = ByteArrayOutputStream(data.size * 2)
            val buffer = ByteArray(4096)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && inflater.needsInput()) break
                outputStream.write(buffer, 0, count)
            }
            inflater.end()
            val decompressedBytes = outputStream.toByteArray()

            // Try UTF-8 first, then Windows-874 / ISO-8859-1
            val utf8 = String(decompressedBytes, Charsets.UTF_8)
            if (utf8.any { it.code in 0x0E00..0x0E7F }) {
                utf8
            } else {
                try {
                    String(decompressedBytes, Charset.forName("TIS-620"))
                } catch (_: Exception) {
                    String(decompressedBytes, Charsets.ISO_8859_1)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractTextFromPdfBlock(stream: String): String {
        val sb = StringBuilder()

        // Match BT ... ET blocks
        val btEtRegex = Regex("BT([\\s\\S]*?)ET")
        val blocks = btEtRegex.findAll(stream)

        for (b in blocks) {
            val block = b.groupValues[1]

            // Match literal strings: ( ... ) Tj or \'
            val tjRegex = Regex("\\((.*?)\\)\\s*(?:Tj|\\')")
            for (m in tjRegex.findAll(block)) {
                sb.append(decodePdfString(m.groupValues[1])).append(" ")
            }

            // Match array strings: [ (...) -10 (...) ] TJ
            val arrayRegex = Regex("\\[([^\\]]*)\\]\\s*TJ")
            for (m in arrayRegex.findAll(block)) {
                val arrayContent = m.groupValues[1]
                val subStrings = Regex("\\((.*?)\\)").findAll(arrayContent)
                for (sub in subStrings) {
                    sb.append(decodePdfString(sub.groupValues[1]))
                }
                sb.append(" ")
            }

            // Match Hex strings: <0E01...> Tj
            val hexRegex = Regex("<([0-9a-fA-F]+)>\\s*Tj")
            for (m in hexRegex.findAll(block)) {
                sb.append(decodeHexPdfString(m.groupValues[1])).append(" ")
            }

            sb.append("\n")
        }

        return sb.toString()
    }

    private fun decodePdfString(raw: String): String {
        return raw.replace("\\\\", "\\")
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\n", "\n")
            .replace("\\r", "")
            .replace("\\t", " ")
    }

    private fun decodeHexPdfString(hex: String): String {
        val cleanHex = hex.trim()
        if (cleanHex.length % 2 != 0) return ""
        val bytes = ByteArray(cleanHex.length / 2)
        for (i in bytes.indices) {
            val index = i * 2
            bytes[i] = cleanHex.substring(index, index + 2).toInt(16).toByte()
        }
        return try {
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractReadableStrings(bytes: ByteArray): String {
        val sb = StringBuilder()
        val current = StringBuilder()
        for (b in bytes) {
            val char = b.toInt().toChar()
            if (char.isLetterOrDigit() || char.isWhitespace() || char in ".,:;()/-_\"'%") {
                current.append(char)
            } else {
                if (current.length >= 4) {
                    sb.append(current.toString()).append(" ")
                }
                current.setLength(0)
            }
        }
        if (current.length >= 4) {
            sb.append(current.toString())
        }
        return sb.toString()
    }

    private fun cleanExtractedText(raw: String): String {
        return raw.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .replace(Regex("[ \\t]+"), " ")
    }
}
