"""FloroBeat Real-Time Messenger Backend Service.

Provides complete real-time messaging, user discovery, presence tracking,
typing indicators, delivery receipts, and SQLite persistence.
Operates fully standalone or mounted into the FloroBeat backend.
"""

from __future__ import annotations

import asyncio
import json
import logging
import os
import sqlite3
import time
from collections import defaultdict
from typing import Any, Optional

from fastapi import APIRouter, Header, HTTPException, Query, WebSocket, WebSocketDisconnect
from pydantic import BaseModel, Field

log = logging.getLogger("messenger")

DB_DIR = os.environ.get("MESSAGING_DATA_DIR", os.path.join(os.path.dirname(__file__), "..", "data"))
os.makedirs(DB_DIR, exist_ok=True)
DB_PATH = os.path.join(DB_DIR, "messenger.db")


# ----------------------------------------------------------- SQLite Storage ----


class MessengerDatabase:
    """Thread-safe SQLite storage for FloroBeat Messenger."""

    def __init__(self, path: str = DB_PATH):
        self.path = path
        self._init_db()

    def _get_connection(self) -> sqlite3.Connection:
        conn = sqlite3.connect(self.path, check_same_thread=False)
        conn.row_factory = sqlite3.Row
        conn.execute("PRAGMA journal_mode = WAL")
        conn.execute("PRAGMA foreign_keys = ON")
        return conn

    def _init_db(self):
        with self._get_connection() as conn:
            conn.executescript(
                """
                CREATE TABLE IF NOT EXISTS users (
                    user_id TEXT PRIMARY KEY,
                    username TEXT UNIQUE,
                    display_name TEXT NOT NULL,
                    avatar_url TEXT,
                    bio TEXT,
                    fcm_token TEXT,
                    last_seen_at INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                );

                CREATE TABLE IF NOT EXISTS conversations (
                    id TEXT PRIMARY KEY,
                    participant1 TEXT NOT NULL,
                    participant2 TEXT NOT NULL,
                    updated_at INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    UNIQUE(participant1, participant2)
                );

                CREATE TABLE IF NOT EXISTS messages (
                    id TEXT PRIMARY KEY,
                    conversation_id TEXT NOT NULL,
                    sender_id TEXT NOT NULL,
                    receiver_id TEXT NOT NULL,
                    content TEXT NOT NULL,
                    type TEXT NOT NULL DEFAULT 'TEXT',
                    status TEXT NOT NULL DEFAULT 'SENT',
                    reply_to_id TEXT,
                    created_at INTEGER NOT NULL,
                    edited_at INTEGER,
                    deleted_everyone INTEGER NOT NULL DEFAULT 0,
                    deleted_by_sender INTEGER NOT NULL DEFAULT 0,
                    deleted_by_receiver INTEGER NOT NULL DEFAULT 0
                );

                CREATE TABLE IF NOT EXISTS blocks (
                    blocker_id TEXT NOT NULL,
                    blocked_id TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    PRIMARY KEY (blocker_id, blocked_id)
                );

                CREATE TABLE IF NOT EXISTS reports (
                    id TEXT PRIMARY KEY,
                    reporter_id TEXT NOT NULL,
                    reported_id TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    details TEXT,
                    created_at INTEGER NOT NULL
                );

                CREATE INDEX IF NOT EXISTS idx_messages_conv ON messages(conversation_id, created_at DESC);
                CREATE INDEX IF NOT EXISTS idx_messages_receiver ON messages(receiver_id, status);
                CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
                CREATE INDEX IF NOT EXISTS idx_users_display_name ON users(display_name);
                """
            )

    def upsert_user(
        self,
        user_id: str,
        display_name: str,
        username: Optional[str] = None,
        avatar_url: Optional[str] = None,
        bio: Optional[str] = None,
        fcm_token: Optional[str] = None,
    ) -> dict[str, Any]:
        now = int(time.time() * 1000)
        clean_handle = (username or user_id[:8]).strip().lstrip("@").lower()
        with self._get_connection() as conn:
            existing = conn.execute("SELECT * FROM users WHERE user_id = ?", (user_id,)).fetchone()
            if existing:
                conn.execute(
                    """
                    UPDATE users SET
                        display_name = coalesce(?, display_name),
                        username = coalesce(?, username),
                        avatar_url = coalesce(?, avatar_url),
                        bio = coalesce(?, bio),
                        fcm_token = coalesce(?, fcm_token),
                        last_seen_at = ?
                    WHERE user_id = ?
                    """,
                    (display_name, clean_handle, avatar_url, bio, fcm_token, now, user_id),
                )
            else:
                conn.execute(
                    """
                    INSERT INTO users (user_id, username, display_name, avatar_url, bio, fcm_token, last_seen_at, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    (user_id, clean_handle, display_name, avatar_url, bio or "", fcm_token, now, now),
                )
            user = conn.execute("SELECT * FROM users WHERE user_id = ?", (user_id,)).fetchone()
            return dict(user)

    def update_last_seen(self, user_id: str):
        now = int(time.time() * 1000)
        with self._get_connection() as conn:
            conn.execute("UPDATE users SET last_seen_at = ? WHERE user_id = ?", (now, user_id))

    def get_user(self, user_id: str) -> Optional[dict[str, Any]]:
        with self._get_connection() as conn:
            row = conn.execute("SELECT * FROM users WHERE user_id = ?", (user_id,)).fetchone()
            return dict(row) if row else None

    def search_users(self, query: str, exclude_user_id: str, limit: int = 30) -> list[dict[str, Any]]:
        q = query.strip().lstrip("@").lower()
        if not q:
            return []
        with self._get_connection() as conn:
            rows = conn.execute(
                """
                SELECT * FROM users
                WHERE user_id != ?
                  AND (lower(username) LIKE ? OR lower(display_name) LIKE ?)
                  AND user_id NOT IN (SELECT blocked_id FROM blocks WHERE blocker_id = ?)
                  AND user_id NOT IN (SELECT blocker_id FROM blocks WHERE blocked_id = ?)
                ORDER BY last_seen_at DESC
                LIMIT ?
                """,
                (exclude_user_id, f"%{q}%", f"%{q}%", exclude_user_id, exclude_user_id, limit),
            ).fetchall()
            return [dict(r) for r in rows]

    def get_or_create_conversation(self, user_a: str, user_b: str) -> dict[str, Any]:
        p1, p2 = sorted([user_a, user_b])
        conv_id = f"c_{p1}_{p2}"
        now = int(time.time() * 1000)
        with self._get_connection() as conn:
            row = conn.execute("SELECT * FROM conversations WHERE id = ?", (conv_id,)).fetchone()
            if not row:
                conn.execute(
                    "INSERT INTO conversations (id, participant1, participant2, updated_at, created_at) VALUES (?, ?, ?, ?, ?)",
                    (conv_id, p1, p2, now, now),
                )
                row = conn.execute("SELECT * FROM conversations WHERE id = ?", (conv_id,)).fetchone()
            return dict(row)

    def is_blocked(self, user_a: str, user_b: str) -> bool:
        with self._get_connection() as conn:
            row = conn.execute(
                """
                SELECT 1 FROM blocks
                WHERE (blocker_id = ? AND blocked_id = ?)
                   OR (blocker_id = ? AND blocked_id = ?)
                """,
                (user_a, user_b, user_b, user_a),
            ).fetchone()
            return row is not None

    def save_message(
        self,
        message_id: str,
        conversation_id: str,
        sender_id: str,
        receiver_id: str,
        content: str,
        msg_type: str = "TEXT",
        reply_to_id: Optional[str] = None,
        created_at: Optional[int] = None,
        status: str = "SENT",
    ) -> dict[str, Any]:
        now = created_at or int(time.time() * 1000)
        with self._get_connection() as conn:
            conn.execute(
                """
                INSERT INTO messages (
                    id, conversation_id, sender_id, receiver_id, content,
                    type, status, reply_to_id, created_at, edited_at,
                    deleted_everyone, deleted_by_sender, deleted_by_receiver
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, 0, 0, 0)
                """,
                (message_id, conversation_id, sender_id, receiver_id, content, msg_type, status, reply_to_id, now),
            )
            conn.execute(
                "UPDATE conversations SET updated_at = ? WHERE id = ?",
                (now, conversation_id),
            )
            row = conn.execute("SELECT * FROM messages WHERE id = ?", (message_id,)).fetchone()
            return dict(row)

    def mark_delivered(self, message_id: str) -> Optional[dict[str, Any]]:
        with self._get_connection() as conn:
            conn.execute(
                "UPDATE messages SET status = 'DELIVERED' WHERE id = ? AND status = 'SENT'",
                (message_id,),
            )
            row = conn.execute("SELECT * FROM messages WHERE id = ?", (message_id,)).fetchone()
            return dict(row) if row else None

    def mark_read(self, conversation_id: str, reader_id: str, up_to_time: Optional[int] = None) -> list[str]:
        now = up_to_time or int(time.time() * 1000)
        with self._get_connection() as conn:
            rows = conn.execute(
                """
                SELECT id FROM messages
                WHERE conversation_id = ? AND receiver_id = ? AND created_at <= ? AND status != 'READ'
                """,
                (conversation_id, reader_id, now),
            ).fetchall()
            updated_ids = [r["id"] for r in rows]
            if updated_ids:
                conn.execute(
                    """
                    UPDATE messages SET status = 'READ'
                    WHERE conversation_id = ? AND receiver_id = ? AND created_at <= ? AND status != 'READ'
                    """,
                    (conversation_id, reader_id, now),
                )
            return updated_ids

    def get_messages(
        self,
        conversation_id: str,
        user_id: str,
        limit: int = 50,
        before_timestamp: Optional[int] = None,
    ) -> list[dict[str, Any]]:
        with self._get_connection() as conn:
            base_query = """
                SELECT * FROM messages
                WHERE conversation_id = ?
                  AND deleted_everyone = 0
                  AND (
                    (sender_id = ? AND deleted_by_sender = 0) OR
                    (receiver_id = ? AND deleted_by_receiver = 0)
                  )
            """
            params: list[Any] = [conversation_id, user_id, user_id]
            if before_timestamp:
                base_query += " AND created_at < ?"
                params.append(before_timestamp)

            base_query += " ORDER BY created_at DESC LIMIT ?"
            params.append(limit)

            rows = conn.execute(base_query, tuple(params)).fetchall()
            return [dict(r) for r in reversed(rows)]

    def delete_message(self, message_id: str, user_id: str, for_everyone: bool) -> bool:
        with self._get_connection() as conn:
            msg = conn.execute("SELECT * FROM messages WHERE id = ?", (message_id,)).fetchone()
            if not msg:
                return False

            if for_everyone:
                if msg["sender_id"] != user_id:
                    return False  # Only sender can delete for everyone
                conn.execute(
                    "UPDATE messages SET deleted_everyone = 1 WHERE id = ?",
                    (message_id,),
                )
            else:
                if msg["sender_id"] == user_id:
                    conn.execute("UPDATE messages SET deleted_by_sender = 1 WHERE id = ?", (message_id,))
                else:
                    conn.execute("UPDATE messages SET deleted_by_receiver = 1 WHERE id = ?", (message_id,))
            return True

    def get_conversations(self, user_id: str) -> list[dict[str, Any]]:
        with self._get_connection() as conn:
            conv_rows = conn.execute(
                """
                SELECT * FROM conversations
                WHERE participant1 = ? OR participant2 = ?
                ORDER BY updated_at DESC
                """,
                (user_id, user_id),
            ).fetchall()

            result = []
            for c in conv_rows:
                other_id = c["participant2"] if c["participant1"] == user_id else c["participant1"]
                other_user = conn.execute("SELECT * FROM users WHERE user_id = ?", (other_id,)).fetchone()
                if not other_user:
                    continue

                # Get latest message
                last_msg = conn.execute(
                    """
                    SELECT * FROM messages
                    WHERE conversation_id = ?
                      AND deleted_everyone = 0
                      AND (
                        (sender_id = ? AND deleted_by_sender = 0) OR
                        (receiver_id = ? AND deleted_by_receiver = 0)
                      )
                    ORDER BY created_at DESC LIMIT 1
                    """,
                    (c["id"], user_id, user_id),
                ).fetchone()

                # Get unread count
                unread = conn.execute(
                    """
                    SELECT COUNT(*) as count FROM messages
                    WHERE conversation_id = ? AND receiver_id = ? AND status != 'READ' AND deleted_everyone = 0 AND deleted_by_receiver = 0
                    """,
                    (c["id"], user_id),
                ).fetchone()["count"]

                result.append({
                    "id": c["id"],
                    "otherUser": dict(other_user),
                    "lastMessage": dict(last_msg) if last_msg else None,
                    "unreadCount": unread,
                    "updatedAt": c["updated_at"],
                })
            return result

    def block_user(self, blocker_id: str, blocked_id: str):
        now = int(time.time() * 1000)
        with self._get_connection() as conn:
            conn.execute(
                "INSERT OR REPLACE INTO blocks (blocker_id, blocked_id, created_at) VALUES (?, ?, ?)",
                (blocker_id, blocked_id, now),
            )

    def unblock_user(self, blocker_id: str, blocked_id: str):
        with self._get_connection() as conn:
            conn.execute(
                "DELETE FROM blocks WHERE blocker_id = ? AND blocked_id = ?",
                (blocker_id, blocked_id),
            )

    def report_user(self, reporter_id: str, reported_id: str, reason: str, details: Optional[str] = None):
        now = int(time.time() * 1000)
        rep_id = f"rep_{now}_{reporter_id[:6]}"
        with self._get_connection() as conn:
            conn.execute(
                "INSERT INTO reports (id, reporter_id, reported_id, reason, details, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                (rep_id, reporter_id, reported_id, reason, details or "", now),
            )


db = MessengerDatabase()


# ------------------------------------------------------------- Real-time Hub ----


class MessagingHub:
    """Manages active WebSockets and dispatches real-time events between users."""

    def __init__(self):
        # Maps user_id -> set of active WebSockets
        self.connections: dict[str, set[WebSocket]] = defaultdict(set)
        # Maps user_id -> last ping time
        self.last_seen: dict[str, int] = {}

    def is_online(self, user_id: str) -> bool:
        return bool(self.connections.get(user_id))

    async def connect(self, user_id: str, websocket: WebSocket):
        await websocket.accept()
        self.connections[user_id].add(websocket)
        self.last_seen[user_id] = int(time.time() * 1000)
        db.update_last_seen(user_id)
        await self.broadcast_presence(user_id, is_online=True)

    async def disconnect(self, user_id: str, websocket: WebSocket):
        if user_id in self.connections:
            self.connections[user_id].discard(websocket)
            if not self.connections[user_id]:
                del self.connections[user_id]
                self.last_seen[user_id] = int(time.time() * 1000)
                db.update_last_seen(user_id)
                await self.broadcast_presence(user_id, is_online=False)

    async def send_to_user(self, user_id: str, payload: dict[str, Any]) -> bool:
        sockets = self.connections.get(user_id, set())
        if not sockets:
            return False
        dead = []
        delivered = False
        text = json.dumps(payload)
        for ws in list(sockets):
            try:
                await ws.send_text(text)
                delivered = True
            except Exception:
                dead.append(ws)
        for d in dead:
            sockets.discard(d)
        return delivered

    async def broadcast_presence(self, user_id: str, is_online: bool):
        now = int(time.time() * 1000)
        payload = {
            "type": "presence",
            "userId": user_id,
            "isOnline": is_online,
            "lastSeenAt": now,
        }
        # Broadcast to all connected active users
        text = json.dumps(payload)
        for target_id, sockets in list(self.connections.items()):
            if target_id == user_id:
                continue
            for ws in list(sockets):
                try:
                    await ws.send_text(text)
                except Exception:
                    pass


hub = MessagingHub()


# ----------------------------------------------------------- Pydantic Models ----


class ProfileUpdateRequest(BaseModel):
    user_id: str
    display_name: str
    username: Optional[str] = None
    avatar_url: Optional[str] = None
    bio: Optional[str] = None
    fcm_token: Optional[str] = None


class StartConversationRequest(BaseModel):
    target_user_id: str


class SendMessageRequest(BaseModel):
    id: Optional[str] = None
    content: str
    type: str = "TEXT"
    reply_to_id: Optional[str] = None


class BlockRequest(BaseModel):
    target_user_id: str


class ReportRequest(BaseModel):
    target_user_id: str
    reason: str
    details: Optional[str] = None


# ------------------------------------------------------------- REST Endpoints ----

router = APIRouter(prefix="/api/messaging", tags=["messaging"])


@router.post("/profile")
async def update_profile(req: ProfileUpdateRequest) -> dict[str, Any]:
    user = db.upsert_user(
        user_id=req.user_id,
        display_name=req.display_name,
        username=req.username,
        avatar_url=req.avatar_url,
        bio=req.bio,
        fcm_token=req.fcm_token,
    )
    user["isOnline"] = hub.is_online(req.user_id)
    return user


@router.get("/users/search")
async def search_users(
    q: str,
    x_user_id: str = Header(..., alias="X-User-Id"),
) -> dict[str, Any]:
    results = db.search_users(query=q, exclude_user_id=x_user_id)
    for u in results:
        u["isOnline"] = hub.is_online(u["user_id"])
    return {"users": results}


@router.get("/users/{target_id}")
async def get_user_profile(
    target_id: str,
    x_user_id: str = Header(..., alias="X-User-Id"),
) -> dict[str, Any]:
    user = db.get_user(target_id)
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    user["isOnline"] = hub.is_online(target_id)
    user["isBlocked"] = db.is_blocked(x_user_id, target_id)
    return user


@router.get("/conversations")
async def get_conversations(x_user_id: str = Header(..., alias="X-User-Id")) -> dict[str, Any]:
    conversations = db.get_conversations(x_user_id)
    for c in conversations:
        c["otherUser"]["isOnline"] = hub.is_online(c["otherUser"]["user_id"])
    return {"conversations": conversations}


@router.post("/conversations")
async def start_conversation(
    req: StartConversationRequest,
    x_user_id: str = Header(..., alias="X-User-Id"),
) -> dict[str, Any]:
    if req.target_user_id == x_user_id:
        raise HTTPException(status_code=400, detail="Cannot chat with yourself")

    if db.is_blocked(x_user_id, req.target_user_id):
        raise HTTPException(status_code=403, detail="Communication is blocked")

    target = db.get_user(req.target_user_id)
    if not target:
        raise HTTPException(status_code=404, detail="Target user not found")

    conv = db.get_or_create_conversation(x_user_id, req.target_user_id)
    target["isOnline"] = hub.is_online(req.target_user_id)
    return {
        "id": conv["id"],
        "otherUser": target,
        "updatedAt": conv["updated_at"],
    }


@router.get("/conversations/{conversation_id}/messages")
async def get_messages(
    conversation_id: str,
    limit: int = Query(50, ge=1, le=100),
    before: Optional[int] = None,
    x_user_id: str = Header(..., alias="X-User-Id"),
) -> dict[str, Any]:
    messages = db.get_messages(
        conversation_id=conversation_id,
        user_id=x_user_id,
        limit=limit,
        before_timestamp=before,
    )
    return {"messages": messages}


@router.post("/conversations/{conversation_id}/messages")
async def send_message_rest(
    conversation_id: str,
    req: SendMessageRequest,
    x_user_id: str = Header(..., alias="X-User-Id"),
) -> dict[str, Any]:
    with db._get_connection() as conn:
        conv = conn.execute("SELECT * FROM conversations WHERE id = ?", (conversation_id,)).fetchone()
        if not conv:
            raise HTTPException(status_code=404, detail="Conversation not found")
        if x_user_id not in (conv["participant1"], conv["participant2"]):
            raise HTTPException(status_code=403, detail="Not a participant in this conversation")

        receiver_id = conv["participant2"] if conv["participant1"] == x_user_id else conv["participant1"]

    if db.is_blocked(x_user_id, receiver_id):
        raise HTTPException(status_code=403, detail="User is blocked")

    msg_id = req.id or f"m_{int(time.time() * 1000)}_{x_user_id[:6]}"
    is_receiver_online = hub.is_online(receiver_id)
    initial_status = "DELIVERED" if is_receiver_online else "SENT"

    msg = db.save_message(
        message_id=msg_id,
        conversation_id=conversation_id,
        sender_id=x_user_id,
        receiver_id=receiver_id,
        content=req.content,
        msg_type=req.type,
        reply_to_id=req.reply_to_id,
        status=initial_status,
    )

    # Deliver via WebSocket if receiver is online
    if is_receiver_online:
        await hub.send_to_user(receiver_id, {
            "type": "message",
            "message": msg,
        })

    return msg


@router.post("/messages/{message_id}/read")
async def mark_read(
    message_id: str,
    x_user_id: str = Header(..., alias="X-User-Id"),
) -> dict[str, Any]:
    with db._get_connection() as conn:
        msg = conn.execute("SELECT * FROM messages WHERE id = ?", (message_id,)).fetchone()
        if not msg:
            raise HTTPException(status_code=404, detail="Message not found")
        if msg["receiver_id"] != x_user_id:
            raise HTTPException(status_code=403, detail="Not authorized")

        conv_id = msg["conversation_id"]
        sender_id = msg["sender_id"]

    updated_ids = db.mark_read(conv_id, x_user_id, msg["created_at"])
    if updated_ids:
        await hub.send_to_user(sender_id, {
            "type": "read_receipt",
            "conversationId": conv_id,
            "messageIds": updated_ids,
        })
    return {"ok": True, "readCount": len(updated_ids)}


@router.delete("/messages/{message_id}")
async def delete_message(
    message_id: str,
    for_everyone: bool = Query(False),
    x_user_id: str = Header(..., alias="X-User-Id"),
) -> dict[str, Any]:
    with db._get_connection() as conn:
        msg = conn.execute("SELECT * FROM messages WHERE id = ?", (message_id,)).fetchone()
        if not msg:
            raise HTTPException(status_code=404, detail="Message not found")
        receiver_id = msg["receiver_id"] if msg["sender_id"] == x_user_id else msg["sender_id"]
        conv_id = msg["conversation_id"]

    success = db.delete_message(message_id, x_user_id, for_everyone=for_everyone)
    if not success:
        raise HTTPException(status_code=403, detail="Cannot delete message")

    if for_everyone:
        await hub.send_to_user(receiver_id, {
            "type": "message_deleted",
            "messageId": message_id,
            "conversationId": conv_id,
            "forEveryone": True,
        })

    return {"ok": True}


@router.post("/users/{target_id}/block")
async def block_user(target_id: str, x_user_id: str = Header(..., alias="X-User-Id")) -> dict[str, Any]:
    db.block_user(x_user_id, target_id)
    return {"ok": True}


@router.delete("/users/{target_id}/block")
async def unblock_user(target_id: str, x_user_id: str = Header(..., alias="X-User-Id")) -> dict[str, Any]:
    db.unblock_user(x_user_id, target_id)
    return {"ok": True}


@router.post("/users/{target_id}/report")
async def report_user(
    target_id: str,
    req: ReportRequest,
    x_user_id: str = Header(..., alias="X-User-Id"),
) -> dict[str, Any]:
    db.report_user(x_user_id, target_id, req.reason, req.details)
    return {"ok": True}


# --------------------------------------------------------- WebSocket Endpoint ----


@router.websocket("/ws/{user_id}")
async def websocket_endpoint(websocket: WebSocket, user_id: str):
    await hub.connect(user_id, websocket)
    try:
        while True:
            data = await websocket.receive_text()
            frame = json.loads(data)
            action = frame.get("type")

            if action == "ping":
                await websocket.send_text(json.dumps({"type": "pong", "time": int(time.time() * 1000)}))

            elif action == "send_message":
                conv_id = frame.get("conversationId")
                receiver_id = frame.get("receiverId")
                content = frame.get("content", "")
                msg_id = frame.get("id") or f"m_{int(time.time() * 1000)}_{user_id[:6]}"
                msg_type = frame.get("msgType", "TEXT")
                reply_to_id = frame.get("replyToId")

                if db.is_blocked(user_id, receiver_id):
                    await websocket.send_text(json.dumps({
                        "type": "error",
                        "code": "BLOCKED",
                        "message": "Cannot send message to this user",
                    }))
                    continue

                is_online = hub.is_online(receiver_id)
                initial_status = "DELIVERED" if is_online else "SENT"

                msg = db.save_message(
                    message_id=msg_id,
                    conversation_id=conv_id,
                    sender_id=user_id,
                    receiver_id=receiver_id,
                    content=content,
                    msg_type=msg_type,
                    reply_to_id=reply_to_id,
                    status=initial_status,
                )

                # Ack back to sender
                await websocket.send_text(json.dumps({
                    "type": "message_sent_ack",
                    "messageId": msg_id,
                    "status": initial_status,
                    "createdAt": msg["created_at"],
                }))

                # Relay to receiver if online
                if is_online:
                    await hub.send_to_user(receiver_id, {
                        "type": "message",
                        "message": msg,
                    })

            elif action == "typing":
                conv_id = frame.get("conversationId")
                target_id = frame.get("targetUserId")
                is_typing = bool(frame.get("isTyping", False))
                if target_id and not db.is_blocked(user_id, target_id):
                    await hub.send_to_user(target_id, {
                        "type": "typing",
                        "conversationId": conv_id,
                        "userId": user_id,
                        "isTyping": is_typing,
                    })

            elif action == "read":
                conv_id = frame.get("conversationId")
                sender_id = frame.get("senderId")
                up_to_time = frame.get("upToTime")
                updated_ids = db.mark_read(conv_id, user_id, up_to_time)
                if updated_ids and sender_id:
                    await hub.send_to_user(sender_id, {
                        "type": "read_receipt",
                        "conversationId": conv_id,
                        "messageIds": updated_ids,
                    })

            elif action == "delivery_receipt":
                msg_id = frame.get("messageId")
                sender_id = frame.get("senderId")
                if msg_id:
                    updated = db.mark_delivered(msg_id)
                    if updated and sender_id:
                        await hub.send_to_user(sender_id, {
                            "type": "delivery_receipt",
                            "messageId": msg_id,
                            "status": "DELIVERED",
                        })

    except WebSocketDisconnect:
        await hub.disconnect(user_id, websocket)
    except Exception as e:
        log.warning("WebSocket error for user %s: %s", user_id, e)
        await hub.disconnect(user_id, websocket)
