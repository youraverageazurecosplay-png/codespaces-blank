# Workspace Projects

This repository currently contains a few separate projects. This README is the quick map for what each one is and how to run it.

## Projects

### [Apex Client](/workspaces/codespaces-blank/Apex%20Client)

A Fabric client-side Minecraft utility mod with combat helpers, render tools, inventory automation, hotkeys, and other utility features.

What it is for:

- client-side Minecraft utility features
- combat and render helpers
- inventory and macro automation

How to build it:

```bash
cd "/workspaces/codespaces-blank/Apex Client"
gradle build -x test
```

Build output:

- `build/libs/clientutils-0.1.0.jar`

Main docs:

- [Apex Client/README.md](/workspaces/codespaces-blank/Apex%20Client/README.md)

### [Echo](/workspaces/codespaces-blank/Echo)

An offline-first AI photo editing app with a local Gradio UI and image-processing tools like enhancement, blur, resizing, masking, and prompt-style edits.

What it is for:

- local image editing
- browser-based offline UI
- computer vision powered photo workflows

How to run it:

```bash
cd /workspaces/codespaces-blank/Echo
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python app.py
```

Then open the local Gradio URL shown in the terminal.

Main docs:

- [Echo/README.md](/workspaces/codespaces-blank/Echo/README.md)

### [Nova](/workspaces/codespaces-blank/Nova)

A local-first coding assistant project built around Ollama, repo inspection tools, profiles for Python/Bash/Minecraft, local knowledge files, and a simple credits system.

What it is for:

- offline or local-first coding help
- repo-aware text prompts
- starter project creation
- customizable knowledge and profiles

How to run it:

```bash
ollama serve
```

In another terminal:

```bash
cd /workspaces/codespaces-blank
./nova --profile python ask "Explain this repository"
```

Interactive chat:

```bash
cd /workspaces/codespaces-blank
./nova --profile minecraft chat
```

Create a new Nova-managed starter project:

```bash
cd /workspaces/codespaces-blank
./nova create-project "My Tool"
```

Main docs:

- [Nova/README.md](/workspaces/codespaces-blank/Nova/README.md)

### [NovaProjects](/workspaces/codespaces-blank/NovaProjects)

This is Nova's project workspace. New starter projects created by Nova are placed here so they stay separate from the other main projects in the repo.

What it is for:

- Nova-created starter apps
- experiments and generated project scaffolds

Current example:

- [NovaProjects/test-project](/workspaces/codespaces-blank/NovaProjects/test-project)

## Quick Start

If you want to try each project fast:

1. `Apex Client`: build the mod with `gradle build -x test`
2. `Echo`: install requirements and run `python app.py`
3. `Nova`: start Ollama, then run `./nova --profile python chat`

## Notes

- `Nova` depends on a local Ollama server for model responses.
- `Echo` is a local Python app and needs its Python dependencies installed.
- `Apex Client` is a Minecraft mod project, so the main output is a built jar rather than a long-running local app.
