# VS Code Side Panel Setup Guide

This guide walks you through setting up and using Nova's VS Code side panel extension.

## Prerequisites

- VS Code 1.80 or later
- Node.js and npm (for extension development)
- Python 3.11+ with Nova running locally

## Quick Start

### 1. Enable the VS Code Extension

Open the `Nova/vscode-nova` folder in VS Code:

```bash
code Nova/vscode-nova
```

### 2. Install Dependencies (if needed)

```bash
cd Nova/vscode-nova
npm install
```

### 3. Test the Extension

In VS Code within `vscode-nova`, press **F5** to start the extension in debug mode. This opens a new VS Code window with the extension enabled.

### 4. Open Nova Side Panel

In the main VS Code window (or the debug window):

1. Open the command palette: **Ctrl+Shift+P** (or **Cmd+Shift+P** on Mac)
2. Type: `Nova: Open Side Panel`
3. Press Enter

A new panel opens on the right side of your editor.

### 5. Use Nova from the Side Panel

- Type your question in the text area
- Click **Ask Nova** to send it
- Nova runs `python -m nova_ai.cli ask <prompt>` in your current workspace
- The response appears in the panel

## Example Prompts

**Python:**
```
Help me write a clean async function that fetches data from multiple URLs
```

**Bash:**
```
Write a safe backup script with error handling for system files
```

**Minecraft Modding:**
```
How should I register a custom item in a Fabric 1.20 mod?
```

## Troubleshooting

### "Nova not found" error

The extension looks for `python` or `python3` in your PATH. Ensure Nova's Python environment is active:

```bash
# Test if Nova CLI is available
python -m nova_ai.cli --help
```

If Python is in a virtual environment, activate it before running VS Code:

```bash
source venv/bin/activate  # or equivalent for your shell
code Nova/vscode-nova
```

### Extension doesn't load

1. Check the debug console in VS Code (Ctrl+Shift+Y)
2. Look for activation errors or missing dependencies
3. Try reloading: **Developer: Reload Window** (Ctrl+Shift+P)

### Response times are slow

Nova waits for Ollama and the model to respond. If prompts take more than 30 seconds:

1. Ensure Ollama is running: `ollama serve`
2. Verify the model is loaded: `ollama list`
3. Check for network or system resource issues

## Extending the Extension

The extension's entry point is `extension.js`. To modify:

- **Add more commands**: Add entries to `contributes.commands` in `package.json`
- **Change the UI**: Edit the HTML in `getWebviewContent()`
- **Modify behavior**: Edit the `activate()` function

After changes, press **Ctrl+Shift+P** > **Developer: Reload Window** to test.

## For Developers

If you want to package and distribute the extension:

1. Install `vsce`: `npm install -g vsce`
2. Run: `vsce package`
3. This creates a `.vsix` file that can be installed with `code --install-extension <file.vsix>`

For more details, see the [VS Code Extension Development Guide](https://code.visualstudio.com/api).
