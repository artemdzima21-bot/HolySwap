package com.holyswap;

/** Общая палитра и примитивы «клиентского» интерфейса HolySwap. */
public final class UiTheme {
    public static final int BG        = 0xD8070710; // фон-затемнение экрана
    public static final int PANEL     = 0xF0121220; // панель
    public static final int PANEL_2   = 0xFF1B1B2C; // строка/вложение
    public static final int ROW_HOVER = 0xFF252540; // ховер строки
    public static final int ACCENT    = 0xFF7C4DFF; // основной акцент (фиолет)
    public static final int ACCENT_2  = 0xFF00E5FF; // второй акцент (циан)
    public static final int TEXT      = 0xFFE8E8F2;
    public static final int TEXT_DIM  = 0xFF8A8AA0;
    public static final int GOOD      = 0xFF00E676;
    public static final int DANGER    = 0xFFFF5252;

    private UiTheme() {}

    /** Скруглённая панель: основной прямоугольник + рамка из тонких полос. */
    public static void panel(net.minecraft.client.gui.GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + h, PANEL);
        g.fill(x, y + 1, x + 1, y + h - 1, PANEL);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, PANEL);
        border(g, x, y, w, h, 0x507C4DFF);
    }

    public static void border(net.minecraft.client.gui.GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** Градиентный текст — по букве от ACCENT к ACCENT_2. */
    public static void gradientTitle(net.minecraft.client.gui.GuiGraphics g,
                                     net.minecraft.client.gui.Font font,
                                     String text, float x, float y) {
        int total = font.width(text);
        int acc = 0;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            float t = total == 0 ? 0 : (acc + font.width(ch) / 2f) / total;
            int c = lerpColor(ACCENT_2, ACCENT, t);
            g.drawString(font, ch, (int)(x + acc), (int)(y), c);
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

    /** Обрезка строки под ширину с «…». */
    public static String ellipsize(net.minecraft.client.gui.Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) return s;
        while (s.length() > 1 && font.width(s + "…") > maxWidth) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    public static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
