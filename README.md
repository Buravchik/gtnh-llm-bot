# GTNH LLM Bot

Бот-игрок для **GregTech: New Horizons 2.9** (Minecraft 1.7.10 Forge), которым управляет локальная LLM через [Ollama](https://ollama.com).
A Minecraft 1.7.10 / GTNH player bot driven by a local LLM (Ollama) over MCP.

## Что умеет
- Ходит к координатам сам: ищет путь, прыгает на ступеньки, обходит деревья и стены, спускается под землю (не больше 3 блоков), плавает, лезет по лестницам, открывает деревянные двери и калитки. Не ломает блоки, не телепортируется, не использует читы.
- Осматривается: карта блоков сверху и в разрезе, поиск входов, дверей, сундуков и т.п., игроки и мобы рядом.
- Видит путевые точки JourneyMap (1.7.10-6.0.8) и игроков в зоне прорисовки, ходит к ним.
- Читает чат, отвечает, когда к нему обращаются по нику или «бот», и выполняет просьбы владельца.

## Как устроено
- [HeadlessMC](https://github.com/headlesshq/headlessmc) + [HMC-Specifics](https://github.com/headlesshq/hmc-specifics) + [headlessmc-mcp](https://github.com/headlesshq/headlessmc-mcp): мост, через который MCP-сервер управляет запущенным из Prism Launcher клиентом.
- `mod/hmc-nav`: клиентский Forge-мод с командами навигации и осмотра для HMC-Specifics.
- `scripts/chat-agent.py`: демон, который следит за чатом и гоняет цикл вызова инструментов в Ollama (`qwen3:32b`).
- `scripts/start-bot.sh`: интерактивный режим через [ollmcp](https://github.com/jonigl/mcp-client-for-ollama).

## Команды мода (через `game_command`)
| Команда | Что делает |
|---|---|
| `pos` | позиция, взгляд, здоровье, еда, состояние ходьбы |
| `goto X Y Z` / `goto X Z` [`--sprint`], `goto status`, `goto stop` | дойти до точки |
| `scan [R] [--level Y]`, `scan --slice N\|E\|... [--depth D]` | карта сверху или в разрезе |
| `find openings [R] --y Y`, `find <блок> [R]` | поиск входов и блоков |
| `map players`, `map waypoints [фильтр]` | игроки рядом с координатами, путевые точки JourneyMap |
| `goto player <ник>`, `goto waypoint <имя>` | дойти до игрока или точки |
| `follow <ник> [--seconds N] [--distance D]`, `follow stop`, `follow status` | постоянно идти за игроком (держит ~2 блока) |
| `jump [--seconds N]`, `jump --while-following`, `jump stop` | прыгать (можно вместе с follow) |
| `halt` | остановить goto/follow/jump (`stop` в HMC — выход из игры!) |
| `join <сервер>` | зайти на сервер из списка |
| `look <yaw> [pitch]`, `look north\|...`, `lookat X Y Z` | повернуть камеру |

## Установка (кратко)
1. Положить в `mods/` инстанса GTNH 2.9 в Prism: `hmc-specifics-1.7.10-forge`, `hmc-mcp-bridge` (из headlessmc-mcp) и собранный `hmc-nav` (`cd mod/hmc-nav && ./build.sh`).
2. Установить Ollama и `ollama pull qwen3:32b`.
3. Положить `headlessmc-mcp` в `~/gtnh-bot/bin/`, скопировать `config/mcp-servers.example.json`.
4. Запустить игру из Prism, зайти на сервер, затем `scripts/chat-agent-start.sh`.

Подробности в `docs-README-install.txt`. Настройки чат-агента (владельцы, имена, лимиты) в `config/chat-agent.json`.

## Лицензия
MIT
