package com.fitquest.app.ui.fragments.pose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File

class ImageProcessor(private val context: Context) {

    fun createFileFromUri(uri: Uri): File? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(
                context.cacheDir,
                "${PoseConstants.FILE_PREFIX_GALLERY}${System.currentTimeMillis()}${PoseConstants.FILE_EXTENSION}"
            )
            file.outputStream().use { out ->
                input.copyTo(out)
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    fun decodeBitmapWithExifCorrected(file: File): Bitmap? {
        val src = BitmapFactory.decodeFile(file.absolutePath) ?: return null

        val exif = try {
            ExifInterface(file.absolutePath)
        } catch (e: Exception) {
            return src
        }

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }

        return try {
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        } catch (e: Exception) {
            src
        }
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val scaled = downscale(bitmap, PoseConstants.IMAGE_MAX_SIDE_PX)
        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, PoseConstants.IMAGE_JPEG_QUALITY, baos)
        val bytes = baos.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun saveBitmapToFile(bitmap: Bitmap): File {
        val file = File(
            context.cacheDir,
            "${PoseConstants.FILE_PREFIX_PROCESSED}${System.currentTimeMillis()}${PoseConstants.FILE_EXTENSION}"
        )
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file
    }

    private fun downscale(src: Bitmap, maxSide: Int): Bitmap {
        val w = src.width
        val h = src.height
        val maxDim = maxOf(w, h)
        if (maxDim <= maxSide) return src

        val scale = maxSide.toFloat() / maxDim.toFloat()
        val nw = (w * scale).toInt()
        val nh = (h * scale).toInt()
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }
}