import Foundation

/// Thread-safe observable value. `stream()` replays the current value, then every update.
/// Backs the in-memory repositories; a real implementation would be fed by the network/database.
public final class AsyncStore<Value: Sendable>: @unchecked Sendable {
    private let lock = NSLock()
    private var current: Value
    private var continuations: [UUID: AsyncStream<Value>.Continuation] = [:]

    public init(_ initial: Value) { current = initial }

    public var value: Value {
        lock.lock(); defer { lock.unlock() }
        return current
    }

    public func set(_ newValue: Value) { update { $0 = newValue } }

    public func update(_ change: (inout Value) -> Void) {
        lock.lock()
        change(&current)
        let snapshot = current
        let listeners = Array(continuations.values)
        lock.unlock()
        for listener in listeners { listener.yield(snapshot) }
    }

    public func stream() -> AsyncStream<Value> {
        AsyncStream { continuation in
            let id = UUID()
            lock.lock()
            continuations[id] = continuation
            let snapshot = current
            lock.unlock()
            continuation.yield(snapshot)
            continuation.onTermination = { [weak self] _ in self?.remove(id) }
        }
    }

    private func remove(_ id: UUID) {
        lock.lock(); continuations[id] = nil; lock.unlock()
    }
}
