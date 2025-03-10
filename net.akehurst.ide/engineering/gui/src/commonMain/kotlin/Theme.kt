/*
 * Based on [https://github.com/n34t0/compose-code-editor]
 * which is Apache-2.0 licence
 */

package net.akehurst.ide.gui

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

object AppTheme {

    val colors: Colors = Colors()

    val code: Code = Code()

    val typography: Text = Text()

    class Colors(
        val backgroundDark: Color = Color(0xFF2B2B2B),
        val backgroundMedium: Color = Color(0xFF313335),
        val backgroundLight: Color = Color(0xFFFFFF),
        val borderDark: Color = Color(0xFF2F2F2F),
        val borderMedium: Color = Color(0xFF46494B),
        val borderLight: Color = Color(0xFF54585B),
        val primary: Color = Color(0xFF214283),
        val secondary: Color = Color(0xFF113A5C),
        val textFieldBackground: Color = Color(0xFF4E5254),
        val textColor: Color = Color(0xFFBABABA),
        val codeColor: Color = Color(0xFFA9B7C6),
        val commentColor: Color = Color(0xFF808080),
        val errorColor: Color = Color(0xFFFF574C),

        val buttonHover: Color = Color(0xFF4E5254),
        val buttonPress: Color = Color(0xFF686D70),
        val buttonDisabled: Color = commentColor,

        val indicatorColor: Color = commentColor,

        val selectionColors: TextSelectionColors = TextSelectionColors(
            handleColor = primary,
            backgroundColor = primary.copy(alpha = .9f)
        ),

        val severityError: Color = Color(0xFFBC3F3C),
        val severityWarning: Color = Color(0xFF52503A),
        val severityInfo: Color = Color(0xFF4E4E4E),

        val searchSelectionFillColor: Color = Color(0xFF32593D),
        val searchSelectionStrokeColor: Color = Color(0xFF3C704B),
        val searchSelectionActiveStrokeColor: Color = Color(0xFFBBBBBB),

        val dark: ColorScheme = darkColorScheme(
            background = backgroundDark,
       //     surface = backgroundLight,
            primary = primary,
            secondary = secondary,
            onBackground = codeColor,
            onSurface = textColor
        ),

        val light: ColorScheme = lightColorScheme(
            background = backgroundLight,
        //    surface = backgroundDark,
        //    primary = primary,
        //    secondary = secondary,
        //    onBackground = codeColor,
        //    onSurface = textColor
        )
    )

    class Code(
        val simple: SpanStyle = SpanStyle(colors.codeColor),
        val value: SpanStyle = SpanStyle(Color(0xFF6897BB)),
        val keyword: SpanStyle = SpanStyle(Color(0xFFCC7832)),
        val punctuation: SpanStyle = SpanStyle(Color(0xFFA1C17E)),
        val annotation: SpanStyle = SpanStyle(Color(0xFFBBB529)),
        val comment: SpanStyle = SpanStyle(colors.commentColor),
        val reference: SpanStyle = SpanStyle(Color(0xFF589DF6), textDecoration = TextDecoration.Underline)
    )

    class Text(
        val code: TextStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),

        val input: TextStyle = TextStyle(
            letterSpacing = 0.4.sp,
            fontSize = 14.sp
        ),

        val caption: TextStyle = TextStyle(
            letterSpacing = 0.4.sp,
            fontSize = 13.sp,
            lineHeight = 15.sp
        ),

        val overline: TextStyle = TextStyle(
            letterSpacing = 0.4.sp,
            fontSize = 11.sp
        ),

        val button: TextStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            lineHeight = 16.sp
        ),

        val material: Typography = Typography(
            bodyLarge = code,
            bodyMedium = input,
            bodySmall = caption,
            labelMedium = overline,
            labelLarge = button
        )
    )
}
