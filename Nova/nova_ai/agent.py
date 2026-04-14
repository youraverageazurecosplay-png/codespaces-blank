from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import textwrap

from .config import NovaConfig
from .credits import CreditManager
from .knowledge import load_knowledge
from .model_client import OllamaClient
from .tools import RepoTools


@dataclass(slots=True)
class NovaResponse:
    answer: str
    context_summary: str


class NovaAgent:
    def __init__(self, config: NovaConfig) -> None:
        self.config = config
        self.tools = RepoTools(
            config.repo_root,
            config.shell_timeout_seconds,
            projects_dir=config.projects_dir,
        )
        self.client = OllamaClient(config.ollama_host, config.model)
        self.credits = CreditManager(config.state_dir / "credits.json")
        self.credits.set_enabled(config.credits_enabled)

    def answer(self, user_request: str) -> NovaResponse:
        self.credits.ensure_can_spend(
            self.config.prompt_credit_cost,
            access_password=self.config.access_password,
        )
        context = self._build_context(user_request)
        prompt = textwrap.dedent(
            f"""
            Repository root: {self.config.repo_root}
            Active profile: {self.config.profile}
            User request:
            {user_request}

            Relevant local context:
            {context}

            Respond as a coding assistant.
            If code changes are appropriate, provide a unified diff in a fenced diff block.
            Keep the answer practical and specific to the repository context above.
            """
        ).strip()
        answer = self.client.generate(prompt=prompt, system_prompt=self.config.system_prompt)
        self.credits.spend(
            self.config.prompt_credit_cost,
            access_password=self.config.access_password,
        )
        diff_text = self._extract_diff(answer)
        if diff_text:
            self.tools.save_patch_proposal(diff_text)
        return NovaResponse(answer=answer, context_summary=context)

    def _build_context(self, user_request: str) -> str:
        lowered = user_request.lower()
        knowledge = load_knowledge(self.config.knowledge_dir, self.config.profile)
        chunks = [
            f"Profile guidance: {self._profile_guidance()}",
            f"Knowledge base:\n{knowledge}",
            f"Nova projects directory: {self.config.projects_dir}",
            f"Known Nova projects:\n{self.tools.list_projects().content}",
            f"File inventory:\n{self.tools.list_files(limit=120).content}",
        ]

        probable_paths = self._extract_probable_paths(user_request)
        for relative_path in probable_paths[:3]:
            chunks.append(f"File preview: {relative_path}\n{self.tools.read_file(relative_path).content}")

        keywords = self._extract_keywords(lowered)
        if keywords:
            search_query = keywords[0]
            chunks.append(
                f"Search results for '{search_query}':\n{self.tools.search_text(search_query, limit=40).content}"
            )

        return "\n\n".join(chunks)

    def _profile_guidance(self) -> str:
        profile = self.config.profile
        if profile == "python":
            return "Prioritize Python patterns, module structure, type hints, tests, and traceback-oriented debugging."
        if profile == "bash":
            return "Prioritize safe shell scripting, quoting, exit codes, portability, and automation workflows."
        if profile == "minecraft":
            return (
                "Prioritize Minecraft mod/plugin context, inspect version and platform files, and tailor suggestions "
                "to Fabric, Forge, Paper, Spigot, or datapack structure when visible."
            )
        return "Use general coding-assistant behavior."

    def _extract_probable_paths(self, user_request: str) -> list[str]:
        candidates = []
        for token in user_request.split():
            cleaned = token.strip("`'\"(),.")
            if "/" in cleaned or "." in Path(cleaned).name:
                candidates.append(cleaned)
        return candidates

    def _extract_keywords(self, text: str) -> list[str]:
        stop_words = {
            "the",
            "this",
            "that",
            "with",
            "from",
            "into",
            "where",
            "what",
            "when",
            "please",
            "would",
            "could",
            "should",
            "about",
        }
        words = [word.strip(".,!?()[]{}:;") for word in text.split()]
        return [word for word in words if len(word) > 3 and word not in stop_words]

    def _extract_diff(self, answer: str) -> str | None:
        marker = "```diff"
        if marker not in answer:
            return None
        remainder = answer.split(marker, 1)[1]
        return remainder.split("```", 1)[0].strip() or None
