package com.holyswap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Селектор в фирменном стиле: тёмная панель со свечением, сайдбар категорий,
 * строки с иконками предметов, плавные акценты. Кастомная отрисовка, без vanilla-виджетов.
 */
public class SelectorScreen extends Screen {
    private static final int PANEL_W = 420;
    private static final int SIDEBAR_W = 104;
    private static final int ROW_H = 26;
    private static final int HEADER = 56;
    private static final int FOOTER = 40;

    private final SwapConfig config;
    private final List<SwapLogic.Row> rows = new ArrayList<>();
    private int scroll = 0;
    private int tab = -1;
    private int hoveredRow = -1;
    private int hoveredSide = -1;
    private int hoverBtn = -1; // 0=клавиши 1=звук 2=закрыть

    public SelectorScreen(SwapConfig config) {
        super(Component.translatable("holyswap.screen.selector.title"));
        this.config = config;
    }

    private record Side(String id, String label, int color) {}

    private List<Side> sides() {
        List<Side> out = new ArrayList<>();
        out.add(new Side("__all", Component.translatable("holyswap.tab.all").getString(), UiTheme.ACCENT));
        for (SwapLogic.Category c : SwapLogic.Category.values()) {
            out.add(new Side("__cat" + c.ordinal(), c.title(), catColor(c)));
        }
        return out;
    }

