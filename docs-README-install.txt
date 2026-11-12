GTNH 2.9 LLM-бот (HeadlessMC + Ollama)
=====================================

Рекомендуемый способ: запустить сборку из Prism под Buravchik_backup.
Мод hmc-mcp-bridge сам регистрирует игру, MCP-сервер к ней подключается.
HeadlessMC сам игру не запускает: у GTNH 2.9 кастомный lwjgl3ify/Forge 1.7.10,
штатный `headlessmc launch forge 1.7.10` эту сборку не поднимет.

Что установлено
---------------
~/gtnh-bot/bin/headlessmc          HeadlessMC 3.0.0-RC8 (macos-arm64)
~/gtnh-bot/bin/headlessmc-mcp      headlessmc-mcp 0.1.0 (macos-arm64)
~/gtnh-bot/jars/                   копии jar (launcher, mcp, specifics, bridge)
~/gtnh-bot/scripts/launch-gtnh.sh  запуск Prism-инстанса под Buravchik_backup
~/gtnh-bot/scripts/start-bot.sh    запуск ollmcp + headlessmc-mcp
~/gtnh-bot/scripts/login-headlessmc.sh  вход Microsoft в HeadlessMC (не обязателен)
~/gtnh-bot/config/mcp-servers.json
~/.local/bin/ollmcp                MCP-клиент для Ollama

Моды, добавленные в сборку GT_New_Horizons_2.9.0_Java_17-26
(.minecraft/mods), только эти три файла:
  hmc-specifics-1.7.10-forge-latest.jar   (HMC-Specifics 3.0.1)
  hmc-mcp-bridge-0.1.0.jar
  hmc-nav-0.1.0.jar                       (навигация, собран здесь)
Чтобы убрать бота из сборки, удалите эти jar. Другие файлы инстанса не менялись.

Ollama
------
Приложение обновлено с 0.1.48 до 0.40.16 (tool calling).
Старая копия: ~/gtnh-bot/backup/Ollama-0.1.48.app
Модели llama2 и codellama на месте.

Как пользоваться
----------------
1. Запустите сборку (из Prism или):
     ~/gtnh-bot/scripts/launch-gtnh.sh
   Должен быть выбран аккаунт Buravchik_backup.
2. Дождитесь меню или входа в мир (окно игры должно быть видно).
3. В Терминале:
     ~/gtnh-bot/scripts/start-bot.sh
   (или: ~/gtnh-bot/scripts/start-bot.sh qwen3:14b)
4. Напишите задачу, например: «посмотри, какие инстансы видны, зайди в мир
   и иди вперёд» или «напиши в чат привет».

Вход Microsoft в HeadlessMC не нужен для схемы с Prism.
Если всё же понадобится:
     ~/gtnh-bot/scripts/login-headlessmc.sh
откроется ссылка https://www.microsoft.com/link — войти как Buravchik_backup.

Конфиг HeadlessMC: ~/Library/Application Support/headlessmc/config.properties
Каталог данных Minecraft для самого HeadlessMC (не Prism): ~/gtnh-bot/minecraft

Навигация (мод hmc-nav, исходники ~/gtnh-bot/src/hmc-nav, сборка ./build.sh)
---------------------------------------------------------------------------
Команды выполняются через game_command (боту это прописано в system prompt ollmcp),
вручную: ~/gtnh-bot/scripts/mcpcall.py game_command '{"instance":"1","command":"pos"}'
  pos                         позиция, здоровье, еда, состояние goto
  goto X Y Z | goto X Z       идти к точке (A*-поиск пути, прыжки на 1 блок, обход деревьев/стен,
       [--sprint]             лава/огонь/обрывы >3 обходятся, деревянные двери/калитки открывает,
                              лестницы (ladder), плавание; блоки НЕ ломает, не телепортирует)
  goto status | goto stop     опрос / остановка; итог: ARRIVED, STUCK (с причиной), FAILED
  scan [R] [--level Y]        карта сверху и сводка по 8 направлениям, блоки и мобы рядом
  scan [L] --slice N|E|S|W|NE|.. [--depth D]   вид сбоку (разрез) — пещеры/залы под тобой
  find openings [R] --y Y [--at X Y Z]         входы (пещеры, тоннели, проёмы) на высоте Y
  find <имя> [R]              блоки по имени: door, ladder, chest, trapdoor...
  look <yaw> [pitch] | look north|south|east|west|up|down,  lookat X Y Z
Лог ходьбы пишется в лог игры строками "[hmc-nav] ...".

Чат-агент (бот сам читает чат и отвечает)
-----------------------------------------
Запуск:   ~/gtnh-bot/scripts/chat-agent-start.sh
Стоп:     ~/gtnh-bot/scripts/chat-agent-stop.sh
Тест:     python3 ~/gtnh-bot/scripts/chat-agent.py --test   (подставляет фейковую строку от хозяина и пишет ответ в чат)
Лог:      ~/gtnh-bot/logs/chat-agent.log
Настройки: ~/gtnh-bot/config/chat-agent.json (модель, хозяева, имена-обращения, лимиты)
Отвечает, когда в чате упомянуты Buravchik_backup / буравчик / бот / ботик / bot или ему пишут /msg;
хозяину (Boorawchick) ещё 90 с после ответа можно писать без обращения.
Хозяин может давать задания (goto, найти вход, осмотреться); остальным — только справки (pos/scan/find), без действий.
Читает чат из logs/latest.log игры, действует через headlessmc-mcp + Ollama (qwen3:32b).
