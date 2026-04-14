# Advanced Python Knowledge

Use advanced Python guidance only when the repository shows nontrivial application structure, packaging, or concurrency.

Architectural guidance:

- Separate core business logic from IO, CLI, and integration entry points.
- Favor small, composable functions and explicit abstractions over one large utility module.
- Keep state ownership clear: a function either returns data or mutates a well-defined object.
- Prefer explicit protocols, typed dataclasses, or small interface objects for replaceable services.
- Use dependency injection for testable code paths and reduce hidden global state.

Concurrency and async:

- Decide whether the task is CPU-bound or IO-bound before recommending `threading`, `multiprocessing`, or `asyncio`.
- Use `asyncio` only when multiple IO operations naturally overlap or when the code already has an event loop.
- Avoid shared mutable state across coroutines unless it is protected by clear synchronization.
- Prefer `asyncio.run()` and cancel tasks cleanly with `asyncio.CancelledError` handlers.
- For thread-safe access, use `queue.Queue`, `threading.Lock`, or a separate worker process instead of racing on globals.

Types and validation:

- Add type hints on public APIs and use `typing.Protocol` for broader interface compatibility.
- Prefer explicit runtime validation for boundary inputs, especially from CLI arguments, file contents, or network sources.
- Avoid overly broad `except Exception:` handlers; use narrow exception catches and re-raise or log context.

Packaging and imports:

- Inspect `pyproject.toml`, `setup.py`, and the package layout when reasoning about import errors.
- Prefer relative imports within a package and absolute imports for top-level modules.
- When adding CLI entry points, keep `argparse` or `click` usage explicit and avoid hidden assumptions about `cwd`.

Performance and debugging:

- Only optimize after identifying a real bottleneck; prefer clarity over micro-optimizations.
- For performance changes, mention whether the benefit is due to algorithmic improvement, I/O batching, or concurrency.
- When debugging memory or lifecycle bugs, watch file handles, background tasks, weak references, and long-lived caches.
- Use `tracemalloc`, `cProfile`, or `memory_profiler` only as investigative tools, not as default recommendations.

Testing and maintainability:

- Suggest tests that isolate side effects with temporary directories and mock only external boundaries.
- Prefer property-based or data-driven tests when the logic has many edge cases.
- When adding helper functions, also add focused regression tests for the smallest behavior.
