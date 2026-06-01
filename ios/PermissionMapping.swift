import MWDATCore

/// String ↔ SDK permission mapping (nonisolated — safe to call from Expo module handlers).
enum EMWDATPermissionMapping {
    static func sdkPermission(from name: String) -> Permission? {
        switch name {
        case "camera": return .camera
        case "microphone": return .microphone
        default: return nil
        }
    }

    static func permissionName(_ permission: Permission) -> String {
        switch permission {
        case .camera: return "camera"
        case .microphone: return "microphone"
        @unknown default: return "unknown"
        }
    }
}
