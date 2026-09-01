package com.holyswap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран назначения клавиш в «клиентском» стиле: клик по строке -> нажми клавишу.
 * Каждому предмету из инвентаря — своя строка. Escape отменяет выбор.
 */
public class KeysScreen extends Screen {
    private static final int PANEL_W = 320;
    private static final int ROW_H = 22;
    private static final int HEADER = 52;
    private static final int FOOTER = 34;

    private record Act(String id, String label) {}

    /** Фиксированные действия + отдельная строка на каждый предмет в инвентаре. */
    private List<Act> actions() {
        List<Act> out = new ArrayList<>(List.of(
                new Act(SwapConfig.ACT_TALISMAN, Component.translatable("holyswap.category.talisman").getString()),
                new Act(SwapConfig.ACT_SPHERE, Component.translatable("holyswap.category.sphere").getString()),
                new Act(SwapConfig.ACT_SPHERE_PLUS, Component.translatable("holyswap.category.sphere_plus").getString()),
                new Act(SwapConfig.ACT_TALISMAN_PLUS, Component.translatable("holyswap.category.talisman_plus").getString()),
                new Act(SwapConfig.ACT_TOTEM, Component.translatable("holyswap.category.totem").getString()),
                new Act(SwapConfig.ACT_SELECTOR, Component.translatable("holyswap.action.selector").getString())
        ));
        if (minecraft != null && minecraft.player != null) {
            for (SwapLogic.Row row : SwapLogic.listDistinct(minecraft.player)) {
                String match = row.label().contains(" (")
                        ? row.label().substring(0, row.label().lastIndexOf(" (")) : row.label();
                out.add(new Act(SwapConfig.ACT_ITEM_PREFIX + match, match));
            }
        }
        return out;
    }

    private final SwapConfig config;
    private int listening = -1;
    private int hoveredRow = -1;
    private boolean hoveringReset = false, hoveringBack = false;

    public KeysScreen(SwapConfig config) {
        super(Component.translatable("holyswap.screen.keys.title"));
        this.config = config;
    }

    private int panelX() { return (this.width - PANEL_W) / 2; }
    private int panelY() { return Math.max(10, this.height / 2 - 110); }
    private int panelH() { return Math.min(this.height - 2 * panelY(), HEADER + FOOTER + ROW_H * 9); }
    private int listX()  { return panelX() + 10; }
    private int listW()  { return PANEL_W - 20; }
    private int listY()  { return panelY() + HEADER; }
    private int listH()  { return panelH() - HEADER - FOOTER; }
    private int maxScroll() { return Math.max(0, actions().size() - listH() / ROW_H); }

    @Override
    protected void init() {
        scroll = 0;
    }

    private int scroll = 0;

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int px = panelX(), py = panelY(), pw = PANEL_W, ph = panelH();
        g.fill(0, 0, this.width, this.height, UiTheme.BG);
        UiTheme.panel(g, px, py, pw, ph);
        for (int i = 0; i < pw; i++) {
            g.fill(px + i, py, px + i + 1, py + 2, UiTheme.lerpColor(UiTheme.ACCENT_2, UiTheme.ACCENT, i / (float) pw));
        }

        String logo = Component.translatable("holyswap.screen.keys.title").getString();
        int logoW = font.width(logo);
        UiTheme.gradientTitle(g, font, logo, px + (pw - logoW) / 2f, py + 10);
        String hint = Component.translatable("holyswap.screen.keys.hint").getString();
        g.drawCenteredString(font, Component.literal(hint), px + pw / 2, py + 24, UiTheme.TEXT_DIM);
        g.fill(px + 10, py + HEADER - 4, px + pw - 10, py + HEADER - 3, 0x307C4DFF);

        List<Act> acts = actions();
        hoveredRow = -1;
        int y = listY();
        for (int i = scroll; i < acts.size(); i++) {
            if (y + ROW_H > listY() + listH()) break;
            Act act = acts.get(i);
            boolean hover = UiTheme.inRect(mouseX, mouseY, listX(), y, listW(), ROW_H);
            if (hover) hoveredRow = i;
            boolean isListening = listening == i;
            if (isListening) {
                g.fill(listX(), y, listX() + listW(), y + ROW_H, 0x337C4DFF);
            } else if (hover) {
                g.fill(listX(), y, listX() + listW(), y + ROW_H, UiTheme.ROW_HOVER);
            } else if ((i - scroll) % 2 == 0) {
                g.fill(listX(), y, listX() + listW(), y + ROW_H, UiTheme.PANEL_2);
            }
            if (isListening) g.fill(listX(), y, listX() + 2, y + ROW_H, UiTheme.ACCENT_2);

            String label = UiTheme.ellipsize(font, act.label(), listW() - 110);
            g.drawString(font, label, listX() + 8, y + 7, UiTheme.TEXT);

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
                keyName = SwapLogic.keyName(code, "—");
                keyColor = UiTheme.ACCENT;
            }
            int chipW = Math.max(24, font.width(keyName) + 10);
            int chipX = listX() + listW() - chipW - 6;
            g.fill(chipX, y + 3, chipX + chipW, y + ROW_H - 3, UiTheme.PANEL_2);
            UiTheme.border(g, chipX, y + 3, chipW, ROW_H - 6, isListening ? UiTheme.ACCENT_2 : 0x407C4DFF);
            g.drawCenteredString(font, Component.literal(keyName), chipX + chipW / 2, y + 7, keyColor);
            y += ROW_H;
        }

        int fy = py + ph - FOOTER + 8;
        drawFooterButton(g, Component.translatable("holyswap.screen.keys.reset").getString(),
                px + 10, fy, 86, hoveringReset);
        drawFooterButton(g, Component.translatable("holyswap.screen.back").getString(),
                px + 102, fy, 86, hoveringBack);
    }

    private void drawFooterButton(GuiGraphics g, String label, int x, int y, int w, boolean hover) {
        g.fill(x, y - 3, x + w, y + 15, hover ? UiTheme.ROW_HOVER : UiTheme.PANEL_2);
        UiTheme.border(g, x, y - 3, w, 18, hover ? UiTheme.ACCENT : 0x407C4DFF);
        g.drawCenteredString(font, Component.literal(label), x + w / 2, y + 2, UiTheme.TEXT);
    }

    private boolean rowRect(double mx, double my, int idx) {
        return UiTheme.inRect(mx, my, listX(), listY() + (idx - scroll) * ROW_H, listW(), ROW_H);
    }
    private boolean footerResetRect(double mx, double my) {
        return UiTheme.inRect(mx, my, panelX() + 10, panelY() + panelH() - FOOTER + 5, 86, 18);
    }
    private boolean footerBackRect(double mx, double my) {
        return UiTheme.inRect(mx, my, panelX() + 102, panelY() + panelH() - FOOTER + 5, 86, 18);
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
            if (footerResetRect(mx, my)) {
                for (Act act : acts) {
                    if (act.id().startsWith(SwapConfig.ACT_ITEM_PREFIX)) {
                        config.keys.remove(act.id());
                    } else {
                        config.setKey(act.id(), HolySwapClient.defKey(act.id()));
                    }
                }
                return true;
            }
            if (footerBackRect(mx, my)) { onClose(); return true; }
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
