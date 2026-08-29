# Данные для формы публикации на Modrinth

Открой https://modrinth.com/dashboard → Create a project и перенеси эти поля.

## Вкладка Project information

| Поле | Значение |
|---|---|
| Project name | HolySwap |
| Project slug | holyswap |
| Project summary (одно предложение, до 256 символов) | One-key offhand swap for custom server items (talismans, spheres, totems): per-category binds, cycle swap, in-game rebind screen. Client-side, not a cheat. |
| Description | скопировать целиком из body.md |
| Project icon | icon_512.png (512×512, готова) |
| Categories (Client side) | utility, equipment/items? — выбрать **utility** и **management** если есть |
| Environment | **Client** (server: не требуется) |
| License | **MIT** |
| External sources | добавить ссылку на GitHub, когда зальёшь исходники (Modrinth любит опенсорс, для «не-чит» мода это важный аргумент доверия) |
| Discord / Issues | можно пропустить |

## Вкладка Upload a version (первый файл)

| Поле | Значение |
|---|---|
| Version number | 1.0.0 |
| Version title | HolySwap 1.0.0 |
| Channel | **release** |
| Game versions | **1.21.4** (потом можно добавить отдельные файлы под 1.20.x/1.21.1–1.21.3) |
| Loaders | **Fabric** |
| Project types | **mod** |
| File | holyswap-1.0.0.jar (build/libs или Загрузки) |
| Changelog | из CHANGELOG.md |

## Чек-лист перед публикацией

- [x] Скриншоты: в gallery/ лежат 4 готовых PNG (баннер, селектор, клавиши, чат) — загрузи их во вкладку **Gallery** проекта (это же закрывает пункт про скриншоты в описании)
- [ ] Залей исходники на GitHub (приватный аккаунт) и укажи Source URL
- [ ] В fabric.mod.json поле authors = ["you"] — если хочешь свой ник, скажи, поменяю и пересоберу
- [ ] Проверь, что описание НЕ упоминает конкретный сервер (упоминание «для серверов с кастомными предметами» — ок)

## Что говорить модерации, если спросят

Мод использует только `ClickSlotC2SPacket` c SlotActionType.SWAP и кнопкой 40 — это тот же пакет, который шлёт ванильная клавиша F. Никакой автоматизации повторов, чтения пакетов или преимуществ по информации. Аналоги на Modrinth: Inventory Profiles Next, offhand-тогглеры.
