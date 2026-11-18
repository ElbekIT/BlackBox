package uz.elbekweb.blackbox.core.crypto

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec

/**
 * Harbiy darajadagi AES-256-GCM Shifrlash Dvigateli.
 * GCM (Galois/Counter Mode) ma'lumotlarning butunligini (Integrity)
 * va maxfiyligini 128-bitli autentifikatsiya tegi orqali ta'minlaydi.
 */
object AesEncryptionEngine {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12 // 96-bit tavsiya etilgan IV uzunligi
    private const val GCM_TAG_LENGTH = 128 // 128-bit auth tag

    /**
     * Baytlarni xotirada AES-256-GCM bilan shifrlash
     */
    fun encrypt(data: ByteArray): ByteArray {
        val secretKey = KeyStoreManager.getMasterKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)

        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val cipherText = cipher.doFinal(data)

        // Natija: [12-bayt IV] + [Ciphertext + AuthTag]
        val buffer = ByteBuffer.allocate(iv.size + cipherText.size)
        buffer.put(iv)
        buffer.put(cipherText)
        return buffer.array()
    }

    /**
     * Shifrlangan baytlarni asl holiga qaytarish
     */
    fun decrypt(encryptedBytes: ByteArray): ByteArray {
        require(encryptedBytes.size > GCM_IV_LENGTH) { "Noto'g'ri shifrlangan ma'lumot hajmi" }

        val secretKey = KeyStoreManager.getMasterKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)

        val buffer = ByteBuffer.wrap(encryptedBytes)
        val iv = ByteArray(GCM_IV_LENGTH)
        buffer.get(iv)

        val cipherText = ByteArray(buffer.remaining())
        buffer.get(cipherText)

        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return cipher.doFinal(cipherText)
    }

    /**
     * Katta hajmdagi fayllarni (video, rasm, hujjat) oqimli (stream) AES-256-GCM shifrlash
     */
    fun encryptFile(sourceFile: File, destFile: File) {
        val secretKey = KeyStoreManager.getMasterKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)

        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        FileInputStream(sourceFile).use { fis ->
            FileOutputStream(destFile).use { fos ->
                // Avval 12-bayt IV yoziladi
                fos.write(iv)
                CipherOutputStream(fos, cipher).use { cos ->
                    val buffer = ByteArray(64 * 1024) // 64 KB bufer
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        cos.write(buffer, 0, bytesRead)
                    }
                    cos.flush()
                }
            }
        }
    }

    /**
     * Shifrlangan seyf faylini ochish (deshifrlash)
     */
    fun decryptFile(sourceEncryptedFile: File, destFile: File) {
        val secretKey = KeyStoreManager.getMasterKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)

        FileInputStream(sourceEncryptedFile).use { fis ->
            val iv = ByteArray(GCM_IV_LENGTH)
            val readIv = fis.read(iv)
            require(readIv == GCM_IV_LENGTH) { "Faylda IV topilmadi yoki buzilgan" }

            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            CipherInputStream(fis, cipher).use { cis ->
                FileOutputStream(destFile).use { fos ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (cis.read(buffer).also { bytesRead = it } != -1) {
                        fos.write(buffer, 0, bytesRead)
                    }
                    fos.flush()
                }
            }
        }
    }
}
