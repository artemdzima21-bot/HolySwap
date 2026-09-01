package com.holyswap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран назначения клавиш в фирменном стиле: клик по строке -> нажми клавишу.
 * Каждому предмету из инвентаря — своя строка. Escape отменяет выбор.
 */
public class KeysScreen extends Screen {
    private static final int PANEL_W = 420;
    private static final int ROW_H = 26;
    private static final int HEADER = 50;
    private static final int FOOTER = 40;

    private record Act(String id, String label, int color) {}

    /** Фиксированные действия + отдельная строка на каждый предмет в инвентаре. */
    private List<Act> actions() {
        List<Act> out = new ArrayList<>(List.of(
                new Act(SwapConfig.ACT_TALISMAN, Component.translatable("holyswap.category.talisman").getString(), 0xFF4ADE80),
                new Act(SwapConfig.ACT_SPHERE, Component.translatable("holyswap.category.sphere").getString(), 0xFF38BDF8),
                new Act(SwapConfig.ACT_SPHERE_PLUS, Component.translatable("holyswap.category.sphere_plus").getString(), 0xFF818CF8),
                new Act(SwapConfig.ACT_TALISMAN_PLUS, Component.translatable("holyswap.category.talisman_plus").getString(), 0xFFFACC15),
                new Act(SwapConfig.ACT_TOTEM, Component.translatable("holyswap.category.totem").getString(), 0xFFF472B6),
                new Act(SwapConfig.ACT_SELECTOR, Component.translatable("holyswap.action.selector").getString(), UiTheme.ACCENT)
        ));
        if (minecraft != null && minecraft.player != null) {
            for (SwapLogic.Row row : SwapLogic.listDistinct(minecraft.player)) {
                String match = row.label().contains(" (")
                        ? row.label().substring(0, row.label().lastIndexOf(" (")) : row.label();
                out.add(new Act(SwapConfig.ACT_ITEM_PREFIX + match, match, UiTheme.ACCENT_2));
            }
        }
        return out;
    }

    private final SwapConfig config;
    private int listening = -1;
    private int hoveredRow = -1;
    private int hoverBtn = -1;
    private int scroll = 0;

    public KeysScreen(SwapConfig config) {
        super(Component.translatable("holyswap.screen.keys.title"));
        this.config = config;
    }

    private int panelX() { return (this.width - PANEL_W) / 2; }
    private int panelY() { return Math.max(8, this.height / 2 - 120); }
    private int panelH() { return Math.min(this.height - 2 * panelY(), HEADER + FOOTER + ROW_H * 10); }
    private int listX()  { return panelX() + 10; }
    private int listW()  { return PANEL_W - 20; }
    private int listY()  { return panelY() + HEADER; }
    private int listH()  { return panelH() - HEADER - FOOTER; }
    private int maxScroll() { return Math.max(0, actions().size() - listH() / ROW_H); }

