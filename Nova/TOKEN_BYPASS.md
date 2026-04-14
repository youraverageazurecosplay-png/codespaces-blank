# Nova AI - Token Bypass & Credits Guide

## Token Bypass (Unlimited Prompts)

Nova AI uses a credit system to track and limit prompts. You can bypass this in several ways:

### **Option 1: Web GUI (Easiest)**
1. Start the web server: `nova web`
2. In the sidebar, click "Bypass Tokens"
3. Enter your admin password
4. Click "Enable"
5. Send prompts - they'll bypass the credit system

### **Option 2: CLI with Access Password**
```bash
nova --access-password "admin" ask "Your question"
nova --access-password "admin" chat
```

### **Option 3: Disable Credits Entirely**
```bash
nova credits disable
# Then prompts don't cost any credits
```

### **Option 4: Environment Variable**
```bash
export NOVA_BYPASS_PASSWORD="admin"
nova ask "Your question"
```

## Credit System Details

- **Balance:** Each prompt costs 1 credit (default)
- **Default:** 100 credits enabled at startup
- **Check balance:** `nova credits status`
- **Add credits:** `nova credits add 50` (requires admin password)
- **Reset:** `nova credits reset` (requires admin password)

## Admin Password

The admin password controls token management and credit operations.

### Set Admin Password
```bash
nova credits set-admin-password
# Enter a new password when prompted
```

### Clear Admin Password
```bash
nova credits clear-admin-password
# This requires the existing admin password
```

## Common Issues

**"Invalid admin password"**
- You need to set an admin password first with `set-admin-password`
- Or use `disable` to turn off credits entirely

**"Create grants"** (for shared access)
```bash
nova credits grant-create --name "shared" --prompts 50 --hours 24
```

This creates a time-limited, token-limited access key for others to use.
