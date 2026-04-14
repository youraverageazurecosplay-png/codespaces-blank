# VS Code Nova AI Chat Setup

Nova AI has a VS Code side panel extension that works just like GitHub Copilot Chat. Open it with **Cmd+Shift+N** (Mac) or **Ctrl+Shift+N** (Windows/Linux).

## Quick Start

### 1. Open the Extension Folder

```bash
code Nova/vscode-nova
```

### 2. Install Dependencies (first time only)

```bash
cd Nova/vscode-nova
npm install
```

### 3. Run in Debug Mode

Press **F5** in VS Code. This opens a new window with the extension loaded.

### 4. Open Nova Chat

Use the keyboard shortcut:
- **Cmd+Shift+N** (Mac)
- **Ctrl+Shift+N** (Windows/Linux)

Or open the command palette (**Cmd+Shift+P** / **Ctrl+Shift+P**) and search for "Nova: Open Chat".

### 5. Start Chatting!

Type your question about the codebase in the chat box and press **Send** or **Ctrl+Enter**.

## Features

- 💬 Chat interface just like Copilot Chat
- 📁 Ask about repository structure
- 🔍 Get code explanations and suggestions
- 🐛 Find bugs and issues
- ✨ Runs entirely locally with Ollama
- 🎯 Context-aware answers using your repo files

## Troubleshooting

**"Ollama not running"**
- Run `ollama serve` in a terminal to start Ollama

**"Command not found"**
- Make sure you ran `npm install` in the `vscode-nova` folder
- Restart VS Code after installing

**"Extension not loading"**
- Make sure you pressed F5 to start the debug mode
- Check the Debug Console for errors

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
