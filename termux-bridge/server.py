#!/usr/bin/env python3
"""
AgyDroid Termux Bridge Server
Runs inside Termux and bridges HTTP requests from the Android app to the agy CLI.
Port: 7860
"""
import asyncio
import json
import os
import subprocess
import sys
import signal
import logging
from pathlib import Path
from aiohttp import web

# Configure logging to stderr only (never log secrets)
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    stream=sys.stderr
)
log = logging.getLogger(__name__)

AGY_PATH = "/data/data/com.termux/files/usr/bin/agy"
HOME_DIR = str(Path.home())
PORT = 7860
BRIDGE_VERSION = "1.0.0"


def redact_secrets(text: str) -> str:
    """Redact common secret patterns from log output."""
    import re
    patterns = [
        (r'ghp_[A-Za-z0-9]{36}', 'ghp_***REDACTED***'),
        (r'github_pat_[A-Za-z0-9_]{80,}', 'github_pat_***REDACTED***'),
        (r'"token"\s*:\s*"[^"]+"', '"token": "***REDACTED***"'),
    ]
    for pattern, replacement in patterns:
        text = re.sub(pattern, replacement, text)
    return text


async def get_status(request: web.Request) -> web.Response:
    """Check if agy is available and authenticated."""
    try:
        proc = await asyncio.create_subprocess_exec(
            AGY_PATH, "--version",
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE
        )
        stdout, _ = await asyncio.wait_for(proc.communicate(), timeout=10.0)
        version = stdout.decode().strip()
        return web.json_response({
            "status": "ok",
            "agy_version": version,
            "bridge_version": BRIDGE_VERSION,
            "agy_path": AGY_PATH
        })
    except Exception as e:
        log.error(f"Status check failed: {e}")
        return web.json_response({"status": "error", "message": str(e)}, status=500)


async def create_workspace(request: web.Request) -> web.Response:
    """Create a new project workspace directory."""
    try:
        data = await request.json()
        project_name = data.get("project_name", "").strip()
        package_name = data.get("package_name", "com.example.app")

        if not project_name or not project_name.replace("-", "").replace("_", "").isalnum():
            return web.json_response({"error": "Invalid project name"}, status=400)

        workspace = os.path.join(HOME_DIR, "AgyDroid-projects", project_name)
        os.makedirs(workspace, exist_ok=True)

        log.info(f"Created workspace: {workspace}")
        return web.json_response({
            "success": True,
            "workspace": workspace,
            "project_name": project_name
        })
    except Exception as e:
        log.error(f"Create workspace error: {e}")
        return web.json_response({"error": str(e)}, status=500)


async def chat_stream(request: web.Request) -> web.StreamResponse:
    """
    Stream agy output via Server-Sent Events (SSE).
    Accepts JSON: { "prompt": "...", "workspace": "/path/to/workspace", "conversation_id": "..." }
    Streams NDJSON events back.
    """
    try:
        data = await request.json()
        prompt = data.get("prompt", "").strip()
        workspace = data.get("workspace", HOME_DIR)
        conversation_id = data.get("conversation_id")
        model = data.get("model", "gemini-3.8-flash-medium")

        if not prompt:
            return web.json_response({"error": "prompt is required"}, status=400)

        if not os.path.isdir(workspace):
            os.makedirs(workspace, exist_ok=True)

        # Build agy command
        cmd = [
            AGY_PATH,
            "--output-format", "stream-json",
            "--mode", "accept-edits",
            "--dangerously-skip-permissions",
            "--add-dir", workspace,
            "--model", model,
        ]

        if conversation_id:
            cmd += ["--conversation", conversation_id]

        cmd.append(f"--print={prompt}")

        log.info(f"Running agy in workspace: {workspace}")

        # Set up SSE response
        response = web.StreamResponse(
            status=200,
            headers={
                "Content-Type": "text/event-stream",
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
                "Access-Control-Allow-Origin": "*",
            }
        )
        await response.prepare(request)

        # Set AGY_AUTO_UPDATE=0 so non-interactive execution never blocks on update prompts
        sub_env = dict(os.environ)
        sub_env["AGY_AUTO_UPDATE"] = "0"

        proc = await asyncio.create_subprocess_exec(
            *cmd,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
            cwd=workspace,
            env=sub_env
        )

        async def send_event(event_type: str, payload: dict):
            payload["event"] = event_type
            line = json.dumps(payload) + "\n"
            await response.write(line.encode("utf-8"))

        await send_event("start", {"message": "AGY session started"})

        # Stream stdout line by line
        try:
            while True:
                line = await asyncio.wait_for(
                    proc.stdout.readline(), timeout=300.0
                )
                if not line:
                    break
                decoded = line.decode("utf-8").strip()
                if decoded:
                    try:
                        parsed = json.loads(decoded)
                        await send_event("data", parsed)
                    except json.JSONDecodeError:
                        await send_event("text", {"content": decoded})

        except asyncio.TimeoutError:
            log.warning("AGY process timeout")
            await send_event("error", {"message": "Timeout waiting for agy response"})
            proc.kill()

        await proc.wait()

        # Read any stderr
        stderr_data = await proc.stderr.read()
        if stderr_data:
            stderr_text = redact_secrets(stderr_data.decode("utf-8", errors="replace"))
            log.error(f"agy stderr: {stderr_text}")

        exit_code = proc.returncode
        await send_event("done", {"exit_code": exit_code, "success": exit_code == 0})

        return response

    except Exception as e:
        log.error(f"Chat stream error: {e}")
        try:
            err_payload = json.dumps({"event": "error", "message": str(e)}) + "\n"
            await response.write(err_payload.encode("utf-8"))
            return response
        except Exception:
            return web.json_response({"error": str(e)}, status=500)


