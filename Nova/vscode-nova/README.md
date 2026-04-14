# Nova Side Panel

This VS Code extension opens Nova in a side panel and forwards prompts to the local Nova CLI.

## Usage

1. Open the `Nova/vscode-nova` folder in VS Code.
2. Run `Developer: Reload Window` if needed.
3. Open the command palette and choose `Nova: Open Side Panel`.
4. Enter a prompt and click `Ask Nova`.

## Notes

- The extension uses `python` or `python3` from your PATH.
- It runs `python -m nova_ai.cli ask <prompt>` in the current workspace folder.
- For best results, open your Nova repository as the active workspace.
