package com.holyswap;

import net.minecraft.client.gui.GuiGraphics;

/** Палитра и примитивы фирменного интерфейса HolySwap. */
public final class UiTheme {
    public static final int BG       = 0x9005050C;
    public static final int PANEL    = 0xF410101C;
    public static final int PANEL_2  = 0xFF181828;
    public static final int ROW      = 0xFF1C1C2E;
    public static final int ROW_HOVER= 0xFF2A2A46;
    public static final int ACCENT   = 0xFF8B5CF6; // фиолет
    public static final int ACCENT_2 = 0xFF22D3EE; // циан
    public static final int TEXT     = 0xFFF2F2FA;
    public static final int TEXT_DIM = 0xFF9494AC;
    public static final int GOOD     = 0xFF34D399;
    public static final int DANGER   = 0xFFF87171;

    private UiTheme() {}

    /** Скруглённая панель со свечением по контуру. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int accent) {
        // внешнее свечение (3 слоя)
        g.fill(x - 3, y - 3, x + w + 3, y + h + 3, (accent & 0x00FFFFFF) | 0x18000000);
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, (accent & 0x00FFFFFF) | 0x30000000);
        // корпус со срезанными углами (2px)
        g.fill(x + 2, y, x + w - 2, y + h, PANEL);
        g.fill(x, y + 2, x + w, y + h - 2, PANEL);
        corner(g, x, y, w, h, PANEL);
        // рамка со срезанными углами
        g.fill(x + 2, y, x + w - 2, y + 1, accent);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, (accent & 0x00FFFFFF) | 0x60000000);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, accent);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, (accent & 0x00FFFFFF) | 0x60000000);
        g.fill(x, y + 2, x + 1, y + h - 2, accent);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, accent);
    }

    private static void corner(GuiGraphics g, int x, int y, int w, int h, int color) {
        // убираем 2x2 углы (рисуем поверх фоном нельзя — просто закрашиваем тело без углов не нужно для fill)
    }

    /** Вертикальный градиент внутри прямоугольника. */
    public static void vGradient(GuiGraphics g, int x, int y, int w, int h, int from, int to) {
        int steps = Math.max(1, h);
        for (int i = 0; i < steps; i++) {
            g.fill(x, y + i, x + w, y + i + 1, lerpColor(from, to, i / (float) steps));
        }
    }

    /** Горизонтальный градиент. */
    public static void hGradient(GuiGraphics g, int x, int y, int w, int h, int from, int to) {
        int steps = Math.max(1, w);
        for (int i = 0; i < steps; i++) {
            g.fill(x + i, y, x + i + 1, y + h, lerpColor(from, to, i / (float) steps));
        }
    }

    /** Плашка-кнопка со скруглением и свечением при ховере. */
    public static void button(GuiGraphics g, int x, int y, int w, int h, boolean hover, int accent) {
        g.fill(x + 1, y, x + w - 1, y + h, hover ? ROW_HOVER : PANEL_2);
        g.fill(x, y + 1, x + w, y + h - 1, hover ? ROW_HOVER : PANEL_2);
        int border = hover ? accent : ((accent & 0x00FFFFFF) | 0x50000000);
        g.fill(x + 1, y, x + w - 1, y + 1, border);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, border);
        g.fill(x, y + 1, x + 1, y + h - 1, border);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
        if (hover) {
            g.fill(x + 1, y + 1, x + w - 1, y + 2, (accent & 0x00FFFFFF) | 0x40000000);
        }
    }

    /** Градиентный текст — по букве от ACCENT_2 к ACCENT. */
    public static void gradientTitle(GuiGraphics g, net.minecraft.client.gui.Font font,
                                     String text, float x, float y) {
        int total = font.width(text);
        int acc = 0;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            float t = total == 0 ? 0 : (acc + font.width(ch) / 2f) / total;
            g.drawString(font, ch, (int)(x + acc), (int)(y), lerpColor(ACCENT_2, ACCENT, t));
            acc += font.width(ch);
        }
    }

    public static int lerpColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (from >>> 24), r = (from >> 16) & 0xFF, gr = (from >> 8) & 0xFF, b = from & 0xFF;
        int a2 = (to >>> 24), r2 = (to >> 16) & 0xFF, g2 = (to >> 8) & 0xFF, b2 = to & 0xFF;
        return ((a + (int)((a2 - a) * t)) << 24)
             | ((r + (int)((r2 - r) * t)) << 16)
             | ((gr + (int)((g2 - gr) * t)) << 8)
             | (b + (int)((b2 - b) * t));
    }

    public static String ellipsize(net.minecraft.client.gui.Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) return s;
        while (s.length() > 1 && font.width(s + "…") > maxWidth) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    public static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
