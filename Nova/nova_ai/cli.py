from __future__ import annotations

import argparse
import getpass
from pathlib import Path

from .agent import NovaAgent
from .config import NovaConfig
from .profiles import available_profiles, get_system_prompt


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Nova AI local-first coding assistant")
    parser.add_argument("--repo", default=None, help="Repository path to inspect")
    parser.add_argument("--model", default=None, help="Ollama model name")
    parser.add_argument("--host", default=None, help="Ollama host, e.g. http://127.0.0.1:11434")
    parser.add_argument("--knowledge-dir", default=None, help="Directory containing Nova knowledge markdown files")
    parser.add_argument("--projects-dir", default=None, help="Directory where Nova creates projects")
    parser.add_argument("--state-dir", default=None, help="Directory where Nova stores local state")
    parser.add_argument("--access-password", default=None, help="Password for admin bypass or a limited grant")
    parser.add_argument(
        "--profile",
        default=None,
        choices=available_profiles(),
        help="Behavior profile for Nova",
    )

    subparsers = parser.add_subparsers(dest="command", required=True)

    ask = subparsers.add_parser("ask", help="Ask Nova a single coding question")
    ask.add_argument("prompt", help="Question or coding request")

    subparsers.add_parser("chat", help="Interactive chat mode")
    create_project = subparsers.add_parser("create-project", help="Create a starter project in NovaProjects")
    create_project.add_argument("name", help="Project name")
    create_project.add_argument(
        "--template",
        default="basic-python",
        choices=["basic-python", "bash-script", "minecraft-fabric"],
        help="Starter template",
    )
    credits = subparsers.add_parser("credits", help="View or manage Nova credits")
    credits.add_argument(
        "action",
        choices=[
            "status",
            "add",
            "enable",
            "disable",
            "reset",
            "set-admin-password",
            "clear-admin-password",
            "grant-create",
            "grant-list",
            "grant-revoke",
        ],
    )
    credits.add_argument("amount", nargs="?", type=int, default=0, help="Amount for add")
    credits.add_argument("--name", default=None, help="Grant display name")
    credits.add_argument("--prompts", type=int, default=None, help="Grant prompt limit")
    credits.add_argument("--hours", type=int, default=None, help="Grant duration in hours")
    credits.add_argument("--grant-id", default=None, help="Grant id to revoke")
    return parser


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()
    config = NovaConfig.from_args(
        repo_root=args.repo,
        model=args.model,
        ollama_host=args.host,
        profile=args.profile,
        knowledge_dir=args.knowledge_dir,
        projects_dir=args.projects_dir,
        state_dir=args.state_dir,
        credits_enabled=None,
        access_password=args.access_password,
    )
    agent = NovaAgent(config)

    if args.command == "ask":
        try:
            response = agent.answer(args.prompt)
        except RuntimeError as exc:
            print(f"Nova error: {exc}")
            return
        print(response.answer)
        return

    if args.command == "chat":
        run_chat(agent, config)
        return

    if args.command == "create-project":
        print(agent.tools.create_project(args.name, template=args.template).content)
        return

    if args.command == "credits":
        try:
            if args.action == "status":
                info = agent.credits.status(access_password=config.access_password)
            elif args.action == "add":
                _require_admin_password(agent, config)
                info = agent.credits.add(args.amount)
            elif args.action == "enable":
                _require_admin_password(agent, config)
                info = agent.credits.set_enabled(True)
            elif args.action == "set-admin-password":
                if agent.credits.has_admin_password():
                    _require_admin_password(agent, config)
                password = _prompt_password("New admin password: ")
                confirm = _prompt_password("Confirm admin password: ")
                if password != confirm:
                    print("Nova error: passwords did not match.")
                    return
                agent.credits.set_admin_password(password)
                print("Admin password set.")
                return
            elif args.action == "clear-admin-password":
                _require_admin_password(agent, config)
                agent.credits.clear_admin_password()
                print("Admin password cleared.")
                return
            elif args.action == "grant-create":
                _require_admin_password(agent, config)
                name = args.name or "shared-access"
                password = _prompt_password("Grant password: ")
                confirm = _prompt_password("Confirm grant password: ")
                if password != confirm:
                    print("Nova error: passwords did not match.")
                    return
                grant = agent.credits.create_grant(
                    name=name,
                    password=password,
                    prompt_limit=args.prompts,
                    duration_hours=args.hours,
                )
                print(_format_grant(grant))
                return
            elif args.action == "grant-list":
                _require_admin_password(agent, config)
                print(_format_grants(agent.credits.list_grants()))
                return
            elif args.action == "grant-revoke":
                _require_admin_password(agent, config)
                if not args.grant_id:
                    print("Nova error: --grant-id is required for grant-revoke.")
                    return
                removed = agent.credits.revoke_grant(args.grant_id)
                print("Grant revoked." if removed else "Grant not found.")
                return
            elif args.action == "disable":
                _require_admin_password(agent, config)
                info = agent.credits.set_enabled(False)
            else:
                _require_admin_password(agent, config)
                info = agent.credits.reset()
            print(_format_credits(info))
        except RuntimeError as exc:
            print(f"Nova error: {exc}")
        return


