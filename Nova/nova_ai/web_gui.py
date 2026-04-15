from flask import Flask, render_template_string, request, jsonify
from .agent import NovaAgent
from .config import NovaConfig
import subprocess
import time
from urllib import request as urllib_request, error as urllib_error


def create_web_app(config: NovaConfig) -> Flask:
    app = Flask(__name__)
    
    # Create agent but don't pre-check Ollama here - let it happen on first request
    agent = NovaAgent(config)

    @app.route("/")
    def index():
        return render_template_string(get_html_template())

    @app.route("/api/ask", methods=["POST"])
    def ask():
        data = request.get_json()
        prompt = data.get("prompt", "").strip()
        if not prompt:
            return jsonify({"error": "Empty prompt"}), 400
        
        try:
            response = agent.answer(prompt)
            return jsonify({"answer": response.answer})
        except Exception as e:
            error_msg = str(e)
            print(f"❌ Web GUI Error: {error_msg}")
            
            # Provide helpful error messages
            if "500" in error_msg or "Ollama" in error_msg.lower():
                return jsonify({"error": f"Ollama connection issue: {error_msg}. Try starting Ollama with 'ollama serve' and ensure the model is installed."}), 500
            elif "timeout" in error_msg.lower():
                return jsonify({"error": f"Request timed out: {error_msg}. The model may be loading - try again in a moment."}), 500
            else:
                return jsonify({"error": f"Error: {error_msg}"}), 500

    return app


def run_web_gui(config: NovaConfig, host: str = "127.0.0.1", port: int = 5000, debug: bool = False):
    app = create_web_app(config)
    print(f"🚀 Nova Web GUI is running!")
    print(f"📖 Open your browser: http://{host}:{port}")
    print(f"🔌 Press Ctrl+C to stop\n")
    app.run(host=host, port=port, debug=debug, use_reloader=False)