    private static int catColor(SwapLogic.Category c) {
        return switch (c) {
            case TALISMAN -> 0xFF4ADE80;
            case TALISMAN_PLUS -> 0xFFFACC15;
            case SPHERE -> 0xFF38BDF8;
            case SPHERE_PLUS -> 0xFF818CF8;
            case TOTEM -> 0xFFF472B6;
            default -> UiTheme.TEXT_DIM;
        };
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
    private int panelY() { return Math.max(8, this.height / 2 - 120); }
    private int panelH() { return Math.min(this.height - 2 * panelY(), HEADER + FOOTER + ROW_H * 10); }
    private int sideX()  { return panelX() + 10; }
    private int listX()  { return panelX() + SIDEBAR_W + 8; }
    private int listW()  { return panelX() + PANEL_W - 10 - listX(); }
    private int listY()  { return panelY() + HEADER; }
    private int listH()  { return panelH() - HEADER - FOOTER; }
    private int maxScroll() { return Math.max(0, visibleRows().size() - listH() / ROW_H); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int px = panelX(), py = panelY(), pw = PANEL_W, ph = panelH();
        g.fill(0, 0, this.width, this.height, UiTheme.BG);
        UiTheme.panel(g, px, py, pw, ph, UiTheme.ACCENT);
        UiTheme.hGradient(g, px + 2, py + 2, pw - 4, 2, UiTheme.ACCENT_2, UiTheme.ACCENT);

        // шапка
        String logo = "HOLYSWAP";
        int logoW = font.width(logo);
        UiTheme.gradientTitle(g, font, logo, px + (pw - logoW) / 2f, py + 10);
        String hint = Component.translatable("holyswap.screen.selector.hint", config.targets.size()).getString();
        g.drawCenteredString(font, Component.literal(hint), px + pw / 2, py + 25, UiTheme.TEXT_DIM);
        UiTheme.hGradient(g, px + 10, py + HEADER - 5, pw - 20, 1, UiTheme.ACCENT, UiTheme.ACCENT_2);

        // сайдбар
        hoveredSide = -1;
        int sy = listY();
        List<Side> sides = sides();
        for (int i = 0; i < sides.size(); i++) {
            Side s = sides.get(i);
            boolean active = (s.id().equals("__all") && tab == -1)
                    || (s.id().startsWith("__cat") && tab == Integer.parseInt(s.id().substring(5)));
            boolean hover = UiTheme.inRect(mouseX, mouseY, sideX(), sy, SIDEBAR_W - 8, 20);
            if (hover) hoveredSide = i;
            if (active) {
                g.fill(sideX(), sy, sideX() + SIDEBAR_W - 8, sy + 20, UiTheme.ROW_HOVER);
                g.fill(sideX(), sy, sideX() + 2, sy + 20, s.color());
            } else if (hover) {
                g.fill(sideX(), sy, sideX() + SIDEBAR_W - 8, sy + 20, UiTheme.PANEL_2);
            }
            g.fill(sideX() + 6, sy + 8, sideX() + 8, sy + 12, s.color());
            String lbl = UiTheme.ellipsize(font, s.label(), SIDEBAR_W - 24);
            g.drawString(font, lbl, sideX() + 13, sy + 6,
                    active ? UiTheme.TEXT : (hover ? UiTheme.TEXT : UiTheme.TEXT_DIM));
            sy += 22;
        }

        // строки предметов
        List<SwapLogic.Row> vis = visibleRows();
        hoveredRow = -1;
        int y = listY();
        if (vis.isEmpty()) {
            String empty = Component.translatable("holyswap.screen.empty").getString();
            g.drawCenteredString(font, Component.literal(empty),
                    listX() + listW() / 2, listY() + listH() / 2 - 4, UiTheme.TEXT_DIM);
        }
        for (int i = scroll; i < vis.size(); i++) {
            if (y + ROW_H > listY() + listH()) break;
            SwapLogic.Row row = vis.get(i);
            String match = matchOf(row.label());
            boolean selected = config.targets.contains(match);
            boolean hover = UiTheme.inRect(mouseX, mouseY, listX(), y, listW(), ROW_H);
            if (hover) hoveredRow = i;

            // карточка строки
            g.fill(listX() + 1, y + 1, listX() + listW() - 1, y + ROW_H - 1,
                    hover ? UiTheme.ROW_HOVER : UiTheme.ROW);
            if (selected) {
                g.fill(listX() + 1, y + 1, listX() + 3, y + ROW_H - 1, UiTheme.GOOD);
            } else if (hover) {
                g.fill(listX() + 1, y + 1, listX() + listW() - 1, y + 2,
                        (UiTheme.ACCENT & 0x00FFFFFF) | 0x50000000);
            }

            // иконка предмета
            ItemStack icon = row.icon();
            if (icon != null && !icon.isEmpty()) {
                g.renderItem(icon, listX() + 6, y + 4);
            }

            int cx = listX() + 26;
            String label = UiTheme.ellipsize(font, row.label(), listW() - 60);
            g.drawString(font, label, cx, y + 9, selected ? UiTheme.GOOD : UiTheme.TEXT);
            String cnt = "x" + row.count();
            g.drawString(font, cnt, listX() + listW() - font.width(cnt) - 8,
                    y + 9, UiTheme.TEXT_DIM);
            // цветная точка категории
            g.fill(listX() + listW() - 22, y + 11, listX() + listW() - 18, y + 15, catColor(row.category()));
            y += ROW_H;
        }

        // футер
        int fy = py + ph - FOOTER + 8;
        hoverBtn = -1;
        if (UiTheme.inRect(mouseX, mouseY, px + 10, fy - 3, 92, 18)) hoverBtn = 0;
        if (UiTheme.inRect(mouseX, mouseY, px + 108, fy - 3, 100, 18)) hoverBtn = 1;
        if (UiTheme.inRect(mouseX, mouseY, px + pw - 34, fy - 3, 24, 18)) hoverBtn = 2;
        footBtn(g, 0, Component.translatable("holyswap.screen.keys_button").getString(), px + 10, fy, 92);
        footBtn(g, 1, soundLabel().getString(), px + 108, fy, 100);
        footBtn(g, 2, "✕", px + pw - 34, fy, 24);
    }

    private void footBtn(GuiGraphics g, int id, String label, int x, int y, int w) {
        boolean hover = hoverBtn == id;
        int accent = id == 2 ? UiTheme.DANGER : UiTheme.ACCENT;
        UiTheme.button(g, x, y, w, 18, hover, accent);
        g.drawCenteredString(font, Component.literal(label), x + w / 2, y + 5,
                hover ? UiTheme.TEXT : UiTheme.TEXT_DIM);
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

    private int btnY() { return panelY() + panelH() - FOOTER + 11; }
    private boolean btnRect(double mx, double my, int id, int x, int w) {
        return UiTheme.inRect(mx, my, x, btnY() - 3, w, 18);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        double mx = event.x(), my = event.y();
        if (event.button() == 0) {
            // сайдбар
            int sy = listY();
            List<Side> sides = sides();
            for (int i = 0; i < sides.size(); i++) {
                if (UiTheme.inRect(mx, my, sideX(), sy, SIDEBAR_W - 8, 20)) {
                    tab = sides.get(i).id().equals("__all") ? -1 : Integer.parseInt(sides.get(i).id().substring(5));
                    scroll = 0;
                    return true;
                }
                sy += 22;
            }
            // строки
            if (hoveredRow >= 0 && hoveredRow < visibleRows().size()) {
                String match = matchOf(visibleRows().get(hoveredRow).label());
                if (!config.targets.remove(match)) config.targets.add(match);
                config.save();
                return true;
            }
            // кнопки
            if (btnRect(mx, my, 0, panelX() + 10, 92)) { minecraft.setScreen(new KeysScreen(config)); return true; }
            if (btnRect(mx, my, 1, panelX() + 108, 100)) {
                config.sound = nextSound(config.sound);
                config.save();
                HolySwapClient.playSwapSound(minecraft);
                return true;
            }
            if (btnRect(mx, my, 2, panelX() + PANEL_W - 34, 24)) { onClose(); return true; }
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
