# HolySwap — быстрый свап предметов во вторую руку

**HolySwap** — клиентский мод для Fabric 1.21.4: назначаешь свои клавиши и одним нажатием кладёшь нужный предмет во вторую руку (оффхенд). Создан для серверов с кастомными предметами (талисманы, сферы, тотемы и т.п.), где их приходится постоянно менять местами.

> **Это не чит.** Каждый свап — один стандартный клик-пакет, точно такой же, как у ванильной клавиши F «поменять предмет в руках». Мод ничего не автоматизирует по таймингам, не читает пакеты сервера и не даёт информации, которой нет у обычного игрока. Правила конкретного сервера всегда в приоритете.

## Возможности

- **Кнопка на каждую категорию предметов** — обычные талисманы, улучшенные (с «+»), сферы, тотемы. Категории определяются автоматически: тотем — по типу предмета, талисманы/сферы — по имени.
- **Циклический свап** — каждое нажатие кладёт в оффхенд следующий выбранный предмет категории, по кругу.
- **Общая кнопка на несколько категорий** — если назначить одну клавишу на несколько категорий, она листает их по кругу: талисман → сфера → талисман. Предмет в руке не заменяется однородным: талисман не перескочит на другой талисман, пока в цикле категория та же.
- **Экран-селектор** — показывает предметы из инвентаря (включая то, что уже в оффхенде), сгруппированные по категориям; клик добавляет/убирает предмет из цикла свапа.
- **Экран «Клавиши»** — назначь любые клавиши прямо в игре: клик по строке → нажми кнопку → готово. Без перезахода, клавиши не зависят от ванильного меню управления.
- **Конфиг сохраняется** в `config/holyswap.json` и переживает перезаход.

## Как пользоваться

1. Возьми HolySwap в моды и зайди на сервер.
2. Нажми клавишу селектора (по умолчанию **H**) и отметь предметы, которые хочешь свапать.
3. Кнопка «Клавиши…» внизу — назначь свои клавиши (по умолчанию: **G** талисманы, **R** сферы, **V** талисманы+, **B** тотемы).
4. Жми — предмет кладётся в оффхенд, в чате видно, что легло.

## Для серверов

Мод полностью клиентский, серверу ничего не требует (работает и в сингле). Свап неотличим от ручного: тот же пакет, что и ванильная клавиша F.

---

# HolySwap — quick offhand swap

**HolySwap** is a client-side Fabric 1.21.4 mod: bind your own keys and put the right item into your offhand with a single press. Built for servers with custom items (talismans, spheres, totems) that you constantly swap around.

> **Not a cheat.** Every swap is one standard click packet, identical to the vanilla "swap item with offhand" key (F). No timing automation, no packet reading, no information a normal player wouldn't have. Your server's rules always take priority.

## Features

- **A key per item category** — plain talismans, upgraded ("+"), spheres, totems. Categories are detected automatically: totems by item type, talismans/spheres by name.
- **Cycle swap** — each press puts the next selected item of that category into the offhand, wrapping around.
- **One key for several categories** — binds shared between categories cycle the categories themselves: talisman → sphere → talisman. An item already in your offhand is never replaced by a same-category one.
- **Selector screen** — lists inventory items (offhand included) grouped by category; click to add/remove items from the swap cycle.
- **Keys screen** — rebind everything in-game: click a row, press a key, done. Applies instantly, independent from vanilla controls.
- **Config persisted** to `config/holyswap.json`.

## Getting started

1. Drop HolySwap into your mods folder and join a server.
2. Press the selector key (default **H**) and tick the items you want to swap.
3. Use the "Keys" button to set your own binds (defaults: **G** talismans, **R** spheres, **V** talismans+, **B** totems).
4. Press away — the item lands in your offhand, with a chat confirmation.

## Server-side

Fully client-side, works on vanilla servers and in singleplayer. Swaps are indistinguishable from manual play.
