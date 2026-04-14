# 🚀 Nova AI - Complete Setup & Usage Guide

## What is Nova?

Nova AI is a **local-first coding assistant** that runs entirely on your machine using Ollama. It's like GitHub Copilot Chat but self-hosted and offline.

## ✅ One-Time Setup

```bash
cd Nova
./install.sh
```

This will:
- ✅ Install Ollama (if not present)
- ✅ Auto-start Ollama  
- ✅ Install Nova AI dependencies
- ✅ Download the model (qwen2.5-coder:7b - 4.7 GB)
- ✅ Verify everything is working

## 🎯 Three Ways to Use Nova

### **1. VS Code Side Panel** (Recommended) 🎨
Open with **Cmd+Shift+N** (Mac) or **Ctrl+Shift+N** (Windows/Linux)
- Just like GitHub Copilot Chat
- Full conversation history
- Integrated with your editor

**Setup:**
```bash
cd Nova/vscode-nova
npm install
# Then press F5 in VS Code to run in debug mode
```

### **2. Web Browser GUI** 🌐
```bash
nova web --port 5000
# Open: http://127.0.0.1:5000
```
- Beautiful chat interface
- Works in any browser
- Sidebar with quick commands
- Token bypass support

### **3. Terminal / CLI** 💻
```bash
nova ask "Explain this repository"
nova chat                            # Interactive mode
nova info                            # Show configuration
nova info --verbose                  # Full details
```

## 📚 Available Commands

| Command | Description |
|---------|-------------|
| `nova ask "<question>"` | Single question to Nova |
| `nova chat` | Interactive chat mode |
| `nova create-project <name>` | Create starter projects |
| `nova info` | Show config & system info |
| `nova credits status` | Check token balance |
| `nova credits disable` | Unlimited tokens |
| `nova web` | Start web GUI |

## 🔑 Token / Credit System

Nova uses tokens to track usage. You have **100 free tokens** by default.

### Bypass Tokens (Unlimited)

**In Web GUI:**
1. Click "Bypass Tokens" in sidebar
2. Enter admin password
3. All requests bypass token limits

**Via CLI:**
```bash
nova --access-password "admin" ask "Your question"
```

**Disable entirely:**
```bash
nova credits disable   # Unlimited prompts
```

See [TOKEN_BYPASS.md](TOKEN_BYPASS.md) for full credit system details.

## 🛠️ Profile Selection

Nova has profiles for different coding domains:

```bash
nova --profile python ask "Best way to optimize this?"
nova --profile bash ask "How to make this portable?"
nova --profile minecraft ask "How do I register a block?"
nova --profile general ask "General coding question"
```

## 🐛 Troubleshooting

**"Ollama not running"**
- Run: `ollama serve`
- Or rerun installer: `./install.sh`

**"Model not found"**
- Install: `ollama pull qwen2.5-coder:7b`

**"500 error from Ollama"**
- The model may have crashed
- Check: `curl http://127.0.0.1:11434/api/tags`
- Restart: `ollama serve`

**"Token expired"**
- Use bypass: `--access-password "admin"`
- Or add tokens: `nova credits add 50`

## 🎓 Example Prompts

✨ **Repository Structure**
```
nova ask "Explain the structure of this repository"
```

🐛 **Bug Finding**
```
nova --profile python ask "Find bugs or issues in this code"
```

⚡ **Optimization**
```
nova ask "Optimize this for performance"
```

📝 **Testing**
```
nova ask "Write tests for this module"
```

🏗️ **Architecture**
```
nova ask "What are the main modules here?"
```

## 📖 More Information

- [TUTORIAL.md](TUTORIAL.md) - Getting started guide
- [VSCODE_SETUP.md](VSCODE_SETUP.md) - VS Code extension setup
- [TOKEN_BYPASS.md](TOKEN_BYPASS.md) - Credit/token system details
- [README.md](README.md) - Project overview

## ⚙️ Configuration

All settings are configured via `nova_ai/config.py`:

- **Model:** `qwen2.5-coder:7b` (change with `--model`)
- **Ollama Host:** `http://127.0.0.1:11434` (change with `--host`)
- **Profile:** `general` (change with `--profile`)
- **Repo:** Current directory (change with `--repo`)

## 🤝 Contributing

Found an issue? Want to improve Nova? Contributions welcome!

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

Enjoy using Nova! 🚀
