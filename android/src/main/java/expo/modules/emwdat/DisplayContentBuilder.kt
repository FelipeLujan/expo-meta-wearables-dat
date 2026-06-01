package expo.modules.emwdat

import com.meta.wearable.dat.display.views.Alignment
import com.meta.wearable.dat.display.views.ButtonStyle
import com.meta.wearable.dat.display.views.ContentScope
import com.meta.wearable.dat.display.views.CornerRadius
import com.meta.wearable.dat.display.views.Direction
import com.meta.wearable.dat.display.views.FlexBoxBackground
import com.meta.wearable.dat.display.views.FlexBoxScope
import com.meta.wearable.dat.display.views.IconName
import com.meta.wearable.dat.display.views.IconStyle
import com.meta.wearable.dat.display.views.ImageSizePreset
import com.meta.wearable.dat.display.views.TextColor
import com.meta.wearable.dat.display.views.TextStyle

typealias InteractionHandler = (String) -> Unit

object DisplayContentBuilder {

    fun build(scope: ContentScope, node: Map<String, Any>, onInteraction: InteractionHandler) {
        buildFlexBox(scope, node, onInteraction, isRoot = true)
    }

    private fun buildNode(
        scope: FlexBoxScope,
        node: Map<String, Any>,
        onInteraction: InteractionHandler
    ) {
        when (node["type"] as? String) {
            "flexBox" -> buildFlexBox(scope, node, onInteraction, isRoot = false)
            "text" -> buildText(scope, node)
            "button" -> buildButton(scope, node, onInteraction)
            "image" -> buildImage(scope, node)
            "icon" -> buildIcon(scope, node)
        }
    }

    private fun buildFlexBox(
        scope: Any,
        node: Map<String, Any>,
        onInteraction: InteractionHandler,
        isRoot: Boolean
    ) {
        val direction = parseDirection(node["direction"])
        val gap = (node["gap"] as? Number)?.toInt() ?: 0
        val alignment = parseAlignment(node["alignment"])
        val crossAlignment = parseAlignment(node["crossAlignment"])
        val wrap = (node["wrap"] as? Boolean) ?: false
        val onPressId = node["onPressId"] as? String
        val background = if ((node["background"] as? String) == "card") {
            FlexBoxBackground.CARD
        } else {
            FlexBoxBackground.NONE
        }
        val flexGrow = (node["flexGrow"] as? Number)?.toFloat() ?: 0f
        val flexShrink = (node["flexShrink"] as? Number)?.toFloat() ?: 0f
        val alignSelf = (node["alignSelf"] as? String)?.let { parseAlignment(it) }

        val paddingAll = (node["paddingAll"] as? Number)?.toInt() ?: 0
        val paddingTop = (node["paddingTop"] as? Number)?.toInt()
        val paddingBottom = (node["paddingBottom"] as? Number)?.toInt()
        val paddingStart = (node["paddingStart"] as? Number)?.toInt()
        val paddingEnd = (node["paddingEnd"] as? Number)?.toInt()

        @Suppress("UNCHECKED_CAST")
        val children = (node["children"] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()

        val clickHandler: (() -> Unit)? = onPressId?.let { id -> { onInteraction(id) } }

        val block: FlexBoxScope.() -> Unit = {
            for (child in children) {
                buildNode(this, child, onInteraction)
            }
        }

        if (isRoot) {
            (scope as ContentScope).flexBox(
                direction = direction,
                gap = gap,
                alignment = alignment,
                crossAlignment = crossAlignment,
                wrap = wrap,
                padding = paddingAll,
                paddingTop = paddingTop,
                paddingBottom = paddingBottom,
                paddingStart = paddingStart,
                paddingEnd = paddingEnd,
                background = background,
                onClick = clickHandler,
                block = block
            )
        } else {
            (scope as FlexBoxScope).flexBox(
                direction = direction,
                gap = gap,
                alignment = alignment,
                crossAlignment = crossAlignment,
                wrap = wrap,
                padding = paddingAll,
                paddingTop = paddingTop,
                paddingBottom = paddingBottom,
                paddingStart = paddingStart,
                paddingEnd = paddingEnd,
                background = background,
                onClick = clickHandler,
                flexGrow = flexGrow,
                flexShrink = flexShrink,
                alignSelf = alignSelf,
                block = block
            )
        }
    }

    private fun buildText(scope: FlexBoxScope, node: Map<String, Any>) {
        val content = node["content"] as? String ?: return
        val style = when (node["style"] as? String) {
            "heading" -> TextStyle.HEADING
            "meta" -> TextStyle.META
            else -> TextStyle.BODY
        }
        val color = when (node["color"] as? String) {
            "secondary" -> TextColor.SECONDARY
            else -> TextColor.PRIMARY
        }
        scope.text(content, style = style, color = color)
    }

    private fun buildButton(scope: FlexBoxScope, node: Map<String, Any>, onInteraction: InteractionHandler) {
        val label = node["label"] as? String ?: return
        val onPressId = node["onPressId"] as? String ?: return
        val style = when (node["style"] as? String) {
            "secondary" -> ButtonStyle.SECONDARY
            "outline" -> ButtonStyle.OUTLINE
            else -> ButtonStyle.PRIMARY
        }
        val iconName = (node["iconName"] as? String)?.let { parseIconName(it) }
        if (iconName != null) {
            scope.button(
                label = label,
                style = style,
                iconName = iconName,
                onClick = { onInteraction(onPressId) }
            )
        } else {
            scope.button(label = label, style = style, onClick = { onInteraction(onPressId) })
        }
    }

    private fun buildImage(scope: FlexBoxScope, node: Map<String, Any>) {
        val uri = node["uri"] as? String ?: return
        val sizePreset = when (node["sizePreset"] as? String) {
            "icon" -> ImageSizePreset.ICON
            else -> ImageSizePreset.FILL
        }
        val cornerRadius = when (node["cornerRadius"] as? String) {
            "small" -> CornerRadius.SMALL
            "medium" -> CornerRadius.MEDIUM
            else -> CornerRadius.NONE
        }
        scope.image(uri = uri, sizePreset = sizePreset, cornerRadius = cornerRadius)
    }

    private fun buildIcon(scope: FlexBoxScope, node: Map<String, Any>) {
        val name = node["name"] as? String ?: return
        val iconName = parseIconName(name)
        val style = when (node["style"] as? String) {
            "outline" -> IconStyle.OUTLINE
            else -> IconStyle.FILLED
        }
        scope.icon(name = iconName, style = style)
    }

    private fun parseDirection(value: Any?): Direction {
        return when (value as? String) {
            "row" -> Direction.ROW
            "columnReverse" -> Direction.COLUMN_REVERSE
            "rowReverse" -> Direction.ROW_REVERSE
            else -> Direction.COLUMN
        }
    }

    private fun parseAlignment(value: String?): Alignment {
        return when (value) {
            "center" -> Alignment.CENTER
            "end" -> Alignment.END
            "stretch" -> Alignment.STRETCH
            else -> Alignment.START
        }
    }

    /** Accepts camelCase (iOS) or UPPER_SNAKE (Android) icon names from JS. */
    private fun parseIconName(name: String): IconName {
        val upperSnake = name
            .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
            .uppercase()
        return try {
            IconName.valueOf(upperSnake)
        } catch (e: IllegalArgumentException) {
            IconName.CHECKMARK_CIRCLE
        }
    }
}
