# FloroBeat Real-Time Messenger Documentation

## 1. Overview & Architecture

FloroBeat Messenger is a real-time, privacy-first, free-to-operate communication system natively integrated into the FloroBeat Android application (`com.florosoft.florobeat`) and backed by a lightweight, asynchronous Python/FastAPI backend (`backend/app/messaging.py`).

The architecture ensures that music lovers on FloroBeat can discover fellow listeners, check live online/offline presence, exchange instant messages in real time, view delivery and read receipts, track typing status, and receive push notifications when backgrounded or offline—all with **zero third-party paid chat SDKs, zero paid messaging SaaS, and zero paid Firebase plans**.

### High-Level Topology

```
┌─────────────────────────────────────────────────────────────┐
│                 FloroBeat Android Client                    │
│                                                             │
│  - ConversationsScreen (Discovery, List, Status)            │
│  - ChatScreen (Bubbles, Quoted Replies, Ticks, Composer)    │
│  - UserProfileDialog & ReportDialog                         │
│  - MessengerRepository (Optimistic StateFlows & Sync)       │
│  - MessengerDatabase (Native Android SQLite, WAL mode)      │
│  - MessengerClient (OkHttp WebSocket, Exponential Backoff)  │
└──────────────────────────▲───────▲──────────────────────────┘
                           │       │
       REST HTTP/2 (HTTPS) │       │ WSS Real-Time WebSocket
                           ▼       ▼
┌─────────────────────────────────────────────────────────────┐
│          FloroBeat Messaging Service (FastAPI)              │
│                 Domain: chat.shongho.com                    │
│                                                             │
│  - MessagingHub: In-memory live socket router & presence    │
│  - REST Endpoints: Sync, pagination, search, reports        │
│  - Rate Limiter & Security Validator                        │
│  - SQLite Database (WAL mode, multi-threaded safe)          │
│  - FCM Push Dispatcher (Free-tier Firebase Cloud Messaging) │
└──────────────────────────────────┬──────────────────────────┘
                                   │ FCM HTTP v1 / Legacy
                                   ▼
┌─────────────────────────────────────────────────────────────┐
│                  Google FCM Gateway (Free)                  │
│                                                             │
│  - Wakes backgrounded / offline FloroBeat clients           │
│  - Triggers deep-link route directly into target chat       │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Real-Time WebSocket Protocol

### 2.1 Connection URL & Authentication

- **Endpoint:** `wss://chat.shongho.com/api/messaging/ws/{user_id}`
- **Heartbeat:** Ping frames every 25 seconds with immediate Pong responses.
- **Auto-Reconnect:** Android client features exponential backoff reconnecting with randomized jitter (1s initial, up to 20s ceiling).

### 2.2 Protocol Frames

All WebSocket messages are encoded in UTF-8 JSON. Every event has a `type` string.

#### Inbound Message (`msg`)
Transmitted to recipient when a message is sent:
```json
{
  "type": "msg",
  "id": "m_1728038400000_a1b2c3",
  "conversation_id": "c_userA_userB",
  "sender_id": "userA",
  "receiver_id": "userB",
  "content": "Listening to this new release!",
  "msg_type": "TEXT",
  "status": "SENT",
  "created_at": 1728038400000,
  "reply_to_id": null
}
```

#### Message Acknowledgment (`msg_ack`)
Sent back to the sender as soon as the server authoritatively stores the message:
```json
{
  "type": "msg_ack",
  "message_id": "m_1728038400000_a1b2c3",
  "status": "SENT",
  "created_at": 1728038400120
}
```

#### Delivery Receipt (`delivery_receipt`)
Sent to sender when recipient's device receives the message while app is open:
```json
{
  "type": "delivery_receipt",
  "message_id": "m_1728038400000_a1b2c3",
  "status": "DELIVERED"
}
```

#### Read Receipt (`read_receipt`)
Sent when recipient opens the chat and reads the message:
```json
{
  "type": "read_receipt",
  "conversation_id": "c_userA_userB",
  "reader_id": "userB",
  "read_up_to": 1728038400000
}
```

