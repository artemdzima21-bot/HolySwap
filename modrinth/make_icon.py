# -*- coding: utf-8 -*-
"""HolySwap icon: dark rounded square, two swap arrows (orange/gold)."""
from PIL import Image, ImageDraw

S = 512
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

# фон: тёмный градиентный квадрат со скруглением
bg = Image.new("RGBA", (S, S), (0, 0, 0, 0))
bgd = ImageDraw.Draw(bg)
top = (34, 30, 24)
bot = (16, 14, 10)
for y in range(S):
    t = y / S
    c = tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)) + (255,)
    bgd.line([(0, y), (S, y)], fill=c)
mask = Image.new("L", (S, S), 0)
ImageDraw.Draw(mask).rounded_rectangle([8, 8, S - 8, S - 8], radius=96, fill=255)
img.paste(bg, (0, 0), mask)

GOLD = (255, 170, 0, 255)     # #FFAA00 как в чате мода
GOLD_D = (200, 125, 0, 255)

def arrow(d, y, direction, color):
    """Горизонтальная стрелка: direction=1 вправо, -1 влево."""
    x0, x1 = 116, 396
    shaft = 34
    head = 64
    if direction == 1:
        body = [x0, y - shaft // 2, x1 - head, y + shaft // 2]
        tip = [(x1 - head, y - head // 2), (x1, y), (x1 - head, y + head // 2)]
    else:
        body = [x0 + head, y - shaft // 2, x1, y + shaft // 2]
        tip = [(x0 + head, y - head // 2), (x0, y), (x0 + head, y + head // 2)]
    d.rounded_rectangle(body, radius=shaft // 2, fill=color)
    d.polygon(tip, fill=color)

arrow(d, 196, 1, GOLD)          # верхняя — вправо
arrow(d, 316, -1, GOLD_D)       # нижняя — влево

# тонкая рамка
d.rounded_rectangle([8, 8, S - 8, S - 8], radius=96, outline=(255, 200, 80, 90), width=6)

img.save(r"C:\Users\Пользователь\.zcode\workspace\default\holyswap\modrinth\icon_512.png")
img.resize((128, 128), Image.LANCZOS).save(
    r"C:\Users\Пользователь\.zcode\workspace\default\holyswap\src\main\resources\assets\holyswap\icon.png")
print("icons saved")
