#!/bin/bash
# Starts the GTNH chat agent in the background. Log: ~/gtnh-bot/logs/chat-agent.log
PID=~/gtnh-bot/logs/chat-agent.pid
if [ -f "$PID" ] && kill -0 "$(cat "$PID")" 2>/dev/null; then echo "already running (pid $(cat "$PID"))"; exit 0; fi
nohup python3 ~/gtnh-bot/scripts/chat-agent.py >> ~/gtnh-bot/logs/chat-agent.stdout.log 2>&1 &
echo $! > "$PID"
echo "chat agent started (pid $!), log: ~/gtnh-bot/logs/chat-agent.log"
