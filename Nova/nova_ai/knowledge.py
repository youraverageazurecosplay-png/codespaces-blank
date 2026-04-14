from __future__ import annotations

from pathlib import Path


def load_knowledge(knowledge_dir: Path, profile: str, max_chars: int = 12000) -> str:
    sections: list[str] = []
    loaded_names: set[str] = set()

    ordered_files = [
        ("Common knowledge", knowledge_dir / "common.md"),
        (f"{profile.title()} knowledge", knowledge_dir / f"{profile}.md"),
        (f"{profile.title()} advanced knowledge", knowledge_dir / f"{profile}_advanced.md"),
    ]

    for _, path in ordered_files:
        loaded_names.add(path.name)

    for title, path in ordered_files:
        if not path.exists() or not path.is_file():
            continue
        text = path.read_text(encoding="utf-8", errors="replace").strip()
        if not text:
            continue
        if len(text) > max_chars:
            text = text[:max_chars] + "\n...<truncated>..."
        sections.append(f"{title}:\n{text}")

    if knowledge_dir.exists() and knowledge_dir.is_dir():
        extra_paths = sorted(knowledge_dir.glob("*.md"), key=lambda p: p.name)
        for path in extra_paths:
            if path.name in loaded_names:
                continue
            text = path.read_text(encoding="utf-8", errors="replace").strip()
            if not text:
                continue
            if len(text) > max_chars:
                text = text[:max_chars] + "\n...<truncated>..."
            sections.append(f"Extra knowledge ({path.name}):\n{text}")

    return "\n\n".join(sections) if sections else "No extra knowledge files loaded."
