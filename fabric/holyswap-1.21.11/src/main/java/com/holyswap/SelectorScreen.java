package com.holyswap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран-селектор в «клиентском» стиле: тёмная панель, градиентный логотип,
 * табы категорий, строки предметов с хайлайтом, кнопки звук/клавиши/закрыть.
 * Вся отрисовка кастомная — клики через hit-test, vanilla-виджеты не используются.
 */
public class SelectorScreen extends Screen {
    private static final int PANEL_W = 320;
    private static final int ROW_H = 22;
    private static final int HEADER = 58;
    private static final int FOOTER = 34;

    private final SwapConfig config;
    private final List<SwapLogic.Row> rows = new ArrayList<>();
    private int scroll = 0;
    private int tab = -1;                       // -1 = все категории
    private int hoveredRow = -1;
    private boolean hoveringKeys = false, hoveringSound = false, hoveringClose = false;

    public SelectorScreen(SwapConfig config) {
        super(Component.translatable("holyswap.screen.selector.title"));
        this.config = config;
    }

    private record Tab(String id, String label) {}

    private List<Tab> tabs() {
        List<Tab> out = new ArrayList<>();
        out.add(new Tab("__all", Component.translatable("holyswap.tab.all").getString()));
        for (SwapLogic.Category c : SwapLogic.Category.values()) {
            out.add(new Tab("__cat" + c.ordinal(), c.colorTag + c.title()));
        }
        return out;
    }

    @Override
    protected void init() {
        rows.clear();
        if (minecraft != null && minecraft.player != null) rows.addAll(SwapLogic.listDistinct(minecraft.player));
        scroll = 0;
    }

    private List<SwapLogic.Row> visibleRows() {
        if (tab < 0) return rows;
        SwapLogic.Category cat = SwapLogic.Category.values()[tab];
        List<SwapLogic.Row> out = new ArrayList<>();
        for (SwapLogic.Row r : rows) if (r.category() == cat) out.add(r);
        return out;
    }

    private int panelX() { return (this.width - PANEL_W) / 2; }
    private int panelY() { return Math.max(10, this.height / 2 - 110); }
    private int panelH() { return Math.min(this.height - 2 * panelY(), HEADER + FOOTER + ROW_H * 9); }
    private int listX()  { return panelX() + 10; }
    private int listW()  { return PANEL_W - 20; }
    private int listY()  { return panelY() + HEADER; }
    private int listH()  { return panelH() - HEADER - FOOTER; }
    private int maxScroll() { return Math.max(0, visibleRows().size() - listH() / ROW_H); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int px = panelX(), py = panelY(), pw = PANEL_W, ph = panelH();
        g.fill(0, 0, this.width, this.height, UiTheme.BG);
        UiTheme.panel(g, px, py, pw, ph);
        for (int i = 0; i < pw; i++) {
            g.fill(px + i, py, px + i + 1, py + 2, UiTheme.lerpColor(UiTheme.ACCENT_2, UiTheme.ACCENT, i / (float) pw));
        }

        // логотип
        String logo = "HOLYSWAP";
        int logoW = font.width(logo);
        UiTheme.gradientTitle(g, font, logo, px + (pw - logoW) / 2f, py + 10);
        String hint = Component.translatable("holyswap.screen.selector.hint", config.targets.size()).getString();
        g.drawCenteredString(font, Component.literal(hint), px + pw / 2, py + 24, UiTheme.TEXT_DIM);

        // табы категорий
        int tx = px + 10, ty = py + 36;
        for (Tab t : tabs()) {
            int tw = font.width(t.label()) + 12;
            boolean active = (t.id().equals("__all") && tab == -1)
                    || (t.id().startsWith("__cat") && tab == Integer.parseInt(t.id().substring(5)));
            if (active) {
                g.fill(tx, ty, tx + tw, ty + 15, UiTheme.PANEL_2);
                g.fill(tx, ty + 13, tx + tw, ty + 15, UiTheme.ACCENT);
            }
            g.drawString(font, t.label(), tx + 6, ty + 4, active ? UiTheme.TEXT : UiTheme.TEXT_DIM);
            tx += tw + 4;
        }
        g.fill(px + 10, py + HEADER - 4, px + pw - 10, py + HEADER - 3, 0x307C4DFF);

        // строки предметов
        List<SwapLogic.Row> vis = visibleRows();
        hoveredRow = -1;
        int y = listY();
        for (int i = scroll; i < vis.size(); i++) {
            if (y + ROW_H > listY() + listH()) break;
            SwapLogic.Row row = vis.get(i);
            String match = matchOf(row.label());
            boolean selected = config.targets.contains(match);
            boolean hover = UiTheme.inRect(mouseX, mouseY, listX(), y, listW(), ROW_H);
            if (hover) hoveredRow = i;
            if (hover) g.fill(listX(), y, listX() + listW(), y + ROW_H, UiTheme.ROW_HOVER);
            else if ((i - scroll) % 2 == 0) g.fill(listX(), y, listX() + listW(), y + ROW_H, UiTheme.PANEL_2);
            if (selected) g.fill(listX(), y, listX() + 2, y + ROW_H, UiTheme.GOOD);

            int cx = listX() + 8;
            g.drawString(font, selected ? "✔" : "·", cx, y + 7,
                    selected ? UiTheme.GOOD : UiTheme.TEXT_DIM);
            cx += 12;
            boolean newCat = i == 0 || vis.get(i - 1).category() != row.category();
            String tag = newCat ? row.category().colorTag + "[" + row.category().title() + "] " : "   ";
            g.drawString(font, tag, cx, y + 7, UiTheme.TEXT);
            cx += font.width(tag);
            String label = UiTheme.ellipsize(font, row.label(), listX() + listW() - cx - 30);
            g.drawString(font, label, cx, y + 7, selected ? UiTheme.GOOD : UiTheme.TEXT);
            String cnt = "x" + row.count();
            g.drawString(font, cnt, listX() + listW() - font.width(cnt) - 6,
                    y + 7, UiTheme.TEXT_DIM);
            y += ROW_H;
        }

        // футер
        int fy = py + ph - FOOTER + 8;
        drawFooterButton(g, Component.translatable("holyswap.screen.keys_button").getString(),
                px + 10, fy, 86, hoveringKeys);
        drawFooterButton(g, soundLabel().getString(), px + 102, fy, 96, hoveringSound);
        g.drawString(font, "✕", px + pw - 16, fy,
                hoveringClose ? UiTheme.DANGER : UiTheme.TEXT_DIM);
    }

