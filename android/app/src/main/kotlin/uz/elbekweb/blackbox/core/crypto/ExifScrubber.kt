package uz.elbekweb.blackbox.core.crypto

import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * EXIF Scrubber & Metadata Tozalovchi.
 * Fotosuratlardan GPS joylashuv (aniq koordinatalar), qurilma modeli,
 * seriya raqami, dasturiy ta'minot va vaqt tamg'alarini to'liq tozalaydi.
 */
object ExifScrubber {

    private val SENSITIVE_EXIF_TAGS = listOf(
        // GPS koordinatalari
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_AREA_INFORMATION,

        // Qurilma va kamera ma'lumotlari
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_BODY_SERIAL_NUMBER,
        ExifInterface.TAG_LENS_MAKE,
        ExifInterface.TAG_LENS_MODEL,
        ExifInterface.TAG_LENS_SERIAL_NUMBER,
        ExifInterface.TAG_CAMERA_OWNER_NAME,

        // Foydalanuvchi va shaxsiy ma'lumotlar
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED
    )

    /**
     * Rasm faylidagi barcha maxfiy metadata teglarini tozalash
     */
    fun scrubImage(imageFile: File): Boolean {
        return try {
            if (!imageFile.exists() || !imageFile.canWrite()) return false

            val exif = ExifInterface(imageFile.absolutePath)

            for (tag in SENSITIVE_EXIF_TAGS) {
                exif.setAttribute(tag, null)
            }

            // EXIF o'zgarishlarini faylga saqlash
            exif.saveAttributes()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Rasmni tozalab, yangi manzilga nusxalash
     */
    fun scrubToCopy(sourceFile: File, destinationFile: File): Boolean {
        return try {
            sourceFile.copyTo(destinationFile, overwrite = true)
            scrubImage(destinationFile)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
