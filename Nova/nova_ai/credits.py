from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta, UTC
import hashlib
import json
from pathlib import Path
import secrets


@dataclass(slots=True)
class CreditStatus:
    enabled: bool
    balance: int
    bypass_active: bool
    bypass_mode: str
    grant_name: str | None = None
    grant_remaining_prompts: int | None = None
    grant_expires_at: str | None = None


class CreditManager:
    def __init__(self, state_file: Path, default_credits: int = 100) -> None:
        self.state_file = state_file
        self.default_credits = default_credits
        self.state_file.parent.mkdir(parents=True, exist_ok=True)
        if not self.state_file.exists():
            self._write_state(self._default_state())

    def status(self, access_password: str | None = None) -> CreditStatus:
        state = self._read_state()
        grant = self._resolve_access(access_password, state)
        if grant is None:
            return CreditStatus(
                enabled=bool(state.get("enabled", True)),
                balance=int(state.get("credits", self.default_credits)),
                bypass_active=False,
                bypass_mode="none",
            )
        if grant == "admin":
            return CreditStatus(
                enabled=bool(state.get("enabled", True)),
                balance=int(state.get("credits", self.default_credits)),
                bypass_active=True,
                bypass_mode="admin",
            )
        return CreditStatus(
            enabled=bool(state.get("enabled", True)),
            balance=int(state.get("credits", self.default_credits)),
            bypass_active=True,
            bypass_mode="grant",
            grant_name=grant["name"],
            grant_remaining_prompts=grant.get("remaining_prompts"),
            grant_expires_at=grant.get("expires_at"),
        )

    def ensure_can_spend(self, cost: int, access_password: str | None = None) -> CreditStatus:
        state = self._read_state()
        grant = self._resolve_access(access_password, state)
        info = self.status(access_password)
        if grant is not None or not info.enabled:
            return info
        if info.balance < cost:
            raise RuntimeError(
                "Nova credits are exhausted. Add credits, disable credits, or use a valid bypass password."
            )
        return info

    def spend(self, cost: int, access_password: str | None = None) -> CreditStatus:
        state = self._read_state()
        grant = self._resolve_access(access_password, state)
        if grant == "admin" or not bool(state.get("enabled", True)):
            return self.status(access_password)
        if isinstance(grant, dict):
            if grant.get("remaining_prompts") is not None:
                grant["remaining_prompts"] = max(0, int(grant["remaining_prompts"]) - cost)
            self._prune_invalid_grants(state)
            self._write_state(state)
            return self.status(access_password)

        state["credits"] = max(0, int(state.get("credits", self.default_credits)) - cost)
        self._write_state(state)
        return self.status(access_password)

    def add(self, amount: int) -> CreditStatus:
        state = self._read_state()
        state["credits"] = int(state.get("credits", self.default_credits)) + amount
        self._write_state(state)
        return self.status()

    def set_enabled(self, enabled: bool) -> CreditStatus:
        state = self._read_state()
        state["enabled"] = enabled
        self._write_state(state)
        return self.status()

    def reset(self) -> CreditStatus:
        self._write_state(self._default_state())
        return self.status()

    def set_admin_password(self, password: str) -> None:
        state = self._read_state()
        state["admin_password"] = self._hash_password(password)
        self._write_state(state)

    def has_admin_password(self) -> bool:
        state = self._read_state()
        return bool(state.get("admin_password"))

    def is_admin_password_valid(self, password: str) -> bool:
        state = self._read_state()
        stored = state.get("admin_password")
        if not stored:
            return False
        return self._verify_password(password, stored)

    def clear_admin_password(self) -> None:
        state = self._read_state()
        state["admin_password"] = None
        self._write_state(state)

    def create_grant(
        self,
        name: str,
        password: str,
        prompt_limit: int | None = None,
        duration_hours: int | None = None,
    ) -> dict:
        state = self._read_state()
        expires_at = None
        if duration_hours is not None:
            expires_at = (datetime.now(UTC) + timedelta(hours=duration_hours)).isoformat()
        grant = {
            "id": secrets.token_hex(4),
            "name": name,
            "password": self._hash_password(password),
            "remaining_prompts": prompt_limit,
            "expires_at": expires_at,
        }
        state.setdefault("grants", []).append(grant)
        self._write_state(state)
        return {
            "id": grant["id"],
            "name": grant["name"],
            "remaining_prompts": grant["remaining_prompts"],
            "expires_at": grant["expires_at"],
        }

    def list_grants(self) -> list[dict]:
        state = self._read_state()
        self._prune_invalid_grants(state)
        self._write_state(state)
        result = []
        for grant in state.get("grants", []):
            result.append(
                {
                    "id": grant["id"],
                    "name": grant["name"],
                    "remaining_prompts": grant.get("remaining_prompts"),
                    "expires_at": grant.get("expires_at"),
                }
            )
        return result

    def revoke_grant(self, grant_id: str) -> bool:
        state = self._read_state()
        original = len(state.get("grants", []))
        state["grants"] = [grant for grant in state.get("grants", []) if grant.get("id") != grant_id]
        self._write_state(state)
        return len(state["grants"]) != original

    def _default_state(self) -> dict:
        return {
            "credits": self.default_credits,
            "enabled": True,
            "admin_password": None,
            "grants": [],
        }

    def _resolve_access(self, access_password: str | None, state: dict) -> str | dict | None:
        if not access_password:
            return None
        admin_password = state.get("admin_password")
        if admin_password and self._verify_password(access_password, admin_password):
            return "admin"

        self._prune_invalid_grants(state)
        for grant in state.get("grants", []):
            if self._verify_password(access_password, grant["password"]):
                return grant
        return None

    def _prune_invalid_grants(self, state: dict) -> None:
        now = datetime.now(UTC)
        kept = []
        for grant in state.get("grants", []):
            expires_at = grant.get("expires_at")
            remaining_prompts = grant.get("remaining_prompts")
            if expires_at:
                expiry = datetime.fromisoformat(expires_at)
                if expiry <= now:
                    continue
            if remaining_prompts is not None and int(remaining_prompts) <= 0:
                continue
            kept.append(grant)
        state["grants"] = kept

    def _hash_password(self, password: str) -> str:
        salt = secrets.token_bytes(16)
        digest = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, 200_000)
        return f"{salt.hex()}:{digest.hex()}"

    def _verify_password(self, password: str, stored: str) -> bool:
        salt_hex, digest_hex = stored.split(":", 1)
        salt = bytes.fromhex(salt_hex)
        digest = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, 200_000)
        return secrets.compare_digest(digest.hex(), digest_hex)

    def _read_state(self) -> dict:
        return json.loads(self.state_file.read_text(encoding="utf-8"))

    def _write_state(self, state: dict) -> None:
        self.state_file.write_text(json.dumps(state, indent=2), encoding="utf-8")
