"""Web GUI server for Nova AI."""

from __future__ import annotations

import json
from pathlib import Path

try:
    from flask import Flask, render_template_string, request, jsonify
except ImportError:
    Flask = None

from .agent import NovaAgent
from .config import NovaConfig


HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nova AI - Local Coding Assistant</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; padding: 20px; }
        .container { max-width: 900px; margin: 0 auto; background: white; border-radius: 16px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); overflow: hidden; display: flex; flex-direction: column; height: 90vh; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px 40px; }
        .header h1 { font-size: 28px; margin-bottom: 5px; }
        .header p { opacity: 0.9; font-size: 14px; }
        .profile-badge { display: inline-block; background: rgba(255,255,255,0.2); padding: 4px 12px; border-radius: 20px; font-size: 12px; margin-top: 10px; }
        .main { display: flex; flex: 1; overflow: hidden; }
        .sidebar { width: 250px; background: #f5f5f5; border-right: 1px solid #e0e0e0; padding: 20px; overflow-y: auto; }
        .chat-area { flex: 1; display: flex; flex-direction: column; }
        .messages { flex: 1; padding: 20px 40px; overflow-y: auto; display: flex; flex-direction: column; gap: 16px; background: #fafafa; }
        .message { display: flex; gap: 12px; animation: slideIn 0.3s ease; }
        @keyframes slideIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
        .message.user { justify-content: flex-end; }
        .message.assistant { justify-content: flex-start; }
        .message-bubble { max-width: 70%; padding: 12px 16px; border-radius: 12px; white-space: pre-wrap; word-break: break-word; line-height: 1.5; }
        .message.user .message-bubble { background: #667eea; color: white; border-radius: 12px 4px 12px 12px; }
        .message.assistant .message-bubble { background: white; border: 1px solid #e0e0e0; border-radius: 4px 12px 12px 12px; }
        .loading { text-align: center; color: #999; font-size: 14px; }
        .input-area { padding: 20px 40px; border-top: 1px solid #e0e0e0; display: flex; gap: 12px; }
        .input-area textarea { flex: 1; padding: 12px 16px; border: 1px solid #e0e0e0; border-radius: 8px; font-family: inherit; font-size: 14px; resize: none; max-height: 120px; }
        .input-area button { padding: 12px 24px; background: #667eea; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: 14px; transition: background 0.2s; }
        .input-area button:hover { background: #5568d3; }
        .input-area button:disabled { background: #ccc; cursor: not-allowed; }
        .sidebar-section { margin-bottom: 20px; }
        .sidebar-title { font-weight: 600; font-size: 12px; color: #666; text-transform: uppercase; margin-bottom: 8px; }
        .sidebar-item { padding: 8px 12px; margin-bottom: 4px; background: white; border-radius: 6px; font-size: 13px; cursor: pointer; transition: all 0.2s; }
        .sidebar-item:hover { background: #e8e8e8; }
        .error { color: #d32f2f; background: #ffebee; padding: 12px 16px; border-radius: 8px; font-size: 13px; }
        .status { padding: 8px 0; font-size: 12px; color: #999; word-break: break-word; }
        .code-block { background: #f5f5f5; border-left: 3px solid #667eea; padding: 8px 12px; margin: 4px 0; font-family: monospace; font-size: 11px; overflow-x: auto; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 Nova AI</h1>
            <p>Local-first coding assistant for your repositories</p>
            <div class="profile-badge" id="profile">Loading...</div>
        </div>
        <div class="main">
            <div class="sidebar">
                <div class="sidebar-section">
                    <div class="sidebar-title">Quick Commands</div>
                    <div class="sidebar-item" onclick="insertPrompt('Explain the structure of this repository')">📁 Explain repo</div>
                    <div class="sidebar-item" onclick="insertPrompt('What are the main modules in this project?')">📚 Main modules</div>
                    <div class="sidebar-item" onclick="insertPrompt('What are the latest changes in this repo?')">🔄 Recent changes</div>
                    <div class="sidebar-item" onclick="insertPrompt('Find bugs or issues in this code')">🐛 Find bugs</div>
                    <div class="sidebar-item" onclick="insertPrompt('Help me write a test for this')">✅ Write tests</div>
                    <div class="sidebar-item" onclick="insertPrompt('Optimize this code for performance')">⚡ Optimize</div>
                </div>
                <div class="sidebar-section">
                    <div class="sidebar-title">Bypass Tokens</div>
                    <input type="password" id="bypassPassword" placeholder="Admin password" style="width: 100%; padding: 6px; margin-bottom: 8px; border: 1px solid #ddd; border-radius: 4px; font-size: 12px;">
                    <div class="sidebar-item" onclick="enableBypass()" style="background: #667eea; color: white;">🔓 Enable</div>
                </div>
                <div class="sidebar-section">
                    <div class="sidebar-title">Chat</div>
                    <div class="sidebar-item" onclick="clearHistory()" style="color: #d32f2f;">🗑️ Clear</div>
                </div>
            </div>
            <div class="chat-area">
                <div class="messages" id="messages"></div>
                <div class="input-area">
                    <textarea id="prompt" placeholder="Ask Nova anything about this codebase..." rows="2"></textarea>
                    <button id="send" onclick="sendMessage()">Ask</button>
                </div>
            </div>
        </div>
    </div>

    <script>
        const messagesDiv = document.getElementById('messages');
        const promptInput = document.getElementById('prompt');
        const sendBtn = document.getElementById('send');

        // Load system info
        async function loadInfo() {
            try {
                const response = await fetch('/api/info');
                const data = await response.json();
                document.getElementById('profile').textContent = `Profile: ${data.profile}`;
                
                // Add info to sidebar or status
                const infoDiv = document.createElement('div');
                infoDiv.className = 'sidebar-section';
                infoDiv.innerHTML = `
                    <div class="sidebar-title">System</div>
                    <div class="status" id="model-info">Model: ${data.model}</div>
                    <div class="status" id="repo-info">Repo: ${data.repo.split('/').pop()}</div>
                `;
                document.querySelector('.sidebar').appendChild(infoDiv);
            } catch (e) {
                console.error('Failed to load info:', e);
            }
        }

        function addMessage(text, isUser = false) {
            const msgDiv = document.createElement('div');
            msgDiv.className = `message ${isUser ? 'user' : 'assistant'}`;
            const bubble = document.createElement('div');
            bubble.className = 'message-bubble';
            
            // Parse code blocks
            if (!isUser && text.includes('```')) {
                bubble.innerHTML = text.split(/```/g).map((part, i) => {
                    if (i % 2 === 0) return escapeHtml(part);
                    return `<div class="code-block">${escapeHtml(part.trim())}</div>`;
                }).join('');
            } else {
                bubble.textContent = text;
            }
            
            msgDiv.appendChild(bubble);
            messagesDiv.appendChild(msgDiv);
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        }

        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }

        function insertPrompt(text) {
            promptInput.value = text;
            promptInput.focus();
        }

        function clearHistory() {
            if (confirm('Clear chat history?')) {
                messagesDiv.innerHTML = '';
                addMessage('👋 Chat cleared. Start fresh!');
            }
        }

        let currentBypassPassword = null;

        function enableBypass() {
            const pwd = document.getElementById('bypassPassword').value;
            if (!pwd) {
                alert('Enter admin password to bypass token limits');
                return;
            }
            currentBypassPassword = pwd;
            alert('✅ Token bypass enabled!');
            document.getElementById('bypassPassword').value = '';
        }

        async function sendMessage() {
            const prompt = promptInput.value.trim();
            if (!prompt || sendBtn.disabled) return;

            addMessage(prompt, true);
            promptInput.value = '';
            sendBtn.disabled = true;

            const loadingMsg = document.createElement('div');
            loadingMsg.className = 'message assistant';
            const loadingBubble = document.createElement('div');
            loadingBubble.className = 'message-bubble loading';
            loadingBubble.textContent = '⏳ Thinking...';
            loadingMsg.appendChild(loadingBubble);
            messagesDiv.appendChild(loadingMsg);
            messagesDiv.scrollTop = messagesDiv.scrollHeight;

            let retries = 2;
            while (retries > 0) {
                try {
                    const payload = { prompt };
                    if (currentBypassPassword) {
                        payload.access_password = currentBypassPassword;
                    }

                    const response = await fetch('/api/ask', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    });

                    const data = await response.json();

                    if (response.status === 503 && retries > 1) {
                        addMessage('⚠️ Ollama is loading... Retrying in 3 seconds...');
                        await new Promise(r => setTimeout(r, 3000));
                        retries--;
                        continue;
                    }

                    loadingMsg.remove();

                    if (data.error) {
                        let msg = '❌ ' + data.error;
                        if (data.suggestion) msg += '\n💡 ' + data.suggestion;
                        addMessage(msg);
                    } else {
                        addMessage(data.answer);
                    }
                    break;
                } catch (error) {
                    loadingMsg.remove();
                    addMessage('❌ Cannot reach Nova: ' + error.message);
                    break;
                }
            }

            sendBtn.disabled = false;
            promptInput.focus();
        }

        // Event listeners
        document.getElementById('send').addEventListener('click', sendMessage);
        promptInput.addEventListener('keydown', (e) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                sendMessage();
            }
        });

        // Initialize
        loadInfo();
        addMessage('👋 Welcome! I\\'m Nova, your local coding assistant. Ask me anything about this repository. Try "Explain the structure of this repository" to get started!');
        promptInput.focus();
    </script>
</body>
</html>
"""


def create_web_app(config: NovaConfig) -> Flask:
    """Create and configure the Flask web app."""
    if Flask is None:
        raise RuntimeError(
            "Flask is required for the web GUI. Install it with: pip install flask"
        )

    app = Flask(__name__)
    agent = NovaAgent(config)

    @app.route("/")
    def index():
        return render_template_string(HTML_TEMPLATE)

    @app.route("/api/info")
    def get_info():
        return jsonify(
            {
                "profile": config.profile,
                "model": config.model,
                "repo": str(config.repo_root),
            }
        )

    @app.route("/api/ask", methods=["POST"])
    def ask():
        data = request.get_json()
        prompt = data.get("prompt", "").strip()
        access_password = data.get("access_password")

        if not prompt:
            return jsonify({"error": "Prompt cannot be empty"}), 400

        try:
            # Override access password if provided
            original_password = agent.config.access_password
            if access_password:
                agent.config.access_password = access_password
            
            response = agent.answer(prompt)
            
            # Restore original password
            agent.config.access_password = original_password
            
            return jsonify({"answer": response.answer})
        except RuntimeError as e:
            error_msg = str(e)
            if "Ollama" in error_msg or "connection" in error_msg.lower():
                return jsonify({
                    "error": "Ollama is not responding",
                    "suggestion": "Run 'ollama serve' in another terminal"
                }), 503
            return jsonify({"error": error_msg}), 500
        except Exception as e:
            return jsonify({"error": f"Error: {str(e)}"}), 500

    return app


def run_web_gui(config: NovaConfig, host: str = "127.0.0.1", port: int = 5000, debug: bool = False) -> None:
    """Run the Nova web GUI server."""
    app = create_web_app(config)
    print(f"\n🚀 Nova Web GUI is running!")
    print(f"📖 Open your browser: http://{host}:{port}")
    print(f"🔌 Press Ctrl+C to stop\n")
    app.run(host=host, port=port, debug=debug)