#### Typing Status (`typing`)
Ephemeral, debounced typing event (not persisted to disk):
```json
{
  "type": "typing",
  "conversation_id": "c_userA_userB",
  "user_id": "userA",
  "is_typing": true
}
```

#### Presence Event (`presence`)
Broadcast when a user connects or disconnects:
```json
{
  "type": "presence",
  "user_id": "userA",
  "is_online": true,
  "last_seen_at": 1728038450000
}
```

#### Message Deleted (`msg_deleted`)
Broadcast when a message is revoked for everyone:
```json
{
  "type": "msg_deleted",
  "message_id": "m_1728038400000_a1b2c3",
  "conversation_id": "c_userA_userB",
  "for_everyone": true
}
```

---

## 3. REST API Specification

| Method | Path | Headers | Description |
|---|---|---|---|
| `POST` | `/api/messaging/profile` | `Content-Type: application/json` | Upserts local profile (display name, username, bio, FCM token) |
| `GET` | `/api/messaging/users/search?q={query}` | `X-User-Id: {id}` | Safe user discovery by name or `@username` handle |
| `GET` | `/api/messaging/conversations` | `X-User-Id: {id}` | Returns active conversations with latest messages and unread counts |
| `GET` | `/api/messaging/conversations/{id}/messages` | `X-User-Id: {id}` | Paginated message history (default limit: 50, `before` timestamp support) |
| `POST` | `/api/messaging/conversations/{id}/messages` | `X-User-Id: {id}` | REST fallback for sending messages when WebSocket is reconnecting |
| `POST` | `/api/messaging/conversations/{id}/read` | `X-User-Id: {id}` | Marks conversation messages as read |
| `DELETE` | `/api/messaging/messages/{id}?for_everyone={bool}` | `X-User-Id: {id}` | Deletes message (enforces author validation for `for_everyone`) |
| `POST` | `/api/messaging/users/{id}/block` | `X-User-Id: {id}` | Blocks target user from sending messages |
| `DELETE` | `/api/messaging/users/{id}/block` | `X-User-Id: {id}` | Unblocks target user |
| `POST` | `/api/messaging/users/{id}/report` | `X-User-Id: {id}` | Reports user for spam, harassment, inappropriate content, etc. |

---

## 4. Database Schema

Both backend and Android clients use SQLite with Write-Ahead Logging (WAL) enabled:

### Backend Schema (`messenger.db`)
1. **`users`**:
   - `user_id TEXT PRIMARY KEY`
   - `username TEXT UNIQUE NOT NULL`
   - `display_name TEXT NOT NULL`
   - `avatar_url TEXT`
   - `bio TEXT`
   - `fcm_token TEXT`
   - `last_seen_at INTEGER NOT NULL`
   - `created_at INTEGER NOT NULL`
2. **`conversations`**:
   - `id TEXT PRIMARY KEY`
   - `user_a TEXT NOT NULL`
   - `user_b TEXT NOT NULL`
   - `created_at INTEGER NOT NULL`
   - `updated_at INTEGER NOT NULL`
3. **`messages`**:
   - `id TEXT PRIMARY KEY`
   - `conversation_id TEXT NOT NULL`
   - `sender_id TEXT NOT NULL`
   - `receiver_id TEXT NOT NULL`
   - `content TEXT NOT NULL`
   - `type TEXT NOT NULL DEFAULT 'TEXT'`
   - `status TEXT NOT NULL DEFAULT 'SENT'`
   - `reply_to_id TEXT`
   - `created_at INTEGER NOT NULL`
   - `deleted_for_all INTEGER NOT NULL DEFAULT 0`
4. **`blocks`**:
   - `blocker_id TEXT NOT NULL`
   - `blocked_id TEXT NOT NULL`
   - `created_at INTEGER NOT NULL`
   - `PRIMARY KEY (blocker_id, blocked_id)`
5. **`reports`**:
   - `id TEXT PRIMARY KEY`
   - `reporter_id TEXT NOT NULL`
   - `reported_id TEXT NOT NULL`
   - `reason TEXT NOT NULL`
   - `details TEXT`
   - `created_at INTEGER NOT NULL`

