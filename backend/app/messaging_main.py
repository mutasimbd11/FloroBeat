"""Standalone FloroBeat Real-Time Messaging Server.

Designed for deployment at chat.shongho.com (or free cloud container/VPS).
Independent of the Listen Together party server, maintaining separate state and ports.
"""

from __future__ import annotations

import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .messaging import router as messaging_router

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")
log = logging.getLogger("florobeat-chat")

app = FastAPI(
    title="FloroBeat Messenger",
    version="1.0.0",
    description="Real-time messaging, presence, and user discovery service for FloroBeat.",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(messaging_router)


@app.get("/")
async def root():
    return {
        "service": "florobeat-messenger",
        "version": "1.0.0",
        "status": "online",
        "domain": "chat.shongho.com",
    }


@app.get("/healthz")
async def healthz():
    return {"ok": True, "service": "florobeat-messenger"}
