# 🎵 Aezora — Музыка без границ

Полноценный Android музыкальный плеер с поддержкой SoundCloud, Яндекс Музыки и локальных файлов.

---

## 📋 Функциональность

### Источники музыки
- **SoundCloud** — поиск, тренды, стриминг через yt-dlp. DRM-треки автоматически фильтруются.
- **Яндекс Музыка** — поиск, "Моя волна", плейлисты, любимые треки (требует OAuth-токен).
- **Локальные файлы** — скачанные треки из кэша приложения.

### Плеер
- Полноэкранный плеер в стиле Яндекс Музыки / Spotify
- Анимированная обложка (vinyl-эффект при воспроизведении)
- Перемотка, следующий/предыдущий, shuffle, repeat (один трек / все)
- Очередь воспроизведения с просмотром

### Эффекты
| Режим | Изменение |
|-------|-----------|
| Normal | Без изменений |
| Speed UP | +2 тона выше |
| Ultra Speed UP | +4 тона выше |
| Slowed | −2 тона ниже |
| Ultra Slowed | −4 тона ниже |

- **Запоминание режима** — опционально применять pitch к каждому треку
- **Эквалайзер** — пресеты: Flat, Bass Boost, Treble Boost, Vocal

### Библиотека
- Любимые треки (❤️)
- Пользовательские плейлисты — создание, редактирование, удаление
- Скачанные треки

### Скачивание
- Форматы: FLAC (lossless) / MP3
- Скачивание отдельных треков и целых плейлистов
- Фоновая загрузка через WorkManager
- Сохранение в папку `Music/Aezora/`

### Темы
- 🔵 Сине-фиолетовый (по умолчанию)
- 🟢 Жёлто-зелёный
- ⚪ Чёрно-белый

---

## 🔧 Сборка проекта

### Требования
- Android Studio Hedgehog или новее
- JDK 17
- Android SDK 34

### Шаги
```bash
git clone <repo>
cd Aezora
./gradlew assembleDebug
```

APK будет в `app/build/outputs/apk/debug/`.

---

## 🔑 Яндекс Музыка — Получение токена

1. Откройте браузер и перейдите по ссылке:
   ```
   https://oauth.yandex.ru/authorize?response_type=token&client_id=23cabbbdc6cd418abb4b39c32c41195d
   ```
2. Войдите в аккаунт Яндекс
3. Разрешите доступ
4. Скопируйте `access_token` из URL-адреса
5. Вставьте в Настройки → Яндекс Музыка → Токен авторизации

> ⚠️ Токен хранится только локально на устройстве.

---

## 🏗️ Архитектура

```
app/
├── data/
│   ├── local/          # Room DB (треки, плейлисты) + DataStore (настройки)
│   └── remote/
│       ├── soundcloud/ # SoundCloud API + yt-dlp
│       └── yandex/     # Яндекс Музыка неофициальный API
├── domain/
│   ├── model/          # Доменные модели (Track, Playlist, ...)
│   └── repository/     # MusicRepository
├── service/
│   ├── MusicService.kt    # Media3 MediaSessionService
│   ├── PlayerController.kt # ExoPlayer + SonicAudioProcessor (pitch)
│   └── DownloadWorker.kt  # WorkManager фоновое скачивание
└── ui/
    ├── home/           # Главный экран + Поиск
    ├── library/        # Библиотека
    ├── player/         # Полноэкранный плеер + Speed/EQ sheets
    ├── settings/       # Настройки
    ├── theme/          # Темы (3 варианта)
    ├── MainActivity.kt # Navigation + MiniPlayer
    └── MainViewModel.kt
```

**Паттерны:** Clean Architecture + MVVM + Hilt DI + Kotlin Coroutines/Flow

---

## 💙 Поддержать автора

[Отправить донат через CryptoBot](http://t.me/send?start=IVzCeY4eliGd)

---

## ⚖️ Лицензия и ограничения

- SoundCloud: только треки без DRM (policy != SNIP/BLOCK)
- Яндекс Музыка: неофициальный API, используйте на свой риск
- Только для личного использования