def get_html_template():
    return """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nova AI - Local Coding Assistant</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif; 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
            min-height: 100vh; 
            padding: 20px; 
        }
        .container { 
            max-width: 900px; 
            margin: 0 auto; 
            background: white; 
            border-radius: 16px; 
            box-shadow: 0 20px 60px rgba(0,0,0,0.3); 
            overflow: hidden; 
            display: flex; 
            flex-direction: column; 
            height: 90vh; 
        }
        .header { 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
            color: white; 
            padding: 30px 40px; 
        }
        .header h1 { 
            font-size: 28px; 
            margin-bottom: 5px; 
        }
        .header p { 
            opacity: 0.9; 
            font-size: 14px; 
        }
        .messages { 
            flex: 1; 
            padding: 20px 40px; 
            overflow-y: auto; 
            display: flex; 
            flex-direction: column; 
            gap: 16px; 
            background: #fafafa; 
        }
        .message { 
            display: flex; 
            gap: 12px; 
            animation: slideIn 0.3s ease; 
        }
        @keyframes slideIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }
        @keyframes spin {
            to { transform: rotate(360deg); }
        }
        .message.user .bubble { 
            background: #667eea; 
            color: white; 
            margin-left: auto; 
        }
        .message.assistant .bubble { 
            background: #e0e0e0; 
            color: #333; 
        }
        .message.error .bubble {
            background: #ffebee;
            color: #c62828;
            border-left: 4px solid #c62828;
        }
        .message.system .bubble {
            background: #fff3e0;
            color: #e65100;
            border-left: 4px solid #e65100;
        }
        .bubble { 
            padding: 12px 16px; 
            border-radius: 12px; 
            max-width: 70%; 
            word-wrap: break-word; 
            white-space: pre-wrap; 
        }
        .spinner {
            display: inline-block;
            width: 20px;
            height: 20px;
            border: 3px solid #667eea;
            border-top-color: transparent;
            border-radius: 50%;
            animation: spin 1s linear infinite;
        }
        .input-area { 
            padding: 20px 40px; 
            border-top: 1px solid #e0e0e0; 
            display: flex; 
            gap: 12px; 
        }
        textarea { 
            flex: 1; 
            padding: 12px; 
            border: 1px solid #ddd; 
            border-radius: 8px; 
            font-family: inherit; 
            font-size: 14px; 
            resize: none; 
            max-height: 100px; 
        }
        button { 
            padding: 12px 24px; 
            background: #667eea; 
            color: white; 
            border: none; 
            border-radius: 8px; 
            cursor: pointer; 
            font-weight: 600; 
            transition: background 0.2s; 
        }
        button:hover { 
            background: #5568d3; 
        }
        button:disabled { 
            background: #ccc; 
            cursor: not-allowed; 
        }
        .status { 
            padding: 0 40px 10px; 
            color: #666; 
            font-size: 12px; 
            display: flex;
            align-items: center;
            gap: 8px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 Nova AI</h1>
            <p>Local-first coding assistant - Offline & Private</p>
        </div>
        <div class="messages" id="messages"></div>
        <div class="status" id="status">Ready</div>
        <div class="input-area">
            <textarea id="prompt" placeholder="Ask Nova anything...
Examples:
- Explain this repository
- Write a Python function
- Help debug this error" rows="3"></textarea>
            <button id="send" onclick="sendMessage()">Send</button>
        </div>
    </div>

    <script>
        const messagesDiv = document.getElementById('messages');
        const promptInput = document.getElementById('prompt');
        const statusDiv = document.getElementById('status');
        const sendBtn = document.getElementById('send');

        function addMessage(text, type = 'assistant') {
            const msg = document.createElement('div');
            msg.className = `message ${type}`;
            const bubble = document.createElement('div');
            bubble.className = 'bubble';
            bubble.textContent = text;
            msg.appendChild(bubble);
            messagesDiv.appendChild(msg);
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        }

        function setStatus(text, hasSpinner = false) {
            statusDiv.innerHTML = hasSpinner 
                ? `<div class="spinner"></div> ${text}`
                : text;
        }

        async function sendMessage() {
            const prompt = promptInput.value.trim();
            if (!prompt) return;

            addMessage(prompt, 'user');
            promptInput.value = '';
            promptInput.style.height = '60px';
            sendBtn.disabled = true;
            setStatus('Nova is thinking...', true);

            try {
                const response = await fetch('/api/ask', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ prompt })
                });

                const data = await response.json();
                if (response.ok) {
                    addMessage(data.answer, 'assistant');
                    setStatus('Ready');
                } else {
                    const errorMsg = data.error || 'Unknown error';
                    addMessage(`Error: ${errorMsg}`, 'error');
                    
                    // Helpful suggestions based on error type
                    if (errorMsg.includes('500') || errorMsg.includes('Ollama')) {
                        addMessage('💡 Try: 1) Start Ollama (ollama serve)\\n2) Install a model (ollama pull qwen2.5-coder:1.5b)\\n3) Refresh the page', 'system');
                    }
                    setStatus('Error occurred');
                }
            } catch (error) {
                addMessage(`Connection Error: ${error.message}`, 'error');
                addMessage('💡 Make sure the web server is still running. Try refreshing the page.', 'system');
                setStatus('Connection error');
            } finally {
                sendBtn.disabled = false;
            }
        }

        promptInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });

        promptInput.addEventListener('input', () => {
            promptInput.style.height = 'auto';
            promptInput.style.height = Math.min(promptInput.scrollHeight, 100) + 'px';
        });

        // Welcome message
        addMessage('👋 Hello! I\\'m Nova, your local AI coding assistant.\\n\\nI can help you with:\\n- Coding questions\\n- Code explanations\\n- Debugging\\n- Project setup\\n\\nAsk me anything!', 'system');
        
        // Check if server is responding
        setTimeout(async () => {
            try {
                const response = await fetch('/api/ask', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ prompt: 'test' })
                });
                if (response.status === 500) {
                    addMessage('⚠️ Backend error detected. Make sure Ollama is running and a model is installed.', 'error');
                }
            } catch (e) {
                // Server not responding
            }
        }, 2000);
    </script>
</body>
</html>"""
