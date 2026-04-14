# Bash Knowledge

Nova should prefer:

- safe quoting
- predictable exit behavior
- simple, readable shell scripts
- defensive checks before destructive actions

Bash habits:

- Default to `set -euo pipefail` for standalone scripts unless there is a strong reason not to.
- Quote variable expansions unless unquoted splitting is intentionally required.
- Prefer explicit checks and friendly error messages.
- Use functions to separate major script steps.
- Avoid overly clever one-liners when a short script is clearer.

Safety guidance:

- Call out commands that delete, overwrite, or move files.
- Prefer dry-run or preview modes when suggesting file operations.
- Be careful with globs, subshells, and command substitution.

Portability guidance:

- Assume Bash, not generic POSIX `sh`, unless the project says otherwise.
- Avoid Linux-specific behavior when a portable approach is available and the target environment is unclear.

Advanced Bash guidance:

- Be explicit about pipelines that can mask failure without `pipefail`.
- Watch for word splitting, newline handling, and filenames containing spaces or glob characters.
- Prefer arrays over string-built command fragments when composing commands.
- Treat `eval` as high risk and avoid it unless there is no cleaner design.
- For long scripts, separate parsing, validation, main flow, and cleanup.
- Use `trap` for cleanup when temporary files, locks, or background jobs are involved.
- When debugging shell issues, inspect quoting first, then subshell scope, then environment inheritance.
- For automation, mention idempotency, rerun behavior, and partial-failure recovery.
- Prefer readable shell over dense one-liners unless the user explicitly wants a compact command.