async def list_files(request: web.Request) -> web.Response:
    """List files in a workspace directory."""
    try:
        workspace = request.query.get("workspace", HOME_DIR)
        if not os.path.isdir(workspace):
            return web.json_response({"error": "Directory not found"}, status=404)

        def build_tree(path: str, max_depth: int = 4, current_depth: int = 0) -> list:
            if current_depth >= max_depth:
                return []
            result = []
            try:
                entries = sorted(os.scandir(path), key=lambda e: (e.is_file(), e.name))
                for entry in entries:
                    if entry.name.startswith(".") and entry.name not in [".github"]:
                        continue
                    if entry.name in ["build", ".gradle", "__pycache__", "node_modules"]:
                        continue
                    item = {
                        "name": entry.name,
                        "path": entry.path,
                        "is_dir": entry.is_dir(),
                        "size": entry.stat().st_size if entry.is_file() else 0,
                    }
                    if entry.is_dir():
                        item["children"] = build_tree(
                            entry.path, max_depth, current_depth + 1
                        )
                    result.append(item)
            except PermissionError:
                pass
            return result

        tree = build_tree(workspace)
        return web.json_response({"workspace": workspace, "tree": tree})

    except Exception as e:
        log.error(f"List files error: {e}")
        return web.json_response({"error": str(e)}, status=500)


async def read_file(request: web.Request) -> web.Response:
    """Read the content of a file."""
    try:
        file_path = request.query.get("path", "")
        if not file_path or not os.path.isfile(file_path):
            return web.json_response({"error": "File not found"}, status=404)

        # Security: only allow reading within home directory
        home = str(Path.home())
        if not os.path.abspath(file_path).startswith(home):
            return web.json_response({"error": "Access denied"}, status=403)

        with open(file_path, "r", encoding="utf-8", errors="replace") as f:
            content = f.read()

        return web.json_response({
            "path": file_path,
            "name": os.path.basename(file_path),
            "content": content,
            "size": len(content)
        })
    except Exception as e:
        log.error(f"Read file error: {e}")
        return web.json_response({"error": str(e)}, status=500)


def create_app() -> web.Application:
    app = web.Application()
    app.router.add_get("/status", get_status)
    app.router.add_post("/workspace/create", create_workspace)
    app.router.add_post("/chat", chat_stream)
    app.router.add_get("/files", list_files)
    app.router.add_get("/files/read", read_file)
    return app


def handle_sigterm(*args):
    log.info("Received SIGTERM, shutting down...")
    sys.exit(0)


if __name__ == "__main__":
    signal.signal(signal.SIGTERM, handle_sigterm)
    log.info(f"AgyDroid Bridge Server v{BRIDGE_VERSION} starting on port {PORT}")
    log.info(f"AGY path: {AGY_PATH}")

    app = create_app()
    web.run_app(app, host="127.0.0.1", port=PORT, access_log=None)
