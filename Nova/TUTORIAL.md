# Nova Tutorial

This tutorial is for someone using Nova for the first time.

## 1. Quick Setup

For a one-click setup in any new repository:

```bash
# Copy install.sh to your repo and run:
./install.sh
```

This will install Python dependencies, Ollama, the model, and start the server.

## 2. What Nova Is

Nova is a local-first coding assistant.
You talk to it with normal text prompts.
It can:

- answer coding questions
- inspect a repository
- read files
- search code
- suggest patches
- create starter projects in `NovaProjects/`

## 3. Manual Setup (if not using install.sh)

Nova expects Ollama to be running locally.

In one terminal:

```bash
ollama serve
```

If you do not already have a model:

```bash
ollama pull qwen2.5-coder:7b
```

## 3. Run Nova

From the repo root:

```bash
./nova --profile python ask "Explain this repository"
```

Or open chat mode:

```bash
./nova --profile minecraft chat
```

Profiles:

- `general`
- `python`
- `bash`
- `minecraft`

## 4. Ask Text Prompts

Nova follows plain text prompts.

Examples:

```bash
./nova --profile python ask "Help me write a CLI tool that scans folders"
./nova --profile bash ask "Write a safe backup script with rollback checks"
./nova --profile minecraft ask "How should I structure a Fabric networking system?"
```

## 5. Useful Chat Commands

Inside `chat` mode:

- `/help`
- `/files`
- `/read path/to/file.py`
- `/search keyword`
- `/projects`
- `/knowledge`
- `/credits`
- `/create-project My App`
- `/quit`

## 6. Create Projects

Nova creates starter projects in `NovaProjects/`.

Examples:

```bash
./nova create-project "Potion Helper"
./nova create-project "Backup Script" --template bash-script
./nova create-project "Crystal HUD" --template minecraft-fabric
```

## 7. Teach Nova

Nova reads local knowledge files before answering:

- `Nova/knowledge/common.md`
- `Nova/knowledge/python.md`
- `Nova/knowledge/bash.md`
- `Nova/knowledge/minecraft.md`

Add your own rules there, such as:

- coding style
- favorite libraries
- commands you trust
- Minecraft versions and platforms
- patterns to avoid

## 8. Credits

By default, prompts use credits.

Check status:

```bash
./nova credits status
```

Add credits:

```bash
./nova credits add 25
```

Disable or enable credits:

```bash
./nova credits disable
./nova credits enable
```

## 9. Password Bypass

Nova no longer uses an open bypass flag by itself.
Bypass access now needs a password.

Set an admin password:

```bash
./nova credits set-admin-password
```

Then use it when launching Nova:

```bash
./nova --access-password "your-password" --profile python ask "Help me debug this error"
```

You can also export it:

```bash
export NOVA_ACCESS_PASSWORD="your-password"
```

## 10. Limited Access For Other People

You can create a grant with:

- a password
- a prompt limit
- a time limit
- or both

Examples:

```bash
./nova credits grant-create --name "friend-pass" --prompts 20
./nova credits grant-create --name "weekend-pass" --hours 48
./nova credits grant-create --name "trial-pass" --prompts 10 --hours 24
```

List grants:

```bash
./nova credits grant-list
```

Revoke a grant:

```bash
./nova credits grant-revoke --grant-id abc123ef
```

The other person uses the password you gave them:

```bash
./nova --access-password "shared-password" --profile bash ask "Help me write a deployment script"
```

If a grant has a prompt limit, Nova reduces it after each prompt.
If a grant has an expiry time, Nova stops accepting it after that time.
