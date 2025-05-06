package net.akehurst.ide.gui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import net.akehurst.language.editor.compose.EditorIcons
import net.akehurst.language.issues.api.LanguageIssueKind

object GuiIcons {

    private var _Close: ImageVector? = null
    public val Close: ImageVector
        get() {
            if (_Close != null) {
                return _Close!!
            }
            _Close = ImageVector.Builder(
                name = "Close",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1.0f,
                    stroke = null,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(256f, 760f)
                    lineToRelative(-56f, -56f)
                    lineToRelative(224f, -224f)
                    lineToRelative(-224f, -224f)
                    lineToRelative(56f, -56f)
                    lineToRelative(224f, 224f)
                    lineToRelative(224f, -224f)
                    lineToRelative(56f, 56f)
                    lineToRelative(-224f, 224f)
                    lineToRelative(224f, 224f)
                    lineToRelative(-56f, 56f)
                    lineToRelative(-224f, -224f)
                    close()
                }
            }.build()
            return _Close!!
        }

    private var _Circle: ImageVector? = null
    public val Circle: ImageVector
        get() {
            if (_Circle != null) {
                return _Circle!!
            }
            _Circle = ImageVector.Builder(
                name = "Circle",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f
            ).apply {
                path(
                    fill = SolidColor(Color.Black),
                    fillAlpha = 1.0f,
                    stroke = null,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(480f, 880f)
                    quadToRelative(-83f, 0f, -156f, -31.5f)
                    reflectiveQuadTo(197f, 763f)
                    reflectiveQuadToRelative(-85.5f, -127f)
                    reflectiveQuadTo(80f, 480f)
                    reflectiveQuadToRelative(31.5f, -156f)
                    reflectiveQuadTo(197f, 197f)
                    reflectiveQuadToRelative(127f, -85.5f)
                    reflectiveQuadTo(480f, 80f)
                    reflectiveQuadToRelative(156f, 31.5f)
                    reflectiveQuadTo(763f, 197f)
                    reflectiveQuadToRelative(85.5f, 127f)
                    reflectiveQuadTo(880f, 480f)
                    reflectiveQuadToRelative(-31.5f, 156f)
                    reflectiveQuadTo(763f, 763f)
                    reflectiveQuadToRelative(-127f, 85.5f)
                    reflectiveQuadTo(480f, 880f)
                    moveToRelative(0f, -80f)
                    quadToRelative(134f, 0f, 227f, -93f)
                    reflectiveQuadToRelative(93f, -227f)
                    reflectiveQuadToRelative(-93f, -227f)
                    reflectiveQuadToRelative(-227f, -93f)
                    reflectiveQuadToRelative(-227f, 93f)
                    reflectiveQuadToRelative(-93f, 227f)
                    reflectiveQuadToRelative(93f, 227f)
                    reflectiveQuadToRelative(227f, 93f)
                    moveToRelative(0f, -320f)
                }
            }.build()
            return _Circle!!
        }

}

@Composable
fun ColumnScope.tabContent(text: String, isDirty: Boolean, issueMarker: LanguageIssueKind?, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        when (issueMarker) {
            null -> Spacer(Modifier.width(15.dp))
            LanguageIssueKind.INFORMATION -> Icon(
                imageVector = EditorIcons.Information,
                contentDescription = null,
                tint = Color.Blue,
                modifier = Modifier.size(15.dp)
            )

            LanguageIssueKind.WARNING -> Icon(
                imageVector = EditorIcons.Warning,
                contentDescription = null,
                tint = EditorIcons.ORANGE,
                modifier = Modifier.size(15.dp)
            )

            LanguageIssueKind.ERROR -> Icon(
                imageVector = EditorIcons.Error,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = text,
            modifier = Modifier
        )
        Icon(
            imageVector = GuiIcons.Close,
            contentDescription = null,
            tint = when (isDirty) {
                true -> Color.Red
                false -> Color.Green
            },
            modifier = Modifier
                .clickable { onClose() }
                .size(15.dp)
        )
    }
}