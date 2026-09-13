package com.giathinh.canlua.ui.component.market

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.giathinh.canlua.R
import com.giathinh.canlua.ui.theme.AppColors
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

/**
 * Thẻ quảng cáo tự nhiên (AdMob NativeAdCard) chính thức — hiển thị asset từ NativeAd thật.
 *
 * Tuân thủ nghiêm ngặt:
 *  - Giữ nhận diện thương hiệu app: CardBg, bo tròn 16dp, viền gold nhẹ phân biệt với NewsCard.
 *  - Chỉ hiển thị nội dung từ assets của Google NativeAd: headline, body, advertiser, icon/media, callToAction.
 *  - Phân biệt rõ ràng với NewsCard qua badge "Quảng cáo" và nút Call To Action riêng.
 *  - KHÔNG gắn Modifier.clickable hay onClick tự viết lên toàn card.
 *  - NativeAdView / Google Mobile Ads SDK tự động đăng ký và xử lý clicks và ghi nhận impressions.
 */
@Composable
fun NativeAdCard(
    nativeAd: NativeAd,
    modifier: Modifier = Modifier
) {
    val cardBg = AppColors.CardBg
    val textPrimary = AppColors.TextPrimary
    val textSecondary = AppColors.TextSecondary
    val greenPrimary = AppColors.GreenPrimary
    val goldAccent = AppColors.GoldAccent

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            LayoutInflater.from(context).inflate(R.layout.view_native_ad, null, false)
        },
        update = { rootView ->
            val adView = rootView as? NativeAdView ?: return@AndroidView
            val ivIcon = adView.findViewById<ImageView>(R.id.ad_app_icon)
            val tvHeadline = adView.findViewById<TextView>(R.id.ad_headline)
            val tvAdvertiser = adView.findViewById<TextView>(R.id.ad_advertiser)
            val tvBody = adView.findViewById<TextView>(R.id.ad_body)
            val tvPrice = adView.findViewById<TextView>(R.id.ad_price)
            val tvStore = adView.findViewById<TextView>(R.id.ad_store)
            val btnCta = adView.findViewById<Button>(R.id.ad_call_to_action)
            val mediaView = adView.findViewById<MediaView>(R.id.ad_media)

            // Dynamic theme colors thích ứng Dark/Light Mode qua GradientDrawable
            val backgroundDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f * adView.resources.displayMetrics.density
                setColor(cardBg.toArgb())
                setStroke((1 * adView.resources.displayMetrics.density).toInt(), goldAccent.copy(alpha = 0.35f).toArgb())
            }
            adView.background = backgroundDrawable

            val ctaDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * adView.resources.displayMetrics.density
                setColor(greenPrimary.toArgb())
            }
            btnCta?.background = ctaDrawable

            tvHeadline?.setTextColor(textPrimary.toArgb())
            tvBody?.setTextColor(textSecondary.toArgb())
            tvAdvertiser?.setTextColor(greenPrimary.toArgb())
            tvPrice?.setTextColor(goldAccent.toArgb())

            // Đăng ký các view thành phần với NativeAdView
            adView.headlineView = tvHeadline
            adView.bodyView = tvBody
            adView.callToActionView = btnCta
            adView.iconView = ivIcon
            adView.advertiserView = tvAdvertiser
            adView.priceView = tvPrice
            adView.storeView = tvStore

            // 1. Headline (bắt buộc)
            tvHeadline?.text = nativeAd.headline

            // 2. Body
            if (nativeAd.body.isNullOrBlank()) {
                tvBody?.visibility = View.GONE
            } else {
                tvBody?.visibility = View.VISIBLE
                tvBody?.text = nativeAd.body
            }

            // 3. App Icon
            val icon = nativeAd.icon
            if (icon?.drawable != null) {
                ivIcon?.visibility = View.VISIBLE
                ivIcon?.setImageDrawable(icon.drawable)
            } else {
                ivIcon?.visibility = View.GONE
            }

            // 4. Advertiser / Store
            val advertiser = nativeAd.advertiser ?: nativeAd.store
            if (advertiser.isNullOrBlank()) {
                tvAdvertiser?.visibility = View.GONE
            } else {
                tvAdvertiser?.visibility = View.VISIBLE
                tvAdvertiser?.text = advertiser
            }

            // 5. Price & Store
            val price = nativeAd.price
            if (price.isNullOrBlank()) {
                tvPrice?.visibility = View.GONE
            } else {
                tvPrice?.visibility = View.VISIBLE
                tvPrice?.text = price
            }

            val store = nativeAd.store
            if (store.isNullOrBlank() || store == advertiser) {
                tvStore?.visibility = View.GONE
            } else {
                tvStore?.visibility = View.VISIBLE
                tvStore?.text = store
            }

            // 6. Call To Action Button
            if (nativeAd.callToAction.isNullOrBlank()) {
                btnCta?.visibility = View.GONE
            } else {
                btnCta?.visibility = View.VISIBLE
                btnCta?.text = nativeAd.callToAction
            }

            // 7. Media Content & Final Ad Registration
            val mediaContent = nativeAd.mediaContent
            val hasMedia = mediaContent.hasVideoContent || mediaContent.mainImage != null
            if (hasMedia && mediaView != null) {
                mediaView.visibility = View.VISIBLE
            } else {
                mediaView?.visibility = View.GONE
            }

            // Next-Gen SDK yêu cầu đăng ký NativeAd với MediaView
            if (mediaView != null) {
                adView.registerNativeAd(nativeAd, mediaView)
            } else {
                android.util.Log.w("NativeAdCard", "MediaView (R.id.ad_media) is null in layout; cannot register native ad")
            }
        }
    )
}
