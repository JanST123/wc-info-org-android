package de.wcinfo.app.ui.results

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import de.wcinfo.app.R
import de.wcinfo.app.data.model.Toilet
import kotlin.math.roundToInt

object ToiletMarkerIcons {

    private val cache = mutableMapOf<String, BitmapDescriptor>()

    /**
     * Determines and returns the cached [BitmapDescriptor] for a given [Toilet]:
     * - If [Toilet.hasWheelchairAccess] == true: FontAwesome wheelchair icon
     * - Else if [Toilet.isGenderSeparated] == true: FontAwesome restroom icon
     * - Else: FontAwesome toilet paper icon
     *
     * Background color:
     * - If [Toilet.temporaryClosed] == true: Gray (#6B7280)
     * - Else: Primary purple (#9333EA)
     *
     * Icon color: White (#FFFFFF)
     */
    fun getMarkerIcon(context: Context, toilet: Toilet): BitmapDescriptor {
        return getMarkerIcon(
            context = context,
            hasWheelchairAccess = toilet.hasWheelchairAccess,
            isGenderSeparated = toilet.isGenderSeparated,
            temporaryClosed = toilet.temporaryClosed
        )
    }

    fun getMarkerIcon(
        context: Context,
        hasWheelchairAccess: Boolean,
        isGenderSeparated: Boolean,
        temporaryClosed: Boolean
    ): BitmapDescriptor {
        val typeKey = when {
            hasWheelchairAccess -> "wheelchair"
            isGenderSeparated -> "gender"
            else -> "toilet_paper"
        }
        val statusKey = if (temporaryClosed) "closed" else "open"
        val cacheKey = "${typeKey}_$statusKey"

        return cache.getOrPut(cacheKey) {
            val resId = getMarkerDrawableRes(hasWheelchairAccess, isGenderSeparated, temporaryClosed)
            createBitmapDescriptor(context, resId, widthDp = 36, heightDp = 44)
        }
    }

    @DrawableRes
    fun getMarkerDrawableRes(
        hasWheelchairAccess: Boolean,
        isGenderSeparated: Boolean,
        temporaryClosed: Boolean
    ): Int {
        return when {
            temporaryClosed && hasWheelchairAccess -> R.drawable.ic_marker_wheelchair_closed
            temporaryClosed && isGenderSeparated -> R.drawable.ic_marker_restroom_closed
            temporaryClosed -> R.drawable.ic_marker_toilet_paper_closed
            hasWheelchairAccess -> R.drawable.ic_marker_wheelchair
            isGenderSeparated -> R.drawable.ic_marker_restroom
            else -> R.drawable.ic_marker_toilet_paper
        }
    }

    private fun createBitmapDescriptor(
        context: Context,
        @DrawableRes resId: Int,
        widthDp: Int,
        heightDp: Int
    ): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(context, resId)
            ?: return BitmapDescriptorFactory.defaultMarker()

        val density = context.resources.displayMetrics.density
        val widthPx = (widthDp * density).roundToInt().coerceAtLeast(1)
        val heightPx = (heightDp * density).roundToInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, widthPx, heightPx)
        drawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
