#!/bin/zsh
# Start the Ollama MCP client connected to headlessmc-mcp.
# First launch GTNH from Prism (or run launch-gtnh.sh) as Buravchik_backup,
# wait until the main menu or world is up, then run this and give the bot a task.
set -e
export PATH="$HOME/.local/bin:/usr/local/bin:$PATH"
BOT="$HOME/gtnh-bot"
MODEL="${1:-qwen3:32b}"

# Make sure Ollama is running
if ! curl -sf http://127.0.0.1:11434/api/version >/dev/null; then
  open -a Ollama
  for i in {1..30}; do
    curl -sf http://127.0.0.1:11434/api/version >/dev/null && break
    sleep 1
  done
fi

echo "Ollama: $(curl -s http://127.0.0.1:11434/api/version)"
echo "Model:  $MODEL"
echo "MCP:    $BOT/bin/headlessmc-mcp"
echo
echo "In the chat, first ask the bot to list instances / get state."
echo "If the game is running in Prism with the two HMC mods, it should show up as an attached instance."
echo
exec ollmcp -m "$MODEL" -j "$BOT/config/mcp-servers.json"
