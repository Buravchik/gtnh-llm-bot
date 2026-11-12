#!/bin/zsh
# Optional. Only needed if you want HeadlessMC itself to launch a client
# (not required for the recommended Prism attach workflow).
# This prints a Microsoft device-code link; open it in a browser and sign in
# as Buravchik_backup. Do not paste tokens anywhere.
set -e
echo "A Microsoft login link will appear. Open it in the browser, sign in as Buravchik_backup, then come back here."
exec "$HOME/gtnh-bot/bin/headlessmc" account login
