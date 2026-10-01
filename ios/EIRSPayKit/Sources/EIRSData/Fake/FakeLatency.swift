import Foundation

/// Simulated network latency for the in-memory repositories, matching the prototype loader (1.8s).
/// Set `milliseconds` to 0 in tests.
public enum FakeLatency {
    public static var milliseconds: UInt64 = 1800

    public static func wait() async {
        guard milliseconds > 0 else { return }
        try? await Task.sleep(nanoseconds: milliseconds * 1_000_000)
    }
}

/// Deterministic pseudo-IRN (64 hex chars). The real IRN comes back from MoR on registration.
public enum FakeIrn {
    public static func of(_ seed: String) -> String {
        var s = 0
        for scalar in seed.unicodeScalars { s = (s * 31 + Int(scalar.value)) % 233_280 }
        let digits = Array("0123456789abcdef")
        var out = ""
        for _ in 0..<64 {
            s = (s * 9301 + 49297) % 233_280
            out.append(digits[s * 16 / 233_280])
        }
        return out
    }
}
