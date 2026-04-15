# Nova AI - Usage Guide

Nova AI is a local-first coding assistant. Use it via terminal or VS Code.

## Terminal Commands

### Interactive Chat (with learning)
```bash
nova chat
```
Opens an interactive terminal where Nova learns from your full conversation.

**Commands in chat:**
- `/help` - Show all available commands
- `/files` - List files in the current repo
- `/read <path>` - Read a file
- `/search <text>` - Search text in repo
- `/run <command>` - Run a shell command with approval
- `/repo <path>` - Switch to another repository
- `/model <name>` - Switch Ollama model
- `/profile <name>` - Switch profile (general, python, bash, minecraft)
- `/credits` - View credit status
- `/quit` - Exit chat

### Ask a Single Question
```bash
nova ask "What does this function do?"
```

### Web GUI
```bash
nova web
```
Opens a web interface at `http://localhost:5000`. Great for offline use.

### Open in VS Code
```bash
nova open
```
Opens VS Code with the Nova extension side panel.

### View Configuration
```bash
nova info
```

### Manage Credits
```bash
nova credits status                 # View credits
nova credits add 100                # Add credits (requires admin password)
nova credits enable/disable         # Enable or disable credit system
nova credits set-admin-password     # Set admin password
```

## VS Code Extension

### Activity Bar Button
1. Look for the **wand icon** (✨) in the left activity bar
2. Click it to open the Nova view
3. Click the button to open the side panel

### Command Palette
1. Press `Ctrl+Shift+P` (or `Cmd+Shift+P` on Mac)
2. Type "Nova" or search for `nova.openSidePanel`
3. Press Enter

### Editor Title Bar
- Look for the **wand icon** in the top-right of the editor
- Click it to open Nova directly

## Features

- **Offline**: Works with local Ollama models
- **Repository-aware**: Understands your code context
- **Learning**: Chat mode learns from conversation history
- **Profiles**: Different personalities for different tasks
- **Credit System**: Optional usage limits with admin password protection
- **Web GUI**: Access from any browser
- **Terminal Integration**: Full CLI support

## Getting Started

1. **Ensure Ollama is running:**
   ```bash
   ollama serve
   ```

2. **Install a model:**
   ```bash
   ollama pull qwen2.5-coder:7b    # Recommended
   # Or smaller/faster option:
   ollama pull qwen2.5-coder:1.5b
   ```

3. **Start using Nova:**
   ```bash
   nova chat                        # Interactive terminal
   nova web                         # Web interface
   nova open                        # VS Code extension
   ```

## Access Passwords

For security, you can use access passwords:

```bash
nova --access-password "mypassword" chat
nova --access-password "mypassword" ask "hello"
```

Or set an admin password for credit management:
```bash
nova credits set-admin-password
```

## Profiles

Choose different profiles for different tasks:

- `general` - General-purpose coding assistance
- `python` - Python-specific guidance
- `bash` - Shell scripting help
- `minecraft` - Minecraft modding (Fabric)

Use with:
```bash
nova --profile python ask "How do I use decorators?"
nova --profile minecraft chat
```
