package com.GiaThinh.canlua.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.GiaThinh.canlua.R
import com.GiaThinh.canlua.data.model.Card
import com.GiaThinh.canlua.data.model.WeightEntry
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Xuất phiếu cân A4 dạng chứng từ, có mã xác thực/hash và vùng dành cho ký số.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val LINE_HEIGHT = 17f
    private const val FOOTER_Y = PAGE_HEIGHT - 22f

    private val numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN"))
    private val shortDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))

    fun export(context: Context, card: Card, entries: List<WeightEntry>): File {
        val labels = PdfLabels(context)
        val verificationHash = buildVerificationHash(card, entries)
        val shortHash = verificationHash.take(16).uppercase(Locale.US)
        val qrPayload = buildQrPayload(card, verificationHash)
        val qrBitmap = createQrBitmap(qrPayload, 116)

        val pdf = PdfDocument()
        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        var canvas = page.canvas
        var y = MARGIN

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B5E20")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#424242")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E7D32")
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10.5f
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#616161")
            textSize = 9.5f
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#757575")
            textSize = 8.5f
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D6D6D6")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F6FBF6")
            style = Paint.Style.FILL
        }
        val lightFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAFAFA")
            style = Paint.Style.FILL
        }

        fun finishCurrentPage() {
            drawFooter(canvas, pageNum, verificationHash, mutedPaint, labels)
            pdf.finishPage(page)
        }

        fun startNewPage() {
            finishCurrentPage()
            pageNum++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
            canvas = page.canvas
            y = MARGIN
        }

        fun ensureSpace(requiredHeight: Float) {
            if (y + requiredHeight > FOOTER_Y - 10f) {
                startNewPage()
            }
        }

        y = drawHeader(canvas, card, qrBitmap, titlePaint, subtitlePaint, labelPaint, bodyPaint, borderPaint, shortHash, y, labels)
        y += 12f

        y = drawInfoSection(
            canvas = canvas,
            title = labels.transactionInfo,
            leftItems = listOf(
                labels.farmer to card.name.ifBlank { labels.emptyValue },
                labels.trader to card.traderName.ifBlank { labels.emptyValue },
                labels.traderPhone to card.traderPhone.ifBlank { labels.emptyValue },
                labels.createdDate to dateFormat.format(card.date)
            ),
            rightItems = listOf(
                labels.riceVariety to card.riceVariety.ifBlank { labels.emptyValue },
                labels.season to card.seasonLabel.ifBlank { labels.emptyValue },
                labels.fieldAddress to card.fieldAddress.ifBlank { labels.emptyValue },
                labels.cardId to "#${card.id}"
            ),
            y = y,
            sectionPaint = sectionPaint,
            labelPaint = labelPaint,
            bodyPaint = bodyPaint,
            borderPaint = borderPaint,
            fillPaint = lightFillPaint
        )
        y += 10f

        y = drawMetricsAndMoney(
            canvas = canvas,
            card = card,
            y = y,
            sectionPaint = sectionPaint,
            labelPaint = labelPaint,
            bodyPaint = bodyPaint,
            boldPaint = boldPaint,
            borderPaint = borderPaint,
            fillPaint = fillPaint,
            labels = labels
        )
        y += 12f

        if (entries.isNotEmpty()) {
            val showTareImpurity = entries.any { it.bagWeight > 0.0 || it.impurityWeight > 0.0 }
            ensureSpace(78f)
            canvas.drawText(labels.bagDetailsCount(entries.size), MARGIN, y, sectionPaint)
            // +18f thay vì +13f — section font 12.5f, baseline→border chỉ 13f
            // làm chữ "Chi tiết theo bao" dính sát header bảng. 18f cho thoáng visual.
            y += 18f
            y = drawWeightTableHeader(canvas, y, labelPaint, borderPaint, fillPaint, labels, showTareImpurity)

            entries.forEachIndexed { idx, entry ->
                ensureSpace(18f)
                y = drawWeightTableRow(canvas, y, idx + 1, entry, bodyPaint, borderPaint, showTareImpurity)
            }

            ensureSpace(22f)
            y = drawWeightTableTotal(canvas, y, entries, boldPaint, borderPaint, fillPaint, labels, showTareImpurity)
        } else {
            ensureSpace(48f)
            canvas.drawText(labels.bagDetails, MARGIN, y, sectionPaint)
            // +20f thay vì +16f — chữ "Chi tiết theo bao" cách body "Không có" thoáng hơn.
            y += 20f
            canvas.drawText(labels.noBags, MARGIN, y, bodyPaint)
            y += 18f
        }

        ensureSpace(100f)
        y += 14f
        drawSignatureSection(canvas, y, sectionPaint, labelPaint, borderPaint, labels)

        drawFooter(canvas, pageNum, verificationHash, mutedPaint, labels)
        pdf.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "phieu-can-${card.id}-${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }

    fun shareUri(context: Context, file: File) =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

    private fun drawHeader(
        canvas: android.graphics.Canvas,
        card: Card,
        qrBitmap: Bitmap,
        titlePaint: Paint,
        subtitlePaint: Paint,
        labelPaint: Paint,
        bodyPaint: Paint,
        borderPaint: Paint,
        shortHash: String,
        yStart: Float,
        labels: PdfLabels
    ): Float {
        var y = yStart
        canvas.drawText(labels.brand, MARGIN, y + 11f, subtitlePaint)
        canvas.drawText(labels.title, MARGIN, y + 35f, titlePaint)
        canvas.drawText(labels.subtitle, MARGIN, y + 53f, bodyPaint)
        canvas.drawText(labels.createdAt(dateFormat.format(card.date)), MARGIN, y + 70f, bodyPaint)
        canvas.drawText(labels.verificationHash(shortHash), MARGIN, y + 87f, bodyPaint)

        val qrX = PAGE_WIDTH - MARGIN - 116f
        canvas.drawBitmap(qrBitmap, qrX, y, null)
        canvas.drawRect(qrX, y, qrX + 116f, y + 116f, borderPaint)
        canvas.drawText(labels.qrInstruction, qrX + 11f, y + 130f, labelPaint)
        canvas.drawText(labels.cardDate(card.id, shortDateFormat.format(card.date)), qrX + 3f, y + 144f, labelPaint)

        y += 154f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, borderPaint)
        return y
    }

    private fun drawInfoSection(
        canvas: android.graphics.Canvas,
        title: String,
        leftItems: List<Pair<String, String>>,
        rightItems: List<Pair<String, String>>,
        y: Float,
        sectionPaint: Paint,
        labelPaint: Paint,
        bodyPaint: Paint,
        borderPaint: Paint,
        fillPaint: Paint
    ): Float {
        var cursor = y
        canvas.drawText(title, MARGIN, cursor, sectionPaint)
        // +14f thay vì +9f — sectionPaint baseline → box top chỉ 9f làm chữ tiêu đề
        // sát border bảng. Tăng lên 14f cho thoáng (font 12.5f cần ≥ font_size).
        cursor += 14f

        val boxTop = cursor
        val rowHeight = 24f
        val rows = maxOf(leftItems.size, rightItems.size)
        val boxBottom = boxTop + rows * rowHeight + 10f
        val midX = PAGE_WIDTH / 2f

        canvas.drawRect(MARGIN, boxTop, PAGE_WIDTH - MARGIN, boxBottom, fillPaint)
        canvas.drawRect(MARGIN, boxTop, PAGE_WIDTH - MARGIN, boxBottom, borderPaint)
        canvas.drawLine(midX, boxTop, midX, boxBottom, borderPaint)

        repeat(rows) { index ->
            val rowY = boxTop + 18f + index * rowHeight
            leftItems.getOrNull(index)?.let { (label, value) ->
                drawKvInline(canvas, label, value, MARGIN + 12f, rowY, 82f, labelPaint, bodyPaint, maxWidth = 170f)
            }
            rightItems.getOrNull(index)?.let { (label, value) ->
                drawKvInline(canvas, label, value, midX + 12f, rowY, 82f, labelPaint, bodyPaint, maxWidth = 170f)
            }
        }
        return boxBottom
    }

    private fun drawMetricsAndMoney(
        canvas: android.graphics.Canvas,
        card: Card,
        y: Float,
        sectionPaint: Paint,
        labelPaint: Paint,
        bodyPaint: Paint,
        boldPaint: Paint,
        borderPaint: Paint,
        fillPaint: Paint,
        labels: PdfLabels
    ): Float {
        var cursor = y
        val leftX = MARGIN
        val rightX = PAGE_WIDTH / 2f + 8f
        val boxWidth = PAGE_WIDTH / 2f - MARGIN - 8f
        val rowHeight = 22f

        canvas.drawText(labels.weightMetrics, leftX, cursor, sectionPaint)
        canvas.drawText(labels.payment, rightX, cursor, sectionPaint)
        // +14f thay vì +9f — chữ "Chỉ số cân"/"Thanh toán" cách 2 box bên dưới thoáng hơn,
        // không còn dính sát border.
        cursor += 14f

        val boxTop = cursor
        val boxBottom = boxTop + rowHeight * 7f + 10f
        canvas.drawRect(leftX, boxTop, leftX + boxWidth, boxBottom, fillPaint)
        canvas.drawRect(rightX, boxTop, rightX + boxWidth, boxBottom, fillPaint)
        canvas.drawRect(leftX, boxTop, leftX + boxWidth, boxBottom, borderPaint)
        canvas.drawRect(rightX, boxTop, rightX + boxWidth, boxBottom, borderPaint)

        val metrics = listOf(
            labels.totalWeight to "${formatKg(card.totalWeight)} kg",
            labels.bagCount to "${card.bagCount} bao",
            labels.tare to if (card.bagMethodIsSampling && card.bagSampleCount > 0)
                "${formatKg(card.bagSampleTotalWeight)} kg / ${card.bagSampleCount} bao"
                else "${formatKg(card.bagWeight)} kg/bao",
            labels.impurity to if (card.impurityIsPercent)
                "%.1f%%".format(card.impurityWeight)
                else "${formatKg(card.impurityWeight)} kg",
            labels.moisture to "%.1f%%".format(card.moisturePercent),
            labels.netWeight to "${formatKg(card.netWeight)} kg"
        )

        // Payment logic: gross paid (cọc + đã trả) so với thành tiền
        val grossPaid = card.depositAmount + card.paidAmount
        val diff = grossPaid - card.totalAmount  // dương = trả thừa, âm = còn nợ
        val (closingLabel, closingValue, isSettled) = when {
            card.totalAmount <= 0.0 -> Triple(labels.remaining, "0 đ", false)
            diff > 0.0 -> Triple(labels.excessRefund, "${formatMoney(diff)} đ", true)
            diff < 0.0 -> Triple(labels.amountDue, "${formatMoney(-diff)} đ", false)
            else -> Triple(labels.remaining, "0 đ", true)
        }
        val statusText = if (isSettled) labels.paidFull else labels.debtRemaining

        val money = listOf(
            labels.price to "${formatMoney(card.pricePerKg)} đ/kg",
            labels.totalAmount to "${formatMoney(card.totalAmount)} đ",
            labels.deposit to "${formatMoney(card.depositAmount)} đ",
            labels.paid to "${formatMoney(card.paidAmount)} đ",
            closingLabel to closingValue,
            labels.status to statusText
        )

        metrics.forEachIndexed { index, item ->
            val paint = if (item.first == labels.netWeight) boldPaint else bodyPaint
            drawKvInline(canvas, item.first, item.second, leftX + 12f, boxTop + 18f + index * rowHeight, 86f, labelPaint, paint, 130f)
        }
        money.forEachIndexed { index, item ->
            val paint = if (item.first == labels.totalAmount || item.first == closingLabel) boldPaint else bodyPaint
            drawKvInline(canvas, item.first, item.second, rightX + 12f, boxTop + 18f + index * rowHeight, 78f, labelPaint, paint, 130f)
        }

        return boxBottom
    }

    private fun drawWeightTableHeader(
        canvas: android.graphics.Canvas,
        y: Float,
        labelPaint: Paint,
        borderPaint: Paint,
        fillPaint: Paint,
        pdfLabels: PdfLabels,
        showTareImpurity: Boolean
    ): Float {
        val cols = tableColumns(showTareImpurity)
        val rowBottom = y + 20f
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, rowBottom, fillPaint)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, rowBottom, borderPaint)
        for (i in 1 until cols.lastIndex) {
            canvas.drawLine(cols[i], y, cols[i], rowBottom, borderPaint)
        }
        val headers = if (showTareImpurity) listOf(
            pdfLabels.columnIndex,
            pdfLabels.columnGrossWeight,
            pdfLabels.columnTare,
            pdfLabels.columnImpurity,
            pdfLabels.columnNetWeight
        ) else listOf(
            pdfLabels.columnIndex,
            pdfLabels.columnGrossWeight,
            pdfLabels.columnNetWeight
        )
        headers.forEachIndexed { index, label ->
            canvas.drawText(label, cols[index] + 6f, y + 14f, labelPaint)
        }
        return rowBottom
    }

    private fun drawWeightTableRow(
        canvas: android.graphics.Canvas,
        y: Float,
        index: Int,
        entry: WeightEntry,
        bodyPaint: Paint,
        borderPaint: Paint,
        showTareImpurity: Boolean
    ): Float {
        val cols = tableColumns(showTareImpurity)
        val rowBottom = y + 18f
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, rowBottom, borderPaint)
        for (i in 1 until cols.lastIndex) {
            canvas.drawLine(cols[i], y, cols[i], rowBottom, borderPaint)
        }
        val values = if (showTareImpurity) listOf(
            index.toString(),
            formatKg(entry.weight),
            formatKg(entry.bagWeight),
            formatKg(entry.impurityWeight),
            formatKg(entry.netWeight)
        ) else listOf(
            index.toString(),
            formatKg(entry.weight),
            formatKg(entry.netWeight)
        )
        values.forEachIndexed { col, value ->
            canvas.drawText(value, cols[col] + 6f, y + 13f, bodyPaint)
        }
        return rowBottom
    }

    private fun drawWeightTableTotal(
        canvas: android.graphics.Canvas,
        y: Float,
        entries: List<WeightEntry>,
        boldPaint: Paint,
        borderPaint: Paint,
        fillPaint: Paint,
        pdfLabels: PdfLabels,
        showTareImpurity: Boolean
    ): Float {
        val cols = tableColumns(showTareImpurity)
        val rowBottom = y + 20f
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, rowBottom, fillPaint)
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, rowBottom, borderPaint)
        for (i in 1 until cols.lastIndex) {
            canvas.drawLine(cols[i], y, cols[i], rowBottom, borderPaint)
        }
        val sumGross = entries.sumOf { it.weight }
        val sumNet = entries.sumOf { it.netWeight }
        val values = if (showTareImpurity) listOf(
            pdfLabels.tableTotal,
            formatKg(sumGross),
            formatKg(entries.sumOf { it.bagWeight }),
            formatKg(entries.sumOf { it.impurityWeight }),
            formatKg(sumNet)
        ) else listOf(
            pdfLabels.tableTotal,
            formatKg(sumGross),
            formatKg(sumNet)
        )
        values.forEachIndexed { col, value ->
            canvas.drawText(value, cols[col] + 6f, y + 14f, boldPaint)
        }
        return rowBottom
    }

    private fun tableColumns(showTareImpurity: Boolean): FloatArray = if (showTareImpurity) {
        floatArrayOf(MARGIN, MARGIN + 45f, MARGIN + 150f, MARGIN + 245f, MARGIN + 340f, PAGE_WIDTH - MARGIN)
    } else {
        // 3 columns: STT (45f), KL thô (rest/2), KL thực (rest/2)
        val midWidth = (PAGE_WIDTH - MARGIN - (MARGIN + 45f)) / 2f
        floatArrayOf(MARGIN, MARGIN + 45f, MARGIN + 45f + midWidth, PAGE_WIDTH - MARGIN)
    }

    private fun drawSignatureSection(
        canvas: android.graphics.Canvas,
        y: Float,
        sectionPaint: Paint,
        labelPaint: Paint,
        borderPaint: Paint,
        labels: PdfLabels
    ) {
        canvas.drawText(labels.confirmationSection, MARGIN, y, sectionPaint)
        // +16f thay vì +12f — tiêu đề "Xác nhận" cách 3 ô ký tên thoáng hơn.
        val top = y + 16f
        val bottom = top + 78f
        val colWidth = (PAGE_WIDTH - MARGIN * 2) / 3f
        val titles = listOf(labels.sender, labels.receiver, labels.creator)
        titles.forEachIndexed { index, title ->
            val left = MARGIN + index * colWidth
            val right = left + colWidth
            canvas.drawRect(left, top, right, bottom, borderPaint)
            canvas.drawText(title, left + 20f, top + 16f, labelPaint)
            canvas.drawText(labels.signatureHint, left + 20f, bottom - 12f, labelPaint)
        }
    }

    private fun drawKvInline(
        canvas: android.graphics.Canvas,
        label: String,
        value: String,
        x: Float,
        y: Float,
        labelWidth: Float,
        labelPaint: Paint,
        valuePaint: Paint,
        maxWidth: Float
    ) {
        canvas.drawText(label, x, y, labelPaint)
        canvas.drawText(ellipsize(value, valuePaint, maxWidth), x + labelWidth, y, valuePaint)
    }

    private fun drawFooter(
        canvas: android.graphics.Canvas,
        pageNum: Int,
        verificationHash: String,
        paint: Paint,
        labels: PdfLabels
    ) {
        val text = labels.footer(
            pageNum,
            verificationHash.take(16).uppercase(Locale.US),
            dateFormat.format(Date())
        )
        canvas.drawText(text, MARGIN, FOOTER_Y, paint)
    }

    private fun buildVerificationHash(card: Card, entries: List<WeightEntry>): String {
        val canonical = buildString {
            append("CAN_LUA_WEIGHT_TICKET|v1|")
            append(card.id).append('|')
            append(card.date.time).append('|')
            append(card.name.trim()).append('|')
            append(card.traderName.trim()).append('|')
            append(formatKg(card.totalWeight)).append('|')
            append(formatKg(card.netWeight)).append('|')
            append(card.bagCount).append('|')
            append(formatMoney(card.pricePerKg)).append('|')
            append(formatMoney(card.totalAmount)).append('|')
            entries.forEachIndexed { index, entry ->
                append(index + 1).append(':')
                append(formatKg(entry.weight)).append(':')
                append(formatKg(entry.bagWeight)).append(':')
                append(formatKg(entry.impurityWeight)).append(':')
                append(formatKg(entry.netWeight)).append('|')
            }
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun buildQrPayload(card: Card, hash: String): String =
        "{" +
            "\"type\":\"CAN_LUA_WEIGHT_TICKET\"," +
            "\"cardId\":${card.id}," +
            "\"createdAt\":${card.date.time}," +
            "\"hash\":\"$hash\"," +
            "\"totalWeight\":\"${formatKg(card.totalWeight)}\"," +
            "\"netWeight\":\"${formatKg(card.netWeight)}\"," +
            "\"totalAmount\":${card.totalAmount.toLong()}" +
            "}"

    private fun createQrBitmap(payload: String, size: Int): Bitmap {
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    private fun ellipsize(value: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(value) <= maxWidth) return value
        var end = value.length
        while (end > 1 && paint.measureText(value.take(end) + "…") > maxWidth) {
            end--
        }
        return value.take(end) + "…"
    }

    private fun formatKg(value: Double): String = "%.1f".format(Locale.US, value)

    private fun formatMoney(value: Double): String = numberFormat.format(value.toLong())

    private class PdfLabels(private val context: Context) {
        val emptyValue = context.getString(R.string.detail_info_empty_value)
        val transactionInfo = context.getString(R.string.pdf_section_transaction_info)
        val farmer = context.getString(R.string.pdf_farmer)
        val trader = context.getString(R.string.pdf_trader)
        val traderPhone = context.getString(R.string.pdf_trader_phone)
        val createdDate = context.getString(R.string.pdf_created_date)
        val riceVariety = context.getString(R.string.pdf_rice_variety)
        val season = context.getString(R.string.pdf_season)
        val fieldAddress = context.getString(R.string.pdf_field_address)
        val cardId = context.getString(R.string.pdf_card_id)
        val weightMetrics = context.getString(R.string.pdf_section_weight_metrics)
        val payment = context.getString(R.string.pdf_section_payment)
        val totalWeight = context.getString(R.string.pdf_total_weight)
        val bagCount = context.getString(R.string.pdf_bag_count)
        val tare = context.getString(R.string.pdf_tare)
        val impurity = context.getString(R.string.pdf_impurity)
        val moisture = context.getString(R.string.pdf_moisture)
        val netWeight = context.getString(R.string.pdf_net_weight)
        val price = context.getString(R.string.pdf_price)
        val totalAmount = context.getString(R.string.pdf_total_amount)
        val deposit = context.getString(R.string.pdf_deposit)
        val paid = context.getString(R.string.pdf_paid)
        val remaining = context.getString(R.string.pdf_remaining)
        val excessRefund = context.getString(R.string.pdf_excess_refund)
        val amountDue = context.getString(R.string.pdf_amount_due)
        val status = context.getString(R.string.pdf_status)
        val paidFull = context.getString(R.string.pdf_paid_full)
        val debtRemaining = context.getString(R.string.pdf_debt_remaining)
        val bagDetails = context.getString(R.string.pdf_section_bag_details)
        val noBags = context.getString(R.string.pdf_no_bags)
        val columnIndex = context.getString(R.string.pdf_column_index)
        val columnGrossWeight = context.getString(R.string.pdf_column_gross_weight)
        val columnTare = context.getString(R.string.pdf_column_tare)
        val columnImpurity = context.getString(R.string.pdf_column_impurity)
        val columnNetWeight = context.getString(R.string.pdf_column_net_weight)
        val tableTotal = context.getString(R.string.pdf_table_total)
        val confirmationSection = context.getString(R.string.pdf_section_confirmation)
        val sender = context.getString(R.string.pdf_sender)
        val receiver = context.getString(R.string.pdf_receiver)
        val creator = context.getString(R.string.pdf_creator)
        val signatureHint = context.getString(R.string.pdf_signature_hint)
        val brand = context.getString(R.string.pdf_brand)
        val title = context.getString(R.string.pdf_title)
        val subtitle = context.getString(R.string.pdf_subtitle)
        val qrInstruction = context.getString(R.string.pdf_qr_instruction)

        fun bagDetailsCount(count: Int) = context.getString(R.string.pdf_section_bag_details_count, count)
        fun createdAt(date: String) = context.getString(R.string.pdf_created_at, date)
        fun verificationHash(hash: String) = context.getString(R.string.pdf_verification_hash, hash)
        fun cardDate(cardId: Long, date: String) = context.getString(R.string.pdf_card_date, cardId, date)
        fun footer(page: Int, hash: String, createdAt: String) = context.getString(R.string.pdf_footer, page, hash, createdAt)
    }
}
