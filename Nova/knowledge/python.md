# Python Knowledge

Nova should prefer:

- Python 3.11+ syntax when useful
- clear functions and readable modules
- type hints on public functions
- practical tests for logic-heavy code
- traceback-first debugging

Python habits:

- Prefer `pathlib` over manual path string handling.
- Use dataclasses for lightweight structured state when they fit.
- Keep side effects near the edge of the program.
- For CLIs, prefer `argparse` unless the project already uses another framework.
- For file IO, handle encoding explicitly when reasonable.

Debugging guidance:

- When a traceback is available, reason from the deepest relevant frame first.
- Suggest the smallest fix that matches the observed failure.
- If imports fail, check project structure and execution path before proposing bigger changes.

Quality guidance:

- Add tests when behavior is easy to isolate.
- Avoid adding dependencies unless they clearly simplify the solution.
- Match existing formatting and naming patterns inside the repo.

Advanced Python guidance:

- Use `asyncio` deliberately. Prefer async only where concurrency or IO overlap matters.
- Be careful with shared mutable state across coroutines, threads, and callbacks.
- For larger systems, separate orchestration, domain logic, and IO boundaries.
- Prefer explicit protocols, typed dataclasses, or small interfaces over giant utility modules.
- When optimizing, identify the actual bottleneck before changing algorithms or adding caching.
- Distinguish CPU-bound from IO-bound work before recommending threads, processes, or async.
- For packaging issues, inspect entry points, import paths, editable installs, and working directory assumptions.
- For debugging memory or lifecycle bugs, watch file handles, background tasks, circular references, and long-lived caches.
- For testing, isolate side effects with fixtures, temp directories, and narrow mocks rather than mocking everything.
- When proposing architecture, prefer something a solo developer can still maintain.
