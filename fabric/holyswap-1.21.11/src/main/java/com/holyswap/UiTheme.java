package com.holyswap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

/** Палитра и примитивы фирменного интерфейса HolySwap (текстуры + 9-slice). */
public final class UiTheme {
    public static final int BG       = 0x8805050C;
    public static final int PANEL_2  = 0xFF181828;
    public static final int ROW_HOVER= 0xFF2A2A46;
    public static final int ACCENT   = 0xFF8B5CF6;
    public static final int ACCENT_2 = 0xFF22D3EE;
    public static final int TEXT     = 0xFFF2F2FA;
    public static final int TEXT_DIM = 0xFF9494AC;
    public static final int GOOD     = 0xFF34D399;
    public static final int DANGER   = 0xFFF87171;

    private static final Identifier TEX_PANEL = Identifier.fromNamespaceAndPath("holyswap", "textures/gui/panel.png");
    private static final Identifier TEX_ROW   = Identifier.fromNamespaceAndPath("holyswap", "textures/gui/row.png");
    private static final Identifier TEX_BTN   = Identifier.fromNamespaceAndPath("holyswap", "textures/gui/button.png");
    private static final Identifier TEX_CHIP  = Identifier.fromNamespaceAndPath("holyswap", "textures/gui/chip.png");

    private UiTheme() {}

    /** 9-slice: тянем края, углы не трогаем. margin — запечённое свечение вокруг, corner — радиус внутри. */
    private static void nineSlice(GuiGraphics g, Identifier tex, int x, int y, int w, int h,
                                  int margin, int corner, int texSize) {
        int L = margin + corner;                 // размер угловой зоны
        int TW = texSize;
        int cx = x - margin, cy = y - margin;    // полный прямоугольник с полями
        int cw = w + 2 * margin, ch = h + 2 * margin;
        com.mojang.blaze3d.pipeline.RenderPipeline rl = net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED;
        // углы
        g.blit(rl, tex, cx, cy, 0f, 0f, L, L, TW, TW);
        g.blit(rl, tex, cx + cw - L, cy, TW - L, 0f, L, L, TW, TW);
        g.blit(rl, tex, cx, cy + ch - L, 0f, TW - L, L, L, TW, TW);
        g.blit(rl, tex, cx + cw - L, cy + ch - L, TW - L, TW - L, L, L, TW, TW);
        // края
        g.blit(rl, tex, cx + L, cy, L, 0f, cw - 2 * L, L, TW, TW);
        g.blit(rl, tex, cx + L, cy + ch - L, L, TW - L, cw - 2 * L, L, TW, TW);
        g.blit(rl, tex, cx, cy + L, 0f, L, L, ch - 2 * L, TW, TW);
        g.blit(rl, tex, cx + cw - L, cy + L, TW - L, L, L, ch - 2 * L, TW, TW);
        // центр
        g.blit(rl, tex, cx + L, cy + L, L, L, cw - 2 * L, ch - 2 * L, TW, TW);
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        nineSlice(g, TEX_PANEL, x, y, w, h, 10, 12, 84);
    }

    public static void rowBg(GuiGraphics g, int x, int y, int w, int h) {
        nineSlice(g, TEX_ROW, x, y, w, h, 2, 6, 68);
    }

    public static void button(GuiGraphics g, int x, int y, int w, int h) {
        nineSlice(g, TEX_BTN, x, y, w, h, 2, 6, 68);
    }

    public static void chip(GuiGraphics g, int x, int y, int w, int h) {
        nineSlice(g, TEX_CHIP, x, y, w, h, 2, 6, 52);
    }

    public static void hGradient(GuiGraphics g, int x, int y, int w, int h, int from, int to) {
        int steps = Math.max(1, w);
        for (int i = 0; i < steps; i++) {
            g.fill(x + i, y, x + i + 1, y + h, lerpColor(from, to, i / (float) steps));
        }
    }

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