def run_chat(agent: NovaAgent, config: NovaConfig) -> None:
    print(f"Nova chat attached to {config.repo_root}")
    print(f"Active profile: {config.profile}")
    print(f"Knowledge dir: {config.knowledge_dir}")
    print(f"Projects dir: {config.projects_dir}")
    print(_format_credits(agent.credits.status(access_password=config.access_password)))
    print("Type /help for commands.")
    while True:
        try:
            raw = input("\nNova> ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\nExiting Nova.")
            return

        if not raw:
            continue
        if raw == "/quit":
            print("Exiting Nova.")
            return
        if raw == "/help":
            print(HELP_TEXT)
            continue
        if raw == "/files":
            print(agent.tools.list_files().content)
            continue
        if raw == "/projects":
            print(agent.tools.list_projects().content)
            continue
        if raw == "/knowledge":
            print(f"Knowledge directory: {config.knowledge_dir}")
            continue
        if raw == "/credits":
            print(_format_credits(agent.credits.status(access_password=config.access_password)))
            continue
        if raw.startswith("/read "):
            print(agent.tools.read_file(raw[6:].strip()).content)
            continue
        if raw.startswith("/search "):
            print(agent.tools.search_text(raw[8:].strip()).content)
            continue
        if raw.startswith("/repo "):
            new_root = Path(raw[6:].strip()).resolve()
            config.repo_root = new_root
            agent = NovaAgent(config)
            print(f"Switched repo to {new_root}")
            continue
        if raw.startswith("/model "):
            config.model = raw[7:].strip()
            agent = NovaAgent(config)
            print(f"Switched model to {config.model}")
            continue
        if raw.startswith("/profile "):
            selected_profile = raw[9:].strip()
            if selected_profile not in available_profiles():
                print(f"Unknown profile. Available: {', '.join(available_profiles())}")
                continue
            config.profile = selected_profile
            config.system_prompt = get_system_prompt(selected_profile)
            agent = NovaAgent(config)
            print(f"Switched profile to {config.profile}")
            continue
        if raw.startswith("/credits "):
            remainder = raw[len("/credits "):].strip()
            if remainder == "status":
                print(_format_credits(agent.credits.status(access_password=config.access_password)))
                continue
            if remainder.startswith("add "):
                amount = int(remainder.split(maxsplit=1)[1])
                print(_format_credits(agent.credits.add(amount)))
                continue
            if remainder == "enable":
                print(_format_credits(agent.credits.set_enabled(True)))
                continue
            if remainder == "disable":
                print(_format_credits(agent.credits.set_enabled(False)))
                continue
            if remainder == "reset":
                print(_format_credits(agent.credits.reset()))
                continue
            print("Usage: /credits status|add <amount>|enable|disable|reset")
            continue
        if raw.startswith("/create-project "):
            remainder = raw[len("/create-project "):].strip()
            if "::" in remainder:
                name, template = [part.strip() for part in remainder.split("::", 1)]
            else:
                name, template = remainder, "basic-python"
            print(agent.tools.create_project(name, template=template).content)
            continue
        if raw.startswith("/run "):
            command = raw[5:].strip()
            confirm = input(f"Run command in {config.repo_root}? [y/N] ").strip().lower()
            if confirm == "y":
                print(agent.tools.run_command(command).content)
            else:
                print("Command cancelled.")
            continue

        try:
            response = agent.answer(raw)
        except RuntimeError as exc:
            print(f"Nova error: {exc}")
            continue
        print(response.answer)


HELP_TEXT = """Commands:
/help              Show this message
/files             List files in the attached repo
/projects          List project folders in NovaProjects
/knowledge         Show the active knowledge directory
/credits           Show credit status or manage credits
/read <path>       Read a file from the repo
/search <text>     Search text in the repo
/run <command>     Run a shell command with approval
/repo <path>       Switch to another repo
/model <name>      Switch Ollama model
/profile <name>    Switch between general, python, bash, and minecraft modes
/create-project    Create a project in NovaProjects. Use name or name::template
/quit              Exit Nova
"""


def _format_credits(info: object) -> str:
    parts = [
        f"Credits: balance={info.balance}",
        f"enabled={str(info.enabled).lower()}",
        f"bypass={str(info.bypass_active).lower()}",
        f"mode={info.bypass_mode}",
    ]
    if getattr(info, "grant_name", None):
        parts.append(f"grant={info.grant_name}")
    if getattr(info, "grant_remaining_prompts", None) is not None:
        parts.append(f"remaining_prompts={info.grant_remaining_prompts}")
    if getattr(info, "grant_expires_at", None):
        parts.append(f"expires_at={info.grant_expires_at}")
    return " ".join(parts)


def _format_grant(grant: dict) -> str:
    return (
        f"Grant created: id={grant['id']} name={grant['name']} "
        f"remaining_prompts={grant['remaining_prompts']} expires_at={grant['expires_at']}"
    )


def _format_grants(grants: list[dict]) -> str:
    if not grants:
        return "(no active grants)"
    return "\n".join(_format_grant(grant) for grant in grants)


def _prompt_password(label: str) -> str:
    return getpass.getpass(label)


def _require_admin_password(agent: NovaAgent, config: NovaConfig) -> None:
    if not agent.credits.has_admin_password():
        return
    password = config.access_password or _prompt_password("Admin password: ")
    if not agent.credits.is_admin_password_valid(password):
        raise RuntimeError("Invalid admin password.")


if __name__ == "__main__":
    main()
