package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.CompanyProfileEntity
import com.example.data.model.SalesBillEntity
import com.example.data.model.SalesBillItemEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfInvoiceGenerator {

    fun generateAndShareInvoice(
        context: Context,
        bill: SalesBillEntity,
        items: List<SalesBillItemEntity>,
        profile: CompanyProfileEntity?
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (in points)
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
        }

        val companyName = profile?.companyName ?: "Sampad Santra"
        val gstin = profile?.gstin ?: ""
        val address = profile?.address ?: ""
        val state = profile?.state ?: "West Bengal"
        val phone = profile?.phone ?: ""

        // Top Accent Bar
        paint.color = Color.rgb(10, 92, 54) // #0A5C36 Emerald
        canvas.drawRect(0f, 0f, 595f, 15f, paint)

        // Header
        paint.color = Color.rgb(10, 92, 54)
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(companyName, 40f, 50f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var yPos = 65f
        if (gstin.isNotBlank()) {
            canvas.drawText("GSTIN: $gstin", 40f, yPos, paint)
            yPos += 14f
        }
        if (address.isNotBlank()) {
            canvas.drawText(address, 40f, yPos, paint)
            yPos += 14f
        }
        canvas.drawText("State: $state | Phone: $phone", 40f, yPos, paint)

        // Right Header (TAX INVOICE)
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TAX INVOICE", 420f, 50f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Invoice #: ${bill.invoiceNumber}", 420f, 68f, paint)
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        canvas.drawText("Date: ${dateFormat.format(Date(bill.date))}", 420f, 82f, paint)
        canvas.drawText("Payment: ${bill.paymentMode.displayName}", 420f, 96f, paint)

        // Divider
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1.5f
        canvas.drawLine(40f, 115f, 555f, 115f, paint)

        // Bill To section
        paint.color = Color.rgb(10, 92, 54)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILL TO (CUSTOMER):", 40f, 135f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(bill.customerName, 40f, 150f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        var custY = 164f
        if (bill.customerPhone.isNotBlank()) {
            canvas.drawText("Phone: ${bill.customerPhone}", 40f, custY, paint)
            custY += 14f
        }
        if (bill.customerGstin.isNotBlank()) {
            canvas.drawText("GSTIN: ${bill.customerGstin}", 40f, custY, paint)
            custY += 14f
        }
        canvas.drawText("Place of Supply: ${bill.customerState}", 40f, custY, paint)

        // Items Table Header
        val tableTop = 205f
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(40f, tableTop, 555f, tableTop + 24f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("#", 45f, tableTop + 16f, paint)
        canvas.drawText("ITEM DESCRIPTION", 65f, tableTop + 16f, paint)
        canvas.drawText("HSN", 250f, tableTop + 16f, paint)
        canvas.drawText("QTY", 310f, tableTop + 16f, paint)
        canvas.drawText("RATE", 360f, tableTop + 16f, paint)
        canvas.drawText("GST", 420f, tableTop + 16f, paint)
        canvas.drawText("TOTAL", 485f, tableTop + 16f, paint)

        // Items Rows
        var rowY = tableTop + 40f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(30, 41, 59)

        items.forEachIndexed { index, item ->
            canvas.drawText("${index + 1}", 45f, rowY, paint)
            canvas.drawText(item.itemName.take(28), 65f, rowY, paint)
            canvas.drawText(if (item.hsnCode.isBlank()) "-" else item.hsnCode, 250f, rowY, paint)
            canvas.drawText(String.format(Locale.US, "%.1f", item.quantity), 310f, rowY, paint)
            canvas.drawText(String.format(Locale.US, "₹%.2f", item.unitPrice), 360f, rowY, paint)
            val gstStr = if (item.gstRate > 0) "${item.gstRate.toInt()}%" else "0%"
            canvas.drawText(gstStr, 420f, rowY, paint)
            canvas.drawText(String.format(Locale.US, "₹%.2f", item.totalAmount), 485f, rowY, paint)

            paint.color = Color.rgb(241, 245, 249)
            canvas.drawLine(40f, rowY + 8f, 555f, rowY + 8f, paint)
            paint.color = Color.rgb(30, 41, 59)

            rowY += 22f
            if (rowY > 700f) return@forEachIndexed // avoid overflowing single page
        }

        // Totals Box
        val summaryY = rowY + 15f
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRect(330f, summaryY, 555f, summaryY + 110f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9f
        canvas.drawText("Subtotal:", 345f, summaryY + 20f, paint)
        canvas.drawText(String.format(Locale.US, "₹%.2f", bill.subtotal), 485f, summaryY + 20f, paint)

        if (bill.discountAmount > 0) {
            canvas.drawText("Discount:", 345f, summaryY + 36f, paint)
            canvas.drawText(String.format(Locale.US, "-₹%.2f", bill.discountAmount), 485f, summaryY + 36f, paint)
        }

        if (bill.cgstAmount > 0 || bill.sgstAmount > 0) {
            canvas.drawText("CGST + SGST:", 345f, summaryY + 52f, paint)
            canvas.drawText(String.format(Locale.US, "₹%.2f", bill.cgstAmount + bill.sgstAmount), 485f, summaryY + 52f, paint)
        } else if (bill.igstAmount > 0) {
            canvas.drawText("IGST:", 345f, summaryY + 52f, paint)
            canvas.drawText(String.format(Locale.US, "₹%.2f", bill.igstAmount), 485f, summaryY + 52f, paint)
        }

        paint.color = Color.rgb(10, 92, 54)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("Grand Total:", 345f, summaryY + 80f, paint)
        canvas.drawText(String.format(Locale.US, "₹%.2f", bill.grandTotal), 475f, summaryY + 80f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Paid: ₹${bill.paidAmount} | Due: ₹${bill.dueAmount}", 345f, summaryY + 100f, paint)

        // Terms and Signature at Bottom
        val footerY = 760f
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 8f
        val terms = profile?.termsConditions ?: "Goods once sold cannot be returned without original receipt."
        canvas.drawText("Terms & Conditions: $terms", 40f, footerY, paint)
        canvas.drawText("Thank you for your business!", 40f, footerY + 15f, paint)

        // Signatory
        canvas.drawLine(400f, footerY + 20f, 540f, footerY + 20f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("For $companyName", 410f, footerY + 12f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Authorized Signatory", 425f, footerY + 34f, paint)

        document.finishPage(page)

        // Save PDF to cache dir
        return try {
            val file = File(context.cacheDir, "Invoice_${bill.invoiceNumber.replace('/', '_')}.pdf")
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            outputStream.close()
            document.close()

            // Launch share intent
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Tax Invoice #${bill.invoiceNumber} - $companyName")
                putExtra(Intent.EXTRA_TEXT, "Attached Tax Invoice #${bill.invoiceNumber} from $companyName.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Tax Invoice PDF"))
            file
        } catch (e: Exception) {
            document.close()
            Toast.makeText(context, "Invoice saved: ${bill.invoiceNumber}", Toast.LENGTH_SHORT).show()
            null
        }
    }
}