    private void drawFooterButton(GuiGraphics g, String label, int x, int y, int w, boolean hover) {
        g.fill(x, y - 3, x + w, y + 15, hover ? UiTheme.ROW_HOVER : UiTheme.PANEL_2);
        UiTheme.border(g, x, y - 3, w, 18, hover ? UiTheme.ACCENT : 0x407C4DFF);
        g.drawCenteredString(font, Component.literal(label), x + w / 2, y + 2, UiTheme.TEXT);
    }

    private Component soundLabel() {
        List<String> sounds = List.of("none", "pop", "bubble", "candy", "sparkle");
        String cur = sounds.contains(config.sound) ? config.sound : "pop";
        return Component.translatable("holyswap.screen.sound", Component.translatable("holyswap.sound." + cur));
    }

    private static String nextSound(String cur) {
        List<String> s = List.of("none", "pop", "bubble", "candy", "sparkle");
        int i = s.indexOf(s.contains(cur) ? cur : "pop");
        return s.get((i + 1) % s.size());
    }

    private static String matchOf(String label) {
        return label.contains(" (") ? label.substring(0, label.lastIndexOf(" (")) : label;
    }

    private boolean footerKeysRect(double mx, double my) {
        return UiTheme.inRect(mx, my, panelX() + 10, panelY() + panelH() - FOOTER + 5, 86, 18);
    }
    private boolean footerSoundRect(double mx, double my) {
        return UiTheme.inRect(mx, my, panelX() + 102, panelY() + panelH() - FOOTER + 5, 96, 18);
    }
    private boolean footerCloseRect(double mx, double my) {
        return UiTheme.inRect(mx, my, panelX() + PANEL_W - 26, panelY() + panelH() - FOOTER + 5, 20, 18);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        double mx = event.x(), my = event.y();
        if (event.button() == 0) {
            int tx = panelX() + 10, ty = panelY() + 36;
            for (Tab t : tabs()) {
                int tw = font.width(t.label()) + 12;
                if (UiTheme.inRect(mx, my, tx, ty, tw, 16)) {
                    tab = t.id().equals("__all") ? -1 : Integer.parseInt(t.id().substring(5));
                    scroll = 0;
                    return true;
                }
                tx += tw + 4;
            }
            if (hoveredRow >= 0 && hoveredRow < visibleRows().size()) {
                String match = matchOf(visibleRows().get(hoveredRow).label());
                if (!config.targets.remove(match)) config.targets.add(match);
                config.save();
                return true;
            }
            if (footerKeysRect(mx, my)) { minecraft.setScreen(new KeysScreen(config)); return true; }
            if (footerSoundRect(mx, my)) {
                config.sound = nextSound(config.sound);
                config.save();
                HolySwapClient.playSwapSound(minecraft);
                return true;
            }
            if (footerCloseRect(mx, my)) { onClose(); return true; }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll = Mth.clamp(scroll - (int) vertical, 0, maxScroll());
        return true;
    }

    @Override
    public void onClose() {
        config.save();
        minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
