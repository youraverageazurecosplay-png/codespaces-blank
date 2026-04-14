#!/bin/bash

# Nova AI Installer Script
# This script sets up Nova AI in the current repository

set -e

echo "🚀 Setting up Nova AI..."

# Check Python version
if ! python3 --version | grep -q "Python 3.11\|3.12\|3.13"; then
    echo "❌ Python 3.11+ is required. Please install it first."
    exit 1
fi

# Check if Ollama is installed
if ! command -v ollama &> /dev/null; then
    echo "📦 Installing Ollama..."
    curl -fsSL https://ollama.ai/install.sh | sh
fi

# Start Ollama in background
echo "🔄 Starting Ollama server..."
ollama serve &

# Wait for Ollama to start
echo "⏳ Waiting for Ollama to be ready..."
for i in {1..30}; do
    if curl -s http://127.0.0.1:11434/api/tags > /dev/null 2>&1; then
        echo "✅ Ollama is ready!"
        break
    fi
    sleep 1
done

if ! curl -s http://127.0.0.1:11434/api/tags > /dev/null 2>&1; then
    echo "❌ Ollama failed to start. Please run 'ollama serve' manually."
    exit 1
fi

# Install Nova
echo "📦 Installing Nova AI..."
pip install -e .

# Pull the default model
echo "🤖 Pulling default model (qwen2.5-coder:7b)..."
ollama pull qwen2.5-coder:7b

echo "✅ Nova AI setup complete!"
echo ""
echo "To use Nova:"
echo "  nova --profile python ask \"Your question here\""
echo "  nova chat"
echo ""
echo "For VS Code GUI, see VSCODE_SETUP.md"