package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.model.CatalogConstants
import java.io.ByteArrayOutputStream
import kotlin.math.min

object IconImageProcessor {

    private const val TARGET_ICON_PX = 160
    private const val MAX_RAW_ICON_BYTES = 75_000

    fun processIconUri(context: Context, uri: Uri): String? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        val origWidth = boundsOptions.outWidth
        val origHeight = boundsOptions.outHeight
        if (origWidth <= 0 || origHeight <= 0) return null

        var sampleSize = 1
        while ((origWidth / sampleSize) > TARGET_ICON_PX * 2 &&
            (origHeight / sampleSize) > TARGET_ICON_PX * 2
        ) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        val square = centerCropSquare(decoded)
        val scaled = if (square.width != TARGET_ICON_PX || square.height != TARGET_ICON_PX) {
            Bitmap.createScaledBitmap(square, TARGET_ICON_PX, TARGET_ICON_PX, true)
        } else {
            square
        }

        if (square !== decoded) decoded.recycle()
        if (scaled !== square) square.recycle()

        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

        var quality = 88
        var encodedBytes: ByteArray
        while (true) {
            val out = ByteArrayOutputStream()
            scaled.compress(format, quality, out)
            encodedBytes = out.toByteArray()
            if (encodedBytes.size <= MAX_RAW_ICON_BYTES || quality <= 30) {
                break
            }
            quality -= 15
        }
        scaled.recycle()

        val b64 = Base64.encodeToString(encodedBytes, Base64.NO_WRAP)
        return if (b64.length <= CatalogConstants.MAX_ICON_BASE64_CHARS) b64 else null
    }

    fun decodeIconBitmap(iconBase64: String): ImageBitmap? {
        if (iconBase64.isBlank()) return null
        return try {
            val bytes = Base64.decode(iconBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    private fun centerCropSquare(src: Bitmap): Bitmap {
        val side = min(src.width, src.height)
        if (src.width == side && src.height == side) return src
        val x = (src.width - side) / 2
        val y = (src.height - side) / 2
        return Bitmap.createBitmap(src, x, y, side, side)
    }
}
