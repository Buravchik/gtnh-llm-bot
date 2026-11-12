#!/bin/zsh
# Launch the existing Prism GTNH 2.9 instance windowed, as Buravchik_backup.
# HeadlessMC attaches automatically via hmc-mcp-bridge + HMC-Specifics in the mods folder.
set -e
PRISM="/Applications/Prism Launcher.app/Contents/MacOS/prismlauncher"
INSTANCE="GT_New_Horizons_2.9.0_Java_17-26"
ACCOUNT="Buravchik_backup"
exec "$PRISM" --launch "$INSTANCE" --profile "$ACCOUNT" --show-window
