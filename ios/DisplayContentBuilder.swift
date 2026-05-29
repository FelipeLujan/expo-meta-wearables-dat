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

    // MARK: - Builders

    private static func buildFlexBox(
        node: [String: Any],
        onInteraction: @escaping DisplayInteractionHandler
    ) -> FlexBox? {
        let direction: Direction = (node["direction"] as? String) == "row" ? .row : .column
        let spacing = CGFloat(number(node["gap"]) ?? 12)
        let padding = CGFloat(number(node["paddingAll"]) ?? 24)
        let onPressId = node["onPressId"] as? String

        let children: [any ViewComponent] = childNodes(from: node).compactMap {
            buildChild(from: $0, onInteraction: onInteraction)
        }

        guard !children.isEmpty else { return nil }

        var box = FlexBox(direction: direction, spacing: spacing) {
            for child in children {
                child
            }
        }
        .padding(padding)
        .background(.card)

        if let pressId = onPressId {
            box = box.onTap { onInteraction(pressId) }
        }

        return box
    }

    private static func buildChild(
        from node: [String: Any],
        onInteraction: @escaping DisplayInteractionHandler
    ) -> (any ViewComponent)? {
        switch node["type"] as? String {
        case "flexBox":
            return buildFlexBox(node: node, onInteraction: onInteraction)
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
            return MWDATDisplay.Button(label: label, style: style) { onInteraction(onPressId) }
        case "image":
            guard let uri = node["uri"] as? String else { return nil }
            let size: ImageSize = (node["sizePreset"] as? String) == "icon" ? .icon : .fill
            return MWDATDisplay.Image(uri: uri, sizePreset: size)
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
