const vscode = require("vscode");
const child_process = require("child_process");

let currentPanel;

function activate(context) {
  const command = vscode.commands.registerCommand("nova.openSidePanel", async () => {
    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
    if (!workspaceFolder) {
      vscode.window.showErrorMessage("Open a workspace folder before using Nova side panel.");
      return;
    }

    if (currentPanel) {
      currentPanel.reveal(vscode.ViewColumn.Beside);
      return;
    }

    const panel = vscode.window.createWebviewPanel(
      "novaSidePanel",
      "Nova AI Chat",
      { viewColumn: vscode.ViewColumn.Beside, preserveFocus: false },
      { enableScripts: true }
    );

    currentPanel = panel;
    panel.iconPath = vscode.Uri.file(context.extensionPath + '/nova-icon.svg');

    panel.webview.html = getWebviewContent();

    panel.webview.onDidReceiveMessage(async (message) => {
      if (message.command === "ask") {
        panel.webview.postMessage({ type: "status", text: "⏳ Thinking..." });
        try {
          const answer = await askNova(message.prompt, workspaceFolder.uri.fsPath);
          panel.webview.postMessage({ type: "answer", answer });
        } catch (error) {
          panel.webview.postMessage({ type: "error", error: String(error) });
        }
      }
    });

    panel.onDidDispose(() => {
      currentPanel = undefined;
    });
  });

  context.subscriptions.push(command);
}

function askNova(prompt, cwd) {
  return new Promise((resolve, reject) => {
    const python = process.env.PYTHON || process.env.PYTHON3 || "python";
    const args = ["-m", "nova_ai.cli", "ask", prompt];
    child_process.execFile(
      python,
      args,
      { cwd, env: process.env, maxBuffer: 10 * 1024 * 1024 },
      (error, stdout, stderr) => {
        if (error) {
          reject(stderr || stdout || error.message);
          return;
        }
        resolve(stdout.trim());
      }
    );
  });
}

function getWebviewContent() {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: var(--vscode-font-family); color: var(--vscode-foreground); background: var(--vscode-editor-background); display: flex; flex-direction: column; height: 100vh; }
    .header { padding: 12px 16px; border-bottom: 1px solid var(--vscode-editorWidget-border); }
    .header h1 { font-size: 14px; font-weight: 600; }
    .chat-area { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
    .messages { flex: 1; padding: 16px; overflow-y: auto; display: flex; flex-direction: column; gap: 12px; }
    .message { display: flex; gap: 8px; }
    .message.user { justify-content: flex-end; }
    .message-bubble { padding: 8px 12px; border-radius: 6px; max-width: 85%; white-space: pre-wrap; word-break: break-word; font-size: 12px; line-height: 1.4; }
    .message.user .message-bubble { background: var(--vscode-button-background); color: var(--vscode-button-foreground); }
    .message.assistant .message-bubble { background: var(--vscode-editorWidget-background); border: 1px solid var(--vscode-editorWidget-border); }
    .message.error .message-bubble { background: var(--vscode-errorForeground); opacity: 0.2; }
    .loading { text-align: center; color: var(--vscode-descriptionForeground); font-size: 12px; padding: 8px; }
    .input-area { padding: 12px 16px; border-top: 1px solid var(--vscode-editorWidget-border); display: flex; gap: 8px; align-items: flex-end; }
    textarea { flex: 1; padding: 8px; font-family: var(--vscode-editor-font-family); font-size: 12px; border: 1px solid var(--vscode-editorWidget-border); border-radius: 4px; background: var(--vscode-input-background); color: var(--vscode-input-foreground); resize: none; max-height: 100px; }
    button { padding: 6px 12px; font-size: 12px; border: none; border-radius: 4px; background: var(--vscode-button-background); color: var(--vscode-button-foreground); cursor: pointer; white-space: nowrap; }
    button:hover { background: var(--vscode-button-hoverBackground); }
    button:disabled { opacity: 0.5; cursor: not-allowed; }
  </style>
</head>
<body>
  <div class="header">
    <h1>🚀 Nova AI Chat</h1>
  </div>
  <div class="chat-area">
    <div class="messages" id="messages"></div>
    <div class="input-area">
      <textarea id="prompt" placeholder="Ask Nova about this codebase..." rows="1"></textarea>
      <button id="askButton">Send</button>
    </div>
  </div>
  <script>
    const vscode = acquireVsCodeApi();
    const prompt = document.getElementById("prompt");
    const sendBtn = document.getElementById("askButton");
    const messagesDiv = document.getElementById("messages");

    function addMessage(text, type = "assistant") {
      const msgDiv = document.createElement("div");
      msgDiv.className = \`message \${type}\`;
      const bubble = document.createElement("div");
      bubble.className = "message-bubble";
      bubble.textContent = text;
      msgDiv.appendChild(bubble);
      messagesDiv.appendChild(msgDiv);
      messagesDiv.scrollTop = messagesDiv.scrollHeight;
    }

    function sendMessage() {
      const value = prompt.value.trim();
      if (!value || sendBtn.disabled) return;
      
      addMessage(value, "user");
      prompt.value = "";
      sendBtn.disabled = true;
      
      const loadingDiv = document.createElement("div");
      loadingDiv.className = "loading";
      loadingDiv.textContent = "⏳ Nova is thinking...";
      messagesDiv.appendChild(loadingDiv);
      messagesDiv.scrollTop = messagesDiv.scrollHeight;
      
      vscode.postMessage({ command: "ask", prompt: value });
    }

    sendBtn.addEventListener("click", sendMessage);
    prompt.addEventListener("keydown", (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
        sendMessage();
      }
    });

    const initialDiv = document.createElement("div");
    initialDiv.className = "message assistant";
    const initialBubble = document.createElement("div");
    initialBubble.className = "message-bubble";
    initialBubble.textContent = "👋 Hi! I'm Nova. Ask me anything about this repository.";
    initialDiv.appendChild(initialBubble);
    messagesDiv.appendChild(initialDiv);

    window.addEventListener("message", (event) => {
      const msg = event.data;
      if (msg.type === "answer") {
        document.querySelector(".loading")?.remove();
        addMessage(msg.answer, "assistant");
        sendBtn.disabled = false;
        prompt.focus();
      }
      if (msg.type === "error") {
        document.querySelector(".loading")?.remove();
        addMessage("❌ " + msg.error, "error");
        sendBtn.disabled = false;
        prompt.focus();
      }
    });

    prompt.focus();
  </script>
</body>
</html>`;
}

function deactivate() {}

module.exports = { activate, deactivate };
