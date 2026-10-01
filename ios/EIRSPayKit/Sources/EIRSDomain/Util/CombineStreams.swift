import Foundation

/// Emits a transformed value every time either input stream emits (after both have emitted once).
/// Plain Swift concurrency, so the domain layer needs neither Combine nor extra packages.
public func combineLatest2<A: Sendable, B: Sendable, R: Sendable>(
    _ a: AsyncStream<A>, _ b: AsyncStream<B>, _ transform: @escaping @Sendable (A, B) -> R
) -> AsyncStream<R> {
    AsyncStream { continuation in
        let state = CombineState<A, B>()
        let task = Task {
            await withTaskGroup(of: Void.self) { group in
                group.addTask {
                    for await value in a {
                        if Task.isCancelled { return }
                        if let out = state.setA(value, transform: transform) { continuation.yield(out) }
                    }
                }
                group.addTask {
                    for await value in b {
                        if Task.isCancelled { return }
                        if let out = state.setB(value, transform: transform) { continuation.yield(out) }
                    }
                }
            }
            continuation.finish()
        }
        continuation.onTermination = { _ in task.cancel() }
    }
}

public func combineLatest3<A: Sendable, B: Sendable, C: Sendable, R: Sendable>(
    _ a: AsyncStream<A>, _ b: AsyncStream<B>, _ c: AsyncStream<C>, _ transform: @escaping @Sendable (A, B, C) -> R
) -> AsyncStream<R> {
    let ab: AsyncStream<(A, B)> = combineLatest2(a, b) { ($0, $1) }
    return combineLatest2(ab, c) { transform($0.0, $0.1, $1) }
}

/// Holds the latest value of each input, guarded by a lock.
private final class CombineState<A: Sendable, B: Sendable>: @unchecked Sendable {
    private let lock = NSLock()
    private var a: A?
    private var b: B?

    func setA<R>(_ value: A, transform: (A, B) -> R) -> R? {
        lock.lock(); defer { lock.unlock() }
        a = value
        guard let a, let b else { return nil }
        return transform(a, b)
    }

    func setB<R>(_ value: B, transform: (A, B) -> R) -> R? {
        lock.lock(); defer { lock.unlock() }
        b = value
        guard let a, let b else { return nil }
        return transform(a, b)
    }
}
