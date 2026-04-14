const vscode = require("vscode");
const child_process = require("child_process");

function activate(context) {
  const command = vscode.commands.registerCommand("nova.openSidePanel", async () => {
    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
    if (!workspaceFolder) {
      vscode.window.showErrorMessage("Open a workspace folder before using Nova side panel.");
      return;
    }

    const panel = vscode.window.createWebviewPanel(
      "novaSidePanel",
      "Nova",
      { viewColumn: vscode.ViewColumn.Beside, preserveFocus: false },
      { enableScripts: true }
    );

    panel.webview.html = getWebviewContent();

    panel.webview.onDidReceiveMessage(async (message) => {
      if (message.command === "ask") {
        panel.webview.postMessage({ type: "status", text: "Sending request to Nova..." });
        try {
          const answer = await askNova(message.prompt, workspaceFolder.uri.fsPath);
          panel.webview.postMessage({ type: "answer", answer });
        } catch (error) {
          panel.webview.postMessage({ type: "error", error: String(error) });
        }
      }
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
    body { font-family: var(--vscode-font-family); color: var(--vscode-foreground); background: var(--vscode-editor-background); margin: 0; padding: 16px; }
    h1 { margin-top: 0; }
    textarea { width: 100%; min-height: 120px; font-family: var(--vscode-editor-font-family); font-size: 13px; margin-bottom: 12px; padding: 8px; box-sizing: border-box; border: 1px solid var(--vscode-editorWidget-border); border-radius: 4px; background: var(--vscode-editor-background); color: var(--vscode-editor-foreground); }
    button { padding: 8px 16px; font-size: 13px; border: none; border-radius: 4px; background: var(--vscode-button-background); color: var(--vscode-button-foreground); cursor: pointer; }
    button:hover { filter: brightness(1.05); }
    #status { margin-top: 12px; color: var(--vscode-descriptionForeground); }
    pre { white-space: pre-wrap; word-break: break-word; background: var(--vscode-editorWidget-background); border: 1px solid var(--vscode-editorWidget-border); border-radius: 4px; padding: 12px; margin-top: 12px; min-height: 180px; overflow: auto; }
  </style>
</head>
<body>
  <h1>Nova Side Panel</h1>
  <p>Ask Nova any repository-aware question and get an answer in this panel.</p>
  <textarea id="prompt" placeholder="Ask Nova about Python, Bash, or Minecraft modding..."></textarea>
  <button id="askButton">Ask Nova</button>
  <div id="status">Ready.</div>
  <pre id="response"></pre>
  <script>
    const vscode = acquireVsCodeApi();
    const prompt = document.getElementById("prompt");
    const status = document.getElementById("status");
    const response = document.getElementById("response");
    document.getElementById("askButton").addEventListener("click", () => {
      const value = prompt.value.trim();
      if (!value) {
        status.textContent = "Please enter a question for Nova.";
        return;
      }
      response.textContent = "";
      status.textContent = "Sending request...";
      vscode.postMessage({ command: "ask", prompt: value });
    });

    window.addEventListener("message", (event) => {
      const message = event.data;
      if (message.type === "status") {
        status.textContent = message.text;
      }
      if (message.type === "answer") {
        status.textContent = "Nova answered.";
        response.textContent = message.answer;
      }
      if (message.type === "error") {
        status.textContent = "Error from Nova.";
        response.textContent = message.error;
      }
    });
  </script>
</body>
</html>`;
}

function deactivate() {}

module.exports = { activate, deactivate };
