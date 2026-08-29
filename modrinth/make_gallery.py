# -*- coding: utf-8 -*-
"""HolySwap promo images for Modrinth gallery: banner + UI mockups (1920x1080)."""
from PIL import Image, ImageDraw, ImageFont

OUT = r"C:\Users\Пользователь\Downloads\holyswap-modrinth\gallery"
ICON = r"C:\Users\Пользователь\.zcode\workspace\default\holyswap\modrinth\icon_512.png"
W, H = 1920, 1080
GOLD = (255, 170, 0)
GOLD_D = (200, 125, 0)
GRAY_TXT = (170, 170, 170)
WHITE = (255, 255, 255)
BTN_BG = (139, 139, 139)
BTN_BRD = (55, 55, 55)
BTN_TXT = (230, 230, 230)

def font(sz, bold=True):
    p = rf"C:\Windows\Fonts\arial{'bd' if bold else ''}.ttf"
    return ImageFont.truetype(p, sz)

def gradient_bg(w=W, h=H):
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    top, bot = (30, 27, 22), (15, 13, 10)
    for y in range(h):
        t = y / h
        d.line([(0, y), (w, y)], fill=tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)))
    # едва заметная сетка "блоков"
    for x in range(0, w, 64):
        d.line([(x, 0), (x, h)], fill=(255, 255, 255, 6), width=1)
    return img

def mc_button(d, box, text, f, text_col=BTN_TXT, bg=BTN_BG, brd=BTN_BRD):
    x0, y0, x1, y1 = box
    d.rectangle([x0, y0, x1, y1], fill=bg, outline=brd, width=3)
    d.rectangle([x0 + 3, y0 + 3, x1 - 3, y0 + 3], fill=tuple(min(255, c + 25) for c in bg))
    bb = d.textbbox((0, 0), text, font=f)
    tw, th = bb[2] - bb[0], bb[3] - bb[1]
    d.text(((x0 + x1) / 2 - tw / 2, (y0 + y1) / 2 - th / 2 - bb[1]), text, font=f, fill=text_col)

def chat_line(img, x, y, parts, f):
    d = ImageDraw.Draw(img, "RGBA")
    bb = d.textbbox((0, 0), "Ag", font=f)
    line_h = bb[3] - bb[1] + 14
    d.rectangle([x - 8, y - 6, 1500, y + line_h], fill=(0, 0, 0, 140))
    cx = x
    for text, col in parts:
        d.text((cx, y), text, font=f, fill=col)
        cw = d.textbbox((0, 0), text, font=f)[2]
        cx += cw
    return line_h

# ---------------- 1. BANNER ----------------
img = gradient_bg()
d = ImageDraw.Draw(img)
d.rounded_rectangle([16, 16, W - 16, H - 16], radius=40, outline=(255, 200, 80, 90), width=6)

