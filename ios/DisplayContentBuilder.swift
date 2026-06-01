import Foundation
import MWDATDisplay

public typealias DisplayInteractionHandler = (String) -> Void

/// JSON content tree → MWDATDisplay FlexBox (DisplayAccess sample style).
public struct DisplayContentBuilder {

    public static func buildRootFlexBox(
        from node: [String: Any],
        onInteraction: @escaping DisplayInteractionHandler
    ) -> FlexBox? {
        guard node["type"] as? String == "flexBox" else { return nil }
        return buildFlexBox(node: node, onInteraction: onInteraction)
    }

    // MARK: - Bridge parsing

    static func dictionary(from value: Any) -> [String: Any]? {
        if let dict = value as? [String: Any] { return dict }
        if let dict = value as? [String: Any?] { return dict.compactMapValues { $0 } }
        if let dict = value as? NSDictionary {
            var result: [String: Any] = [:]
            for (key, val) in dict where key is String {
                result[key as! String] = val
            }
            return result.isEmpty ? nil : result
        }
        return nil
    }

    static func childNodes(from node: [String: Any]) -> [[String: Any]] {
        guard let raw = node["children"] else { return [] }
        if let direct = raw as? [[String: Any]] { return direct }
        guard let array = raw as? [Any] else { return [] }
        return array.compactMap { dictionary(from: $0) }
    }

    // MARK: - Enum parsing

    private static func parseDirection(_ value: Any?) -> Direction {
        switch value as? String {
        case "row": return .row
        case "columnReverse": return .columnReverse
        case "rowReverse": return .rowReverse
        default: return .column
        }
    }

    private static func parseAlignment(_ value: Any?) -> Alignment {
        switch value as? String {
        case "center": return .center
        case "end": return .end
        case "stretch": return .stretch
        default: return .start
        }
    }

    private static func parseCornerRadius(_ value: Any?) -> CornerRadius {
        switch value as? String {
        case "small": return .small
        case "medium": return .medium
        default: return .none
        }
    }

    // MARK: - Builders

    private static func buildFlexBox(
        node: [String: Any],
        onInteraction: @escaping DisplayInteractionHandler
    ) -> FlexBox? {
        let direction = parseDirection(node["direction"])
        let spacing = CGFloat(number(node["gap"]) ?? 0)
        let alignment = parseAlignment(node["alignment"])
        let crossAlignment = parseAlignment(node["crossAlignment"])
        let wrap = (node["wrap"] as? Bool) ?? false
        let onPressId = node["onPressId"] as? String
        let useCardBackground = (node["background"] as? String) == "card"

        let children: [any ViewComponent] = childNodes(from: node).compactMap {
            buildChild(from: $0, onInteraction: onInteraction)
        }

        guard !children.isEmpty else { return nil }

        var box = FlexBox(
            direction: direction,
            spacing: spacing,
            alignment: alignment,
            crossAlignment: crossAlignment,
            wrap: wrap
        ) {
            for child in children {
                child
            }
        }

        box = applyPadding(to: box, node: node)

        if useCardBackground {
            box = box.background(.card)
        }

        if let pressId = onPressId {
            box = box.onTap { onInteraction(pressId) }
        }

        return box
    }

    private static func buildFlexBoxChild(
        node: [String: Any],
        onInteraction: @escaping DisplayInteractionHandler
    ) -> FlexBox? {
        guard var box = buildFlexBox(node: node, onInteraction: onInteraction) else { return nil }

        if let grow = number(node["flexGrow"]) {
            box = box.flexGrow(grow)
        }
        if let shrink = number(node["flexShrink"]) {
            box = box.flexShrink(shrink)
        }
        if node["alignSelf"] != nil {
            box = box.alignSelf(parseAlignment(node["alignSelf"]))
        }

        return box
    }

    private static func applyPadding(to box: FlexBox, node: [String: Any]) -> FlexBox {
        let top = number(node["paddingTop"])
        let bottom = number(node["paddingBottom"])
        let start = number(node["paddingStart"])
        let end = number(node["paddingEnd"])
        let hasPerEdge = top != nil || bottom != nil || start != nil || end != nil

        if hasPerEdge {
            var result = box
            if let top { result = result.padding(.top, CGFloat(top)) }
            if let bottom { result = result.padding(.bottom, CGFloat(bottom)) }
            if let start { result = result.padding(.leading, CGFloat(start)) }
            if let end { result = result.padding(.trailing, CGFloat(end)) }
            return result
        }

        if let all = number(node["paddingAll"]) {
            return box.padding(CGFloat(all))
        }

        return box
    }

    private static func buildChild(
        from node: [String: Any],
        onInteraction: @escaping DisplayInteractionHandler
    ) -> (any ViewComponent)? {
        switch node["type"] as? String {
        case "flexBox":
            return buildFlexBoxChild(node: node, onInteraction: onInteraction)
        case "text":
            guard let content = node["content"] as? String else { return nil }
            let style: TextStyle
            switch node["style"] as? String {
            case "heading": style = .heading
            case "meta": style = .meta
            default: style = .body
            }
            let color: TextColor = (node["color"] as? String) == "secondary" ? .secondary : .primary
            return MWDATDisplay.Text(content, style: style, color: color)
        case "button":
            guard let label = node["label"] as? String,
                  let onPressId = node["onPressId"] as? String else { return nil }
            let style: ButtonStyle
            switch node["style"] as? String {
            case "secondary": style = .secondary
            case "outline": style = .outline
            default: style = .primary
            }
            if let iconRaw = node["iconName"] as? String,
               let iconName = IconName(rawValue: iconRaw) {
                return MWDATDisplay.Button(label: label, style: style, iconName: iconName) {
                    onInteraction(onPressId)
                }
            }
            return MWDATDisplay.Button(label: label, style: style) { onInteraction(onPressId) }
        case "image":
            guard let uri = node["uri"] as? String else { return nil }
            let size: ImageSize = (node["sizePreset"] as? String) == "icon" ? .icon : .fill
            let cornerRadius = parseCornerRadius(node["cornerRadius"])
            return MWDATDisplay.Image(uri: uri, sizePreset: size, cornerRadius: cornerRadius)
        case "icon":
            guard let name = node["name"] as? String else { return nil }
            let iconStyle: IconStyle = (node["style"] as? String) == "outline" ? .outline : .filled
            let iconName = IconName(rawValue: name) ?? .checkmarkCircle
            return MWDATDisplay.Icon(name: iconName, style: iconStyle)
        default:
            return nil
        }
    }

    private static func number(_ value: Any?) -> Float? {
        switch value {
        case let n as NSNumber: return n.floatValue
        case let i as Int: return Float(i)
        case let d as Double: return Float(d)
        case let f as Float: return f
        default: return nil
        }
    }
}
