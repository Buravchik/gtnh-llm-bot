#!/bin/bash
PID=~/gtnh-bot/logs/chat-agent.pid
if [ -f "$PID" ] && kill -0 "$(cat "$PID")" 2>/dev/null; then
  P=$(cat "$PID"); pkill -P "$P" 2>/dev/null; kill "$P"; echo "chat agent stopped"
else echo "not running"; fi
rm -f "$PID"
