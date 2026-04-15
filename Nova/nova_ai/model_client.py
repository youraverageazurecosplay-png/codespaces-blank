from __future__ import annotations

import json
import subprocess
import time
from urllib import error, request


class OllamaClient:
    def __init__(self, host: str, model: str) -> None:
        self.host = host.rstrip("/")
        self.model = model

    def check_ollama(self) -> None:
        """Check if Ollama is running and the model is available. Start Ollama if not running."""
        try:
            # Check if Ollama is running
            req = request.Request(f"{self.host}/api/tags", method="GET")
            with request.urlopen(req, timeout=10) as response:
                tags_data = json.loads(response.read().decode("utf-8"))
                models = [m["name"] for m in tags_data.get("models", [])]
                if self.model not in models:
                    raise RuntimeError(
                        f"Model '{self.model}' is not installed in Ollama. "
                        f"Available models: {', '.join(models) if models else 'none'}. "
                        f"Install it with: ollama pull {self.model}"
                    )
        except error.URLError:
            # Try to start Ollama
            print("Ollama not running, attempting to start...")
            try:
                subprocess.Popen(
                    ["ollama", "serve"],
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                    start_new_session=True
                )
                # Wait a bit for it to start
                time.sleep(5)
                # Check again
                req = request.Request(f"{self.host}/api/tags", method="GET")
                with request.urlopen(req, timeout=10) as response:
                    tags_data = json.loads(response.read().decode("utf-8"))
                    models = [m["name"] for m in tags_data.get("models", [])]
                    if self.model not in models:
                        raise RuntimeError(
                            f"Model '{self.model}' is not installed in Ollama. "
                            f"Available models: {', '.join(models) if models else 'none'}. "
                            f"Install it with: ollama pull {self.model}"
                        )
            except (subprocess.SubprocessError, error.URLError):
                raise RuntimeError(
                    f"Could not reach Ollama at {self.host}. "
                    "Make sure Ollama is installed and running with: ollama serve"
                )

    def generate(self, prompt: str, system_prompt: str) -> str:
        self.check_ollama()
        payload = {
            "model": self.model,
            "prompt": prompt,
            "system": system_prompt,
            "stream": False,
        }
        body = json.dumps(payload).encode("utf-8")
        req = request.Request(
            f"{self.host}/api/generate",
            data=body,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            with request.urlopen(req, timeout=120) as response:
                data = json.loads(response.read().decode("utf-8"))
        except error.HTTPError as exc:
            if exc.code == 500:
                raise RuntimeError(
                    f"Ollama server error (500). The model '{self.model}' may not be installed or is still loading. "
                    f"Try: ollama pull {self.model} and wait for it to load, or use a different model with --model"
                ) from exc
            raise RuntimeError(
                f"Ollama HTTP error {exc.code}: {exc.reason}"
            ) from exc
        except error.URLError as exc:
            raise RuntimeError(
                f"Failed to generate response from Ollama. {exc}"
            ) from exc
        return data.get("response", "").strip()
