#!/usr/bin/env bash
set -e

FILE="app/src/main/java/com/aezora/music/service/PlayerController.kt"

cp "$FILE" "$FILE.bak"

python3 <<'PY'
from pathlib import Path

p = Path("app/src/main/java/com/aezora/music/service/PlayerController.kt")
text = p.read_text()

old = """).build()"""

new = """)
            .setAudioSink(
                DefaultAudioSink.Builder()
                    .setAudioProcessors(arrayOf(sonicProcessor))
                    .build()
            )
            .build()"""

count = text.count(old)
if count != 1:
    print(f"Ожидался 1 фрагмент ').build()', найдено {count}. Ничего не изменено.")
    raise SystemExit(1)

text = text.replace(old, new, 1)
p.write_text(text)
print("Готово.")
PY

echo
echo "Проверь:"
nl -ba "$FILE" | sed -n '38,52p'