icon = Image.open(ICON).resize((300, 300), Image.LANCZOS)
img.paste(icon, (W // 2 - 150, 150), icon)

f_title = font(150)
f_sub = font(54, bold=False)
f_small = font(40, bold=False)
t = "HolySwap"
bb = d.textbbox((0, 0), t, font=f_title)
d.text((W / 2 - (bb[2] - bb[0]) / 2, 500), t, font=f_title, fill=GOLD)
t = "Быстрый свап предмета во вторую руку"
bb = d.textbbox((0, 0), t, font=f_sub)
d.text((W / 2 - (bb[2] - bb[0]) / 2, 700), t, font=f_sub, fill=WHITE)
t = "One-key offhand swap · Fabric 1.21.4 · Client-side, not a cheat"
bb = d.textbbox((0, 0), t, font=f_small)
d.text((W / 2 - (bb[2] - bb[0]) / 2, 790), t, font=f_small, fill=GRAY_TXT)
# стрелки-украшение
for (x, dirn, col) in [(500, 1, GOLD), (W - 620, -1, GOLD_D)]:
    y = 640
    d.rounded_rectangle([x, y - 14, x + 120, y + 14], radius=14, fill=col)
    tip = [(x + (100 if dirn == 1 else 20), y - 40), (x + (120 if dirn == 1 else 0), y), (x + (100 if dirn == 1 else 20), y + 40)]
    d.polygon(tip, fill=col)
img.save(OUT + r"\1_banner.png")

# ---------------- 2. SELECTOR MOCKUP ----------------
img = gradient_bg()
d = ImageDraw.Draw(img)
f_h = font(52)
f_row = font(40, bold=False)
f_hint = font(34, bold=False)

panel = [360, 90, 1560, 990]
d.rounded_rectangle(panel, radius=24, fill=(20, 18, 15, 235), outline=(255, 170, 0, 120), width=4)
t = "HolySwap — выбор предметов свапа"
bb = d.textbbox((0, 0), t, font=f_h)
d.text((W / 2 - (bb[2] - bb[0]) / 2, 115), t, font=f_h, fill=GOLD)
t = "Клик — добавить/убрать. Выбрано: 4."
bb = d.textbbox((0, 0), t, font=f_hint)
d.text((W / 2 - (bb[2] - bb[0]) / 2, 190), t, font=f_hint, fill=GRAY_TXT)
t = "G талисманы · R сферы · V талисманы+ · B тотемы"
bb = d.textbbox((0, 0), t, font=f_hint)
d.text((W / 2 - (bb[2] - bb[0]) / 2, 240), t, font=f_hint, fill=GRAY_TXT)

rows = [
    ("[Талисманы]", (140, 255, 140), True, "Талисман Воромана"),
    ("[Талисманы]", (140, 255, 140), True, "Талисман Зевса"),
    ("[Талисманы+]", (255, 230, 90), True, "Талисман Инфинити+  x1"),
    ("[Сферы]", (90, 200, 255), False, "Сфера вампира  x2"),
    ("[Сферы]", (90, 200, 255), True, "Сфера невидимости  x1"),
    ("[Обычные тотемы]", (255, 120, 220), True, "Тотем бессмертия  x3"),
]
y = 300
for cat, col, sel, name in rows:
    box = [420, y, 1500, y + 62]
    d.rectangle(box, fill=BTN_BG, outline=BTN_BRD, width=3)
    if sel:
        cx, cy = 450, y + 31
        d.line([(cx - 14, cy), (cx - 4, cy + 12), (cx + 16, cy - 14)], fill=(90, 220, 90), width=7)
    d.text((495, y + 14), cat, font=f_row, fill=col)
    wcat = d.textbbox((0, 0), cat + "  ", font=f_row)[2]
    d.text((500 + wcat, y + 14), name, font=f_row, fill=(40, 40, 40))
    y += 78
mc_button(d, [430, 900, 750, 960], "Клавиши…", f_row)
mc_button(d, [1170, 900, 1490, 960], "Закрыть", f_row)
img.save(OUT + r"\2_selector.png")

# ---------------- 3. KEYS MOCKUP ----------------
img = gradient_bg()
d = ImageDraw.Draw(img)
t = "HolySwap — клавиши"
bb = d.textbbox((0, 0), t, font=f_h)
d.text((W / 2 - (bb[2] - bb[0]) / 2, 120), t, font=f_h, fill=GOLD)
t = "Клик по строке, затем нажми нужную клавишу. Esc — отмена."
bb = d.textbbox((0, 0), t, font=f_hint)
d.text((W / 2 - (bb[2] - bb[0]) / 2, 200), t, font=f_hint, fill=GRAY_TXT)

krows = [
    ("Обычные талисманы: ", "G", ""),
    ("Сферы: ", "R", " (общая с: Талисманы)"),
    ("Талисманы+: ", "V", ""),
    ("Обычные тотемы: ", "B", ""),
    ("Открыть селектор: ", "H", ""),
]
y = 300
for name, key, shared in krows:
    box = [560, y, 1360, y + 64]
    d.rectangle(box, fill=BTN_BG, outline=BTN_BRD, width=3)
    d.text((590, y + 15), name, font=f_row, fill=(35, 35, 35))
    wname = d.textbbox((0, 0), name, font=f_row)[2]
    d.text((600 + wname, y + 15), key, font=f_row, fill=(20, 60, 140))
    wkey = d.textbbox((0, 0), key, font=f_row)[2]
    if shared:
        d.text((610 + wname + wkey, y + 15), shared, font=f_row, fill=(80, 80, 80))
    y += 84
mc_button(d, [560, y + 16, 1360, y + 80], "Сбросить на G/R/V/B/H", f_row, text_col=(120, 255, 120))
img.save(OUT + r"\3_keys.png")

# ---------------- 4. CHAT MOCKUP ----------------
img = gradient_bg()
d = ImageDraw.Draw(img)
t = "В чате всегда видно, что легло в оффхенд"
bb = d.textbbox((0, 0), t, font=f_h)
d.text((W / 2 - (bb[2] - bb[0]) / 2, 120), t, font=f_h, fill=WHITE)

f_chat = font(44, bold=False)
y = 720
y += chat_line(img, 40, y, [("[HolySwap] ", GOLD), ("Талисманы", (140, 255, 140)), (": Талисман Воромана", WHITE)], f_chat)
y += chat_line(img, 40, y, [("[HolySwap] ", GOLD), ("Сферы", (90, 200, 255)), (": Сфера невидимости", WHITE)], f_chat)
y += chat_line(img, 40, y, [("[HolySwap] ", GOLD), ("предмет в левой руке («Талисман Зевса») не выбран — добавь его на H", (170, 170, 170))], f_chat)
img.save(OUT + r"\4_chat.png")

print("4 images saved to", OUT)
