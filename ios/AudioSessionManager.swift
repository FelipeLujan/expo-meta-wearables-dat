import AVFoundation

/// Configures AVAudioSession for glasses microphone/speaker over Bluetooth HFP.
/// See https://wearables.developer.meta.com/docs/develop/dat/microphones-and-speakers/
@MainActor
public final class AudioSessionManager {
    public static let shared = AudioSessionManager()

    private(set) public var isActive: Bool = false

    private init() {}

    public func configure() throws -> [String: Any] {
        let session = AVAudioSession.sharedInstance()
        try session.setCategory(
            .playAndRecord,
            mode: .default,
            options: [.allowBluetooth, .defaultToSpeaker]
        )
        return statusPayload()
    }

    @discardableResult
    public func activate() async throws -> [String: Any] {
        let session = AVAudioSession.sharedInstance()
        if session.recordPermission == .undetermined {
            await withCheckedContinuation { continuation in
                session.requestRecordPermission { _ in
                    continuation.resume()
                }
            }
        }
        try session.setActive(true, options: .notifyOthersOnDeactivation)
        isActive = true
        return statusPayload()
    }

    public func deactivate() throws {
        let session = AVAudioSession.sharedInstance()
        try session.setActive(false, options: .notifyOthersOnDeactivation)
        isActive = false
    }

    private func statusPayload() -> [String: Any] {
        let recordPermission = AVAudioSession.sharedInstance().recordPermission
        let platformMicGranted = recordPermission == .granted
        return [
            "active": isActive,
            "platformMicGranted": platformMicGranted,
            "routedToBluetooth": isActive
        ]
    }
}