Indexes are maintained on `(conversation_id, created_at DESC)`, `(user_a, user_b)`, `(sender_id)`, and `(receiver_id)`.

---

## 5. Message Lifecycle

1. **`SENDING`**:
   - Created optimistically on device with client timestamp and generated UUID.
   - Rendered with hourglass status icon.
2. **`SENT`**:
   - Server receives message, verifies authorization and non-blocked status, writes to SQLite, and sends `msg_ack`.
   - Single checkmark displayed in chat bubble.
3. **`DELIVERED`**:
   - Recipient client receives message frame and transmits `delivery_receipt`.
   - Double grey checkmarks displayed in chat bubble.
4. **`READ`**:
   - Recipient enters conversation screen; client sends `read_receipt`.
   - Double mint checkmarks displayed in chat bubble.
5. **`FAILED`**:
   - Network timeout or client error causes failure state.
   - Red error indicator with instant tap-to-retry button.

---

## 6. Offline Support & Sync

- Every message received or sent is persisted immediately into Android's native `MessengerDatabase.kt` (`SQLiteOpenHelper`).
- The conversation list and chat histories are available instantly without network connectivity.
- When network reconnects, `MessengerRepository` queries remote messages via REST pagination and catches up on any missed frames.

---

## 7. Push Notifications & Privacy

- When recipient is offline (`isOnline == false`), the backend queries the recipient's `fcm_token` from the `users` table and dispatches an FCM message.
- Payload includes `type: "chat"`, `route: "chat"`, `target_id: conversation_id`, `entity_id: sender_id`.
- Tapping the notification deep-links directly into `ChatScreen` with the sender's user context.
- Blocked users cannot trigger push notifications: the server rejects dispatch if `(receiver_id, sender_id)` exists in the `blocks` table.

---

## 8. Deployment & Free Infrastructure Requirements

### 8.1 Domain Separation
- **Listen Together:** `party.shongho.com` (Python/FastAPI or Node room sync)
- **FloroBeat Messenger:** `chat.shongho.com` (Dedicated messaging service)

### 8.2 Why Traditional Shared cPanel Hosting Cannot Host Persistent WebSockets
1. **Connection Lifecycle & Timeout:**
   - Shared cPanel environments run Apache with `mod_php`, `lsapi`, or `CGI`. These web servers enforce request timeouts (typically 30–60 seconds).
   - WebSocket connections are long-lived TCP streams (lasting minutes or hours) which Apache kills immediately upon hitting the execution timeout.
2. **Process Model:**
   - Shared hosting does not permit standing ASGI processes (e.g. `uvicorn`, `daphne`, `hypercorn`) listening on persistent ports.
   - The real-time hub requires maintaining concurrent open sockets in memory across all users to broadcast events instantaneously.

### 8.3 Recommended Free-Tier / Open-Source Deployment

1. **Option A: Free VPS / Cloud VM (Recommended)**
   - Oracle Cloud Always Free Tier (4 OCPU ARM Ampere, 24 GB RAM, 200 GB Storage free forever) or a low-cost VPS.
   - Run via Docker or Systemd service with Caddy / Nginx reverse proxy providing automated Let's Encrypt SSL at `chat.shongho.com`.
2. **Option B: Free-Tier Managed Container (e.g., Render, Railway, Fly.io)**
   - Deploy `backend/app/messaging_main.py` directly using the provided `requirements.txt`.
   - Setup custom domain `chat.shongho.com` with CNAME.
3. **Option C: cPanel with CloudLinux "Setup Python App" (ASGI/Passenger)**
   - Only possible if the cPanel hosting provider supports CloudLinux Python WSGI/ASGI via Phusion Passenger with WebSocket proxying enabled.

### 8.4 Running Standalone Messaging Server Locally or in Production

```bash
cd backend
python -m venv venv
source venv/bin/activate  # On Windows: .\venv\Scripts\Activate.ps1
pip install -r requirements.txt

# Run standalone messaging server on port 8001
python app/messaging_main.py
```