    @Override
    protected void init() {
        scroll = 0;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int px = panelX(), py = panelY(), pw = PANEL_W, ph = panelH();
        g.fill(0, 0, this.width, this.height, UiTheme.BG);
        UiTheme.panel(g, px, py, pw, ph);
        UiTheme.hGradient(g, px + 2, py + 2, pw - 4, 2, UiTheme.ACCENT_2, UiTheme.ACCENT);

        String logo = Component.translatable("holyswap.screen.keys.title").getString();
        int logoW = font.width(logo);
        UiTheme.gradientTitle(g, font, logo, px + (pw - logoW) / 2f, py + 10);
        String hint = Component.translatable("holyswap.screen.keys.hint").getString();
        g.drawCenteredString(font, Component.literal(hint), px + pw / 2, py + 25, UiTheme.TEXT_DIM);
        UiTheme.hGradient(g, px + 10, py + HEADER - 5, pw - 20, 1, UiTheme.ACCENT, UiTheme.ACCENT_2);

        List<Act> acts = actions();
        hoveredRow = -1;
        int y = listY();
        for (int i = scroll; i < acts.size(); i++) {
            if (y + ROW_H > listY() + listH()) break;
            Act act = acts.get(i);
            boolean hover = UiTheme.inRect(mouseX, mouseY, listX(), y, listW(), ROW_H);
            if (hover) hoveredRow = i;
            boolean isListening = listening == i;

            UiTheme.rowBg(g, listX(), y, listW(), ROW_H);
            g.fill(listX() + 1, y + 1, listX() + 3, y + ROW_H - 1,
                    isListening ? UiTheme.ACCENT_2 : act.color());

            String label = UiTheme.ellipsize(font, act.label(), listW() - 110);
            g.drawString(font, label, listX() + 10, y + 9, UiTheme.TEXT);

            int code = config.keyFor(act.id(), HolySwapClient.defKey(act.id()));
            String keyName;
            int keyColor;
            if (isListening) {
                keyName = "▸ " + Component.translatable("holyswap.screen.keys.listening").getString();
                keyColor = UiTheme.ACCENT_2;
            } else if (code == GLFW.GLFW_KEY_UNKNOWN) {
                keyName = "—";
                keyColor = UiTheme.TEXT_DIM;
            } else {
                keyName = keyDisplayName(code);
                keyColor = UiTheme.ACCENT_2;
            }
            int chipW = Math.max(26, font.width(keyName) + 12);
            int chipX = listX() + listW() - chipW - 7;
            UiTheme.chip(g, chipX, y + 4, chipW, ROW_H - 8);
            g.drawCenteredString(font, Component.literal(keyName), chipX + chipW / 2, y + 9, keyColor);
            y += ROW_H;
        }

        int fy = py + ph - FOOTER + 8;
        hoverBtn = -1;
        if (UiTheme.inRect(mouseX, mouseY, px + 10, fy - 3, 92, 18)) hoverBtn = 0;
        if (UiTheme.inRect(mouseX, mouseY, px + 108, fy - 3, 92, 18)) hoverBtn = 1;
        footBtn(g, 0, Component.translatable("holyswap.screen.keys.reset").getString(), px + 10, fy, 92);
        footBtn(g, 1, Component.translatable("holyswap.screen.back").getString(), px + 108, fy, 92);
    }

    private void footBtn(GuiGraphics g, int id, String label, int x, int y, int w) {
        boolean hover = hoverBtn == id;
        UiTheme.button(g, x, y, w, 18);
        g.drawCenteredString(font, Component.literal(label), x + w / 2, y + 5,
                hover ? UiTheme.TEXT : UiTheme.TEXT_DIM);
    }

    private static String keyDisplayName(int code) {
        if (code == GLFW.GLFW_KEY_UNKNOWN) return "—";
        String s = GLFW.glfwGetKeyName(code, 0);
        return (s == null || s.isEmpty()) ? "K" + code : s.toUpperCase();
    }

    private boolean rowRect(double mx, double my, int idx) {
        return UiTheme.inRect(mx, my, listX(), listY() + (idx - scroll) * ROW_H, listW(), ROW_H);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        double mx = event.x(), my = event.y();
        if (event.button() == 0) {
            if (listening >= 0) {
                listening = -1;
                return true;
            }
            List<Act> acts = actions();
            for (int i = scroll; i < acts.size(); i++) {
                if (rowRect(mx, my, i) && i < scroll + listH() / ROW_H) {
                    listening = i;
                    return true;
                }
            }
            if (hoverBtn == 0) {
                for (Act act : acts) {
                    if (act.id().startsWith(SwapConfig.ACT_ITEM_PREFIX)) {
                        config.keys.remove(act.id());
                    } else {
                        config.setKey(act.id(), HolySwapClient.defKey(act.id()));
                    }
                }
                return true;
            }
            if (hoverBtn == 1) { onClose(); return true; }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (listening >= 0) {
            if (event.key() != GLFW.GLFW_KEY_ESCAPE) {
                List<Act> acts = actions();
                if (listening < acts.size()) {
                    config.setKey(acts.get(listening).id(), event.key());
                }
            }
            listening = -1;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll = Mth.clamp(scroll - (int) vertical, 0, maxScroll());
        return true;
    }

    @Override
    public void onClose() {
        config.save();
        minecraft.setScreen(new SelectorScreen(config));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
