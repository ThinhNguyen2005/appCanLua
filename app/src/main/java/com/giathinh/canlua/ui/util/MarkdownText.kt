package com.giathinh.canlua.ui.util

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin

/** Dùng chung 1 instance Markwon cho toàn app, tránh tạo mới ở mỗi bubble chat. */
private object MarkwonHolder {
    @Volatile private var instance: Markwon? = null
    fun get(ctx: Context): Markwon = instance ?: synchronized(this) {
        instance ?: Markwon.builder(ctx.applicationContext)
            .usePlugin(TablePlugin.create(ctx.applicationContext))
            .build()
            .also { instance = it }
    }
}

@Composable
fun AiMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color,
    linkColor: Color = textColor,
    textSizeSp: Float = 14f
) {
    val context = LocalContext.current
    val markwon = remember { MarkwonHolder.get(context) }
    val sizePx = with(LocalDensity.current) { textSizeSp.sp.toPx() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                movementMethod = LinkMovementMethod.getInstance()
                setLineSpacing(0f, 1.1f)
            }
        },
        update = { view ->
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx)
            view.setTextColor(textColor.toArgb())
            view.setLinkTextColor(linkColor.toArgb())
            // Chỉ parse lại khi nội dung thật sự đổi
            if (view.tag != markdown) {
                view.tag = markdown
                try {
                    markwon.setMarkdown(view, markdown)
                } catch (e: Exception) {
                    view.text = markdown   // fallback: hiện text thuần
                }
            }
        }
    )
}