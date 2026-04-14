from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, UTC
from pathlib import Path
import subprocess


@dataclass(slots=True)
class ToolResult:
    name: str
    content: str


class RepoTools:
    def __init__(
        self,
        repo_root: Path,
        shell_timeout_seconds: int = 30,
        projects_dir: Path | None = None,
    ) -> None:
        self.repo_root = repo_root
        self.shell_timeout_seconds = shell_timeout_seconds
        self.projects_dir = (projects_dir or repo_root / "NovaProjects").resolve()
        self.patch_dir = self.repo_root / ".nova" / "patches"
        self.patch_dir.mkdir(parents=True, exist_ok=True)
        self.projects_dir.mkdir(parents=True, exist_ok=True)

    def list_files(self, limit: int = 200) -> ToolResult:
        files = []
        for path in self.repo_root.rglob("*"):
            if path.is_file() and ".git" not in path.parts:
                files.append(str(path.relative_to(self.repo_root)))
            if len(files) >= limit:
                break
        return ToolResult("list_files", "\n".join(sorted(files)) or "(no files found)")

    def read_file(self, relative_path: str, max_chars: int = 12000) -> ToolResult:
        path = (self.repo_root / relative_path).resolve()
        self._assert_inside_repo(path)
        if not path.exists() or not path.is_file():
            return ToolResult("read_file", f"File not found: {relative_path}")
        text = path.read_text(encoding="utf-8", errors="replace")
        if len(text) > max_chars:
            text = text[:max_chars] + "\n...<truncated>..."
        return ToolResult("read_file", text)

    def search_text(self, query: str, limit: int = 80) -> ToolResult:
        matches = []
        lowered = query.lower()
        for path in self.repo_root.rglob("*"):
            if not path.is_file() or ".git" in path.parts:
                continue
            try:
                text = path.read_text(encoding="utf-8", errors="replace")
            except OSError:
                continue
            for index, line in enumerate(text.splitlines(), start=1):
                if lowered in line.lower():
                    matches.append(f"{path.relative_to(self.repo_root)}:{index}: {line.strip()}")
                    if len(matches) >= limit:
                        return ToolResult("search_text", "\n".join(matches))
        return ToolResult("search_text", "\n".join(matches) if matches else "(no matches found)")

    def run_command(self, command: str) -> ToolResult:
        completed = subprocess.run(
            command,
            cwd=self.repo_root,
            shell=True,
            capture_output=True,
            text=True,
            timeout=self.shell_timeout_seconds,
        )
        output = (completed.stdout or "") + (completed.stderr or "")
        output = output.strip() or "(no output)"
        return ToolResult(
            "run_command",
            f"exit_code={completed.returncode}\n{output}",
        )

    def save_patch_proposal(self, diff_text: str) -> ToolResult:
        timestamp = datetime.now(UTC).strftime("%Y%m%dT%H%M%SZ")
        path = self.patch_dir / f"proposal-{timestamp}.diff"
        path.write_text(diff_text, encoding="utf-8")
        return ToolResult("save_patch_proposal", f"Saved patch proposal to {path}")

    def list_projects(self) -> ToolResult:
        projects = [path.name for path in self.projects_dir.iterdir() if path.is_dir()]
        content = "\n".join(sorted(projects)) if projects else "(no Nova projects found)"
        return ToolResult("list_projects", content)

    def create_project(self, name: str, template: str = "basic-python") -> ToolResult:
        slug = self._slugify(name)
        project_root = self.projects_dir / slug
        if project_root.exists():
            return ToolResult("create_project", f"Project already exists: {project_root}")

        project_root.mkdir(parents=True, exist_ok=False)
        self._write_template(project_root, name=name, template=template)
        return ToolResult("create_project", f"Created {template} project at {project_root}")

    def _assert_inside_repo(self, path: Path) -> None:
        path.relative_to(self.repo_root)

    def _slugify(self, value: str) -> str:
        cleaned = "".join(ch.lower() if ch.isalnum() else "-" for ch in value.strip())
        parts = [part for part in cleaned.split("-") if part]
        return "-".join(parts) or "nova-project"

    def _write_template(self, project_root: Path, name: str, template: str) -> None:
        templates = {
            "basic-python": {
                "README.md": f"# {name}\n\nCreated by Nova.\n",
                "main.py": (
                    'def main() -> None:\n'
                    f'    print("Hello from {name}")\n\n'
                    'if __name__ == "__main__":\n'
                    "    main()\n"
                ),
            },
            "bash-script": {
                "README.md": f"# {name}\n\nCreated by Nova.\n",
                "script.sh": (
                    "#!/usr/bin/env bash\n"
                    "set -euo pipefail\n\n"
                    f'echo "Hello from {name}"\n'
                ),
            },
            "minecraft-fabric": {
                "README.md": (
                    f"# {name}\n\n"
                    "Created by Nova as a Fabric-oriented starter workspace.\n"
                ),
                "NOTES.md": (
                    "Start by adding your Fabric build files, mod metadata, and source tree.\n"
                    "Keep Minecraft version notes here so Nova can reason about compatibility.\n"
                ),
                "src/main/java/.gitkeep": "",
                "src/main/resources/.gitkeep": "",
            },
        }
        files = templates.get(template, templates["basic-python"])
        for relative_path, content in files.items():
            destination = project_root / relative_path
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_text(content, encoding="utf-8")
