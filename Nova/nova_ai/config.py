from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import os

from .profiles import DEFAULT_PROFILE, get_system_prompt


@dataclass(slots=True)
class NovaConfig:
    repo_root: Path
    model: str = "qwen2.5-coder:7b"
    ollama_host: str = "http://127.0.0.1:11434"
    profile: str = DEFAULT_PROFILE
    system_prompt: str = get_system_prompt(DEFAULT_PROFILE)
    knowledge_dir: Path = Path(__file__).resolve().parent.parent / "knowledge"
    projects_dir: Path = Path(__file__).resolve().parent.parent.parent / "NovaProjects"
    state_dir: Path = Path(__file__).resolve().parent.parent / ".nova"
    credits_enabled: bool = True
    access_password: str | None = None
    prompt_credit_cost: int = 1
    shell_timeout_seconds: int = 30

    @classmethod
    def from_args(
        cls,
        repo_root: str | None = None,
        model: str | None = None,
        ollama_host: str | None = None,
        profile: str | None = None,
        knowledge_dir: str | None = None,
        projects_dir: str | None = None,
        state_dir: str | None = None,
        credits_enabled: bool | None = None,
        access_password: str | None = None,
    ) -> "NovaConfig":
        selected_profile = profile or os.getenv("NOVA_PROFILE", DEFAULT_PROFILE)
        selected_state_dir = Path(
            state_dir
            or os.getenv("NOVA_STATE_DIR")
            or (Path(__file__).resolve().parent.parent / ".nova")
        ).resolve()
        env_enabled = os.getenv("NOVA_CREDITS_ENABLED")
        env_access_password = os.getenv("NOVA_ACCESS_PASSWORD") or os.getenv("NOVA_BYPASS_PASSWORD")
        return cls(
            repo_root=Path(repo_root or os.getcwd()).resolve(),
            model=model or os.getenv("NOVA_MODEL", "qwen2.5-coder:7b"),
            ollama_host=ollama_host or os.getenv("OLLAMA_HOST", "http://127.0.0.1:11434"),
            profile=selected_profile,
            system_prompt=get_system_prompt(selected_profile),
            knowledge_dir=Path(
                knowledge_dir
                or os.getenv("NOVA_KNOWLEDGE_DIR")
                or (Path(__file__).resolve().parent.parent / "knowledge")
            ).resolve(),
            projects_dir=Path(
                projects_dir
                or os.getenv("NOVA_PROJECTS_DIR")
                or (Path(__file__).resolve().parent.parent.parent / "NovaProjects")
            ).resolve(),
            state_dir=selected_state_dir,
            credits_enabled=credits_enabled if credits_enabled is not None else env_enabled != "0",
            access_password=access_password or env_access_password,
        )
