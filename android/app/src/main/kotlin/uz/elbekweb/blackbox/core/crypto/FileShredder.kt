package uz.elbekweb.blackbox.core.crypto

import java.io.File
import java.io.RandomAccessFile
import java.security.SecureRandom
import java.util.UUID

/**
 * Harbiy standartdagi File Shredder (DoD 5220.22-M usuli).
 * Oddiy o'chirish faqat fayl ko'rsatkichini (pointer) o'chiradi va ma'lumotlar tiklanishi mumkin.
 * File Shredder esa fayl joylashgan sektorlar ustidan 3 marta qatlamlab yangi baytlar yozadi:
 * 1-o'tish: Barcha baytlar ustidan 0x00 yozish.
 * 2-o'tish: Barcha baytlar ustidan 0xFF yozish.
 * 3-o'tish: Kriptografik tasodifiy (SecureRandom) baytlar yozish.
 * So'ngra fayl hajmi 0 ga tushirilib, nomi tasodifiy o'zgartiriladi va o'chiriladi.
 */
object FileShredder {

    private const val BUFFER_SIZE = 64 * 1024 // 64 KB bufer
    private val secureRandom = SecureRandom()

    /**
     * Bitta faylni qaytarib bo'lmas darajada yo'q qilish (Shred)
     */
    fun shredFile(file: File): Boolean {
        if (!file.exists() || !file.isFile) return false

        val fileLength = file.length()
        if (fileLength == 0L) {
            return safeDelete(file)
        }

        try {
            RandomAccessFile(file, "rws").use { raf ->
                // 1-bosqich: 0x00 bilan to'ldirish
                overwriteWithByte(raf, fileLength, 0x00.toByte())

                // 2-bosqich: 0xFF bilan to'ldirish
                overwriteWithByte(raf, fileLength, 0xFF.toByte())

                // 3-bosqich: Tasodifiy baytlar bilan to'ldirish
                overwriteWithRandom(raf, fileLength)

                // Fayl hajmini 0 ga tushirish
                raf.setLength(0)
                raf.fd.sync()
            }

            return safeDelete(file)
        } catch (e: Exception) {
            e.printStackTrace()
            return file.delete()
        }
    }

    /**
     * Butun papkani ichidagi barcha fayllari bilan birga yo'q qilish
     */
    fun shredDirectory(directory: File): Boolean {
        if (!directory.exists()) return true
        if (directory.isDirectory) {
            val children = directory.listFiles() ?: return directory.delete()
            for (child in children) {
                if (child.isDirectory) {
                    shredDirectory(child)
                } else {
                    shredFile(child)
                }
            }
        }
        return directory.delete()
    }

    private fun overwriteWithByte(raf: RandomAccessFile, length: Long, pattern: Byte) {
        raf.seek(0)
        val buffer = ByteArray(BUFFER_SIZE) { pattern }
        var bytesWritten = 0L

        while (bytesWritten < length) {
            val toWrite = minOf(BUFFER_SIZE.toLong(), length - bytesWritten).toInt()
            raf.write(buffer, 0, toWrite)
            bytesWritten += toWrite
        }
        raf.fd.sync()
    }

    private fun overwriteWithRandom(raf: RandomAccessFile, length: Long) {
        raf.seek(0)
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesWritten = 0L

        while (bytesWritten < length) {
            val toWrite = minOf(BUFFER_SIZE.toLong(), length - bytesWritten).toInt()
            secureRandom.nextBytes(buffer)
            raf.write(buffer, 0, toWrite)
            bytesWritten += toWrite
        }
        raf.fd.sync()
    }

    private fun safeDelete(file: File): Boolean {
        // Fayl nomini tasodifiy o'zgartirish (Forensic tahlilda fayl asl nomini o'qib bo'lmasligi uchun)
        val parent = file.parentFile
        val randomName = UUID.randomUUID().toString().replace("-", "")
        val renamedFile = File(parent, randomName)
        val successRename = file.renameTo(renamedFile)

        val target = if (successRename) renamedFile else file
        return target.delete()
    }
}
