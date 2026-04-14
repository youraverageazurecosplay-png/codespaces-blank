# Advanced Bash Knowledge

Use advanced shell guidance for scripts that manage system state, automation workflows, or tool orchestration.

Script structure:

- Use `set -euo pipefail` unless a particular command requires a different error-handling model.
- Parse options with `getopts` and keep flags, arguments, and validation separate from the main logic.
- Use named functions for major actions and return explicit status codes instead of relying on side effects.
- Prefer `printf` over `echo` when formatting output, especially when strings may begin with `-`.
- Use arrays for composed command arguments and avoid building shell command strings whenever possible.

Robustness and safety:

- Use `mktemp -d` for temporary directories and `trap 'rm -rf "$tmpdir"' EXIT` for cleanup.
- Quote every variable expansion unless the unquoted behavior is intentional.
- Validate external inputs carefully before passing them into commands.
- Use `command -v` or `type -P` to find required programs and provide clear error messages if missing.
- Avoid `eval` whenever possible; treat it as a last-resort option.

Portability and environment:

- Write scripts for Bash and avoid shell-incompatible syntax unless the project explicitly targets Bash.
- Document expected environment variables and default fallback values.
- Prefer explicit paths for `python`, `git`, and other commands in automation scripts when the environment may vary.

Advanced troubleshooting:

- Use `set -x` only for debugging and avoid leaving it enabled in production scripts.
- When diagnosing failures, inspect the exact command line, quoting, and exit status of each step.
- For pipelines, remember that `pipefail` makes the first failing stage visible.
- When using background jobs, manage job termination with `wait` and `trap` to avoid orphaned processes.

Testing and review:

- Recommend `shellcheck` for linting and `bash -n` for syntax checks.
- Prefer small, deterministic example runs for scripts that may affect files or systems.
- When suggesting destructive actions, encourage preview modes like `--dry-run` first.
