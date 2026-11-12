#!/usr/bin/env python3
"""Talk newline-delimited JSON-RPC/stdio to headlessmc-mcp and print the tool list. Does not launch the game."""
import json
import os
import select
import subprocess
import sys
import time

MCP = os.path.expanduser("~/gtnh-bot/bin/headlessmc-mcp")

def read_message(proc, timeout=30):
    buf = b""
    end = time.time() + timeout
    while time.time() < end:
        remaining = end - time.time()
        r, _, _ = select.select([proc.stdout], [], [], max(0.05, remaining))
        if not r:
            if proc.poll() is not None:
                err = proc.stderr.read() if proc.stderr else b""
                raise RuntimeError(f"MCP server exited {proc.returncode}: {err[-2000:]}")
            continue
        chunk = os.read(proc.stdout.fileno(), 65536)
        if not chunk:
            if proc.poll() is not None:
                err = proc.stderr.read() if proc.stderr else b""
                raise RuntimeError(f"MCP server closed stdout, exit {proc.returncode}: {err[-2000:]}")
            continue
        buf += chunk
        if b"\n" in buf:
            line, _, rest = buf.partition(b"\n")
            return json.loads(line.decode("utf-8"))
    raise TimeoutError(f"Timed out waiting for MCP message, buf={buf[:400]!r}")

def send(proc, obj):
    proc.stdin.write((json.dumps(obj, ensure_ascii=False) + "\n").encode("utf-8"))
    proc.stdin.flush()

def main():
    log_path = os.path.expanduser("~/gtnh-bot/logs/mcp-server.stderr.log")
    logf = open(log_path, "wb")
    proc = subprocess.Popen(
        [MCP],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=logf,
        bufsize=0,
    )
    try:
        send(proc, {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": "2025-03-26",
                "capabilities": {},
                "clientInfo": {"name": "gtnh-bot-setup", "version": "1.0"},
            },
        })
        init = read_message(proc, timeout=30)
        print("initialize response:")
        print(json.dumps(init, ensure_ascii=False, indent=2)[:2000])
        if "error" in init:
            return 1
        caps = init.get("result", {})
        server = caps.get("serverInfo", {})
        print(f"server: {server.get('name')} {server.get('version')}")
        print(f"protocol: {caps.get('protocolVersion')}")
        send(proc, {"jsonrpc": "2.0", "method": "notifications/initialized"})
        send(proc, {"jsonrpc": "2.0", "id": 2, "method": "tools/list"})
        tools_msg = read_message(proc, timeout=30)
        if "error" in tools_msg:
            print("tools/list error:", json.dumps(tools_msg["error"], ensure_ascii=False, indent=2))
            return 1
        tools = tools_msg.get("result", {}).get("tools", [])
        print(f"tools ({len(tools)}):")
        for t in tools:
            name = t.get("name")
            desc = (t.get("description") or "").splitlines()[0][:200]
            print(f"  - {name}: {desc}")
        return 0
    finally:
        try:
            proc.stdin.close()
        except Exception:
            pass
        proc.terminate()
        try:
            proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            proc.kill()
        logf.close()

if __name__ == "__main__":
    sys.exit(main())
