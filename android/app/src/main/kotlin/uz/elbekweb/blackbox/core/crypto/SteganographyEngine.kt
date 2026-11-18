package uz.elbekweb.blackbox.core.crypto

import android.graphics.Bitmap
import android.graphics.Color
import java.nio.ByteBuffer

/**
 * Kiber Steganografiya Dvigateli (LSB - Least Significant Bit).
 * Maxfiy ma'lumotlarni yoki shifrlangan fayllarni oddiy rasm piksellari
 * (RGB kanallarining eng quyi bitlari) ichiga inson ko'zi ilg'ay olmaydigan
 * tarzda yashiradi va qayta ajratib oladi.
 */
object SteganographyEngine {

    /**
     * Maxfiy baytlarni rasm piksellari ichiga joylash
     */
    fun embedSecret(coverBitmap: Bitmap, secretData: ByteArray): Bitmap {
        val totalPayloadSize = 4 + secretData.size // 4 bayt uzunlik sarlavhasi + ma'lumot
        val totalBitsNeeded = totalPayloadSize * 8

        val width = coverBitmap.width
        val height = coverBitmap.height
        val totalAvailableBits = width * height * 3 // R, G, B kanallarining LSB lari

        require(totalBitsNeeded <= totalAvailableBits) {
            "Rasm hajmi juda kichik! Kerakli bitlar: $totalBitsNeeded, Mavjud bitlar: $totalAvailableBits"
        }

        val outputBitmap = coverBitmap.copy(Bitmap.Config.ARGB_8888, true)

        // Sarlavha (4 bayt uzunlik) + Asl ma'lumot
        val payload = ByteBuffer.allocate(totalPayloadSize).apply {
            putInt(secretData.size)
            put(secretData)
        }.array()

        var bitIndex = 0
        var currentByteIndex = 0
        var currentByte = payload[0].toInt() and 0xFF

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (currentByteIndex >= payload.size) {
                    return outputBitmap // Barcha ma'lumot yozib bo'lindi
                }

                val pixel = outputBitmap.getPixel(x, y)
                var a = Color.alpha(pixel)
                var r = Color.red(pixel)
                var g = Color.green(pixel)
                var b = Color.blue(pixel)

                // R kanalining LSB'sini o'zgartirish
                if (currentByteIndex < payload.size) {
                    val bit = (currentByte shr (7 - (bitIndex % 8))) and 1
                    r = (r and 0xFE) or bit
                    bitIndex++
                    if (bitIndex % 8 == 0) {
                        currentByteIndex++
                        if (currentByteIndex < payload.size) currentByte = payload[currentByteIndex].toInt() and 0xFF
                    }
                }

                // G kanalining LSB'sini o'zgartirish
                if (currentByteIndex < payload.size) {
                    val bit = (currentByte shr (7 - (bitIndex % 8))) and 1
                    g = (g and 0xFE) or bit
                    bitIndex++
                    if (bitIndex % 8 == 0) {
                        currentByteIndex++
                        if (currentByteIndex < payload.size) currentByte = payload[currentByteIndex].toInt() and 0xFF
                    }
                }

                // B kanalining LSB'sini o'zgartirish
                if (currentByteIndex < payload.size) {
                    val bit = (currentByte shr (7 - (bitIndex % 8))) and 1
                    b = (b and 0xFE) or bit
                    bitIndex++
                    if (bitIndex % 8 == 0) {
                        currentByteIndex++
                        if (currentByteIndex < payload.size) currentByte = payload[currentByteIndex].toInt() and 0xFF
                    }
                }

                outputBitmap.setPixel(x, y, Color.argb(a, r, g, b))
            }
        }

        return outputBitmap
    }

    /**
     * Stego-rasm ichidan maxfiy ma'lumotni ajratib olish
     */
    fun extractSecret(stegoBitmap: Bitmap): ByteArray {
        val width = stegoBitmap.width
        val height = stegoBitmap.height

        var bitIndex = 0
        var currentByte = 0
        val extractedHeaderBytes = ByteArray(4)
        var headerBytesRead = 0

        var payloadSize = -1
        var payloadData: ByteArray? = null
        var payloadBytesRead = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = stegoBitmap.getPixel(x, y)
                val channels = listOf(Color.red(pixel), Color.green(pixel), Color.blue(pixel))

                for (channel in channels) {
                    val bit = channel and 1
                    currentByte = (currentByte shl 1) or bit
                    bitIndex++

                    if (bitIndex % 8 == 0) {
                        val completeByte = currentByte.toByte()
                        currentByte = 0

                        if (headerBytesRead < 4) {
                            extractedHeaderBytes[headerBytesRead] = completeByte
                            headerBytesRead++
                            if (headerBytesRead == 4) {
                                payloadSize = ByteBuffer.wrap(extractedHeaderBytes).int
                                if (payloadSize <= 0 || payloadSize > (width * height)) {
                                    throw IllegalStateException("Ushbu rasmda yashirin BlackBox ma'lumoti topilmadi!")
                                }
                                payloadData = ByteArray(payloadSize)
                            }
                        } else {
                            payloadData?.let { data ->
                                if (payloadBytesRead < payloadSize) {
                                    data[payloadBytesRead] = completeByte
                                    payloadBytesRead++
                                    if (payloadBytesRead == payloadSize) {
                                        return data
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return payloadData ?: throw IllegalStateException("Steganografiya ma'lumoti to'liq o'qilmadi")
    }
}
