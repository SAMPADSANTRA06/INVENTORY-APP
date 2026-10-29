package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun Barcode1DView(
    code: String,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val cleanCode = if (code.isBlank()) "000000000000" else code.trim()

    Column(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val totalWidth = size.width
            val height = size.height

            // Generate deterministic bar widths from string characters
            val hash = cleanCode.hashCode()
            val seedChars = cleanCode.toCharArray()
            val totalBars = 35
            val unitWidth = totalWidth / (totalBars * 1.5f)

            var currentX = 0f

            // Start guard pattern (thin, space, thin)
            drawRect(barColor, Offset(currentX, 0f), Size(unitWidth * 1.2f, height))
            currentX += unitWidth * 2.2f
            drawRect(barColor, Offset(currentX, 0f), Size(unitWidth * 1.2f, height))
            currentX += unitWidth * 2.2f

            for (i in 0 until (totalBars - 8)) {
                val charVal = if (i < seedChars.size) seedChars[i].code else ((abs(hash) + i * 31) % 9)
                val isThick = (charVal % 2 == 0)
                val barW = if (isThick) unitWidth * 2f else unitWidth * 1f

                if ((i + charVal) % 4 != 0) { // space gap
                    drawRect(barColor, Offset(currentX, 0f), Size(barW, height))
                }
                currentX += barW + unitWidth * 0.8f
                if (currentX >= totalWidth - unitWidth * 4) break
            }

            // End guard pattern
            drawRect(barColor, Offset(totalWidth - unitWidth * 2.4f, 0f), Size(unitWidth * 1.2f, height))
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = cleanCode,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF334155),
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    darkColor: Color = Color(0xFF0F172A),
    lightColor: Color = Color.White
) {
    val content = if (data.isBlank()) "SAMPAD-SANTRA" else data.trim()
    val matrixSize = 21 // standard version 1 QR grid dimension

    Column(
        modifier = modifier
            .background(lightColor, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .size(110.dp)
        ) {
            val cellSize = size.width / matrixSize

            // Background
            drawRect(lightColor, Offset.Zero, size)

            // Draw Finder Patterns (Top-Left, Top-Right, Bottom-Left)
            drawFinderPattern(0, 0, cellSize, darkColor, lightColor)
            drawFinderPattern(matrixSize - 7, 0, cellSize, darkColor, lightColor)
            drawFinderPattern(0, matrixSize - 7, cellSize, darkColor, lightColor)

            // Draw Timing Patterns
            for (i in 8 until matrixSize - 8) {
                if (i % 2 == 0) {
                    drawRect(darkColor, Offset(6 * cellSize, i * cellSize), Size(cellSize, cellSize))
                    drawRect(darkColor, Offset(i * cellSize, 6 * cellSize), Size(cellSize, cellSize))
                }
            }

            // Pseudo-random deterministic payload bits based on content
            val hash = content.hashCode()
            for (r in 0 until matrixSize) {
                for (c in 0 until matrixSize) {
                    // Skip finder pattern zones
                    val inTL = r < 8 && c < 8
                    val inTR = r < 8 && c >= matrixSize - 8
                    val inBL = r >= matrixSize - 8 && c < 8
                    val inTiming = r == 6 || c == 6

                    if (!inTL && !inTR && !inBL && !inTiming) {
                        val bit = ((r * 17 + c * 31 + abs(hash) + content.length) % 7) < 3
                        if (bit) {
                            drawRect(
                                darkColor,
                                Offset(c * cellSize, r * cellSize),
                                Size(cellSize, cellSize)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFinderPattern(
    startX: Int,
    startY: Int,
    cellSize: Float,
    dark: Color,
    light: Color
) {
    // 7x7 outer square
    drawRect(
        color = dark,
        topLeft = Offset(startX * cellSize, startY * cellSize),
        size = Size(7 * cellSize, 7 * cellSize)
    )
    // 5x5 inner white
    drawRect(
        color = light,
        topLeft = Offset((startX + 1) * cellSize, (startY + 1) * cellSize),
        size = Size(5 * cellSize, 5 * cellSize)
    )
    // 3x3 center dark
    drawRect(
        color = dark,
        topLeft = Offset((startX + 2) * cellSize, (startY + 2) * cellSize),
        size = Size(3 * cellSize, 3 * cellSize)
    )
}
