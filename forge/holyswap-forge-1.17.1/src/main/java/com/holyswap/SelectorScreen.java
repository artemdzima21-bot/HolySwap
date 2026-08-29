package com.holyswap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.ArrayList;
import java.util.List;

/** Экран-селектор: предметы по категориям, клик — добавить/убрать из цикла свапа. Порт под 1.17/1.17.1. */
public class SelectorScreen extends Screen {
    private final SwapConfig config;
    private final List<SwapLogic.Row> rows = new ArrayList<>();
    private int scroll = 0;
    private static final int ROWS = 8;

    public SelectorScreen(SwapConfig config) {
        super(new TranslatableComponent("holyswap.screen.selector.title"));
        this.config = config;
    }

    static Button button(int x, int y, int w, int h, Component msg, Button.OnPress onPress, Component tooltip) {
        return new Button(x, y, w, h, msg, onPress, (btn, ms, mx, my) ->
                Minecraft.getInstance().screen.renderTooltip(ms, tooltip, mx, my));
    }

    static Button button(int x, int y, int w, int h, Component msg, Button.OnPress onPress) {
        return new Button(x, y, w, h, msg, onPress);
    }

    @Override
    protected void init() {
        rows.clear();
        if (minecraft.player != null) {
            rows.addAll(SwapLogic.listDistinct(minecraft.player));
        }
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        int y = 52;
        SwapLogic.Category lastCat = null;
        for (int i = scroll; i < Math.min(rows.size(), scroll + ROWS); i++) {
            final SwapLogic.Row row = rows.get(i);
            final String match = row.label().contains(" (")
                    ? row.label().substring(0, row.label().lastIndexOf(" (")) : row.label();
            boolean selected = config.targets.contains(match);
            String catPrefix;
            if (row.category() != lastCat) {
                lastCat = row.category();
                catPrefix = lastCat.colorTag + "[" + lastCat.title() + "]§r ";
            } else {
                catPrefix = "            ";
            }
            String prefix = selected ? "§a✔ " : "§8   ";
            final String display = catPrefix + prefix + row.label() + " §8x" + row.count();
            final String displayOff = catPrefix + "§8   " + row.label() + " §8x" + row.count();
            Button b = button(this.width / 2 - 180, y, 360, 20, new TextComponent(display), btn -> {
                                if (!config.targets.remove(match)) {
                    config.targets.add(match);
                }
                config.save();
                rebuildButtons();
            }, new TranslatableComponent(selected
                    ? "holyswap.screen.selector.tip_remove" : "holyswap.screen.selector.tip_add"));
            addRenderableWidget(b);
            y += 22;
        }
        addRenderableWidget(button(this.width / 2 - 180, this.height - 30, 120, 20,
                new TranslatableComponent("holyswap.screen.keys_button"),
                btn -> minecraft.setScreen(new KeysScreen(config)),
                new TranslatableComponent("holyswap.screen.selector.keys_tooltip")));
        addRenderableWidget(button(this.width / 2 + 60, this.height - 30, 120, 20,
                new TranslatableComponent("holyswap.screen.close"), btn -> onClose()));
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float delta) {
        renderBackground(pose);
        drawCenteredString(pose, font, title, this.width / 2, 15, 0xFFFFAA00);
        drawCenteredString(pose, font, new TranslatableComponent("holyswap.screen.selector.hint", config.targets.size()),
                this.width / 2, 27, 0xAAAAAA);
        drawCenteredString(pose, font, new TranslatableComponent("holyswap.screen.selector.keyline",
                        keyOf(SwapConfig.ACT_TALISMAN), keyOf(SwapConfig.ACT_SPHERE),
                        keyOf(SwapConfig.ACT_TALISMAN_PLUS), keyOf(SwapConfig.ACT_TOTEM)),
                this.width / 2, 37, 0xAAAAAA);
        super.render(pose, mouseX, mouseY, delta);
    }

    private String keyOf(String action) {
        return SwapLogic.keyName(config.keyFor(action, HolySwapForge.defKey(action)), "—");
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        int max = Math.max(0, rows.size() - ROWS);
        int old = scroll;
        scroll = Math.max(0, Math.min(max, scroll - (int) vertical));
        if (scroll != old) rebuildButtons();
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
