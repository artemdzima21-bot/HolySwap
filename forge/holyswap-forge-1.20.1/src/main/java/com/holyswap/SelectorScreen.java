package com.holyswap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран-селектор: предметы по категориям, клик — добавить/убрать из цикла свапа.
 * Порт под 1.21.11 (Mojang-имена).
 */
public class SelectorScreen extends Screen {
    private final SwapConfig config;
    private final List<SwapLogic.Row> rows = new ArrayList<>();
    private int scroll = 0;
    private static final int ROWS = 8;

    public SelectorScreen(SwapConfig config) {
        super(Component.translatable("holyswap.screen.selector.title"));
        this.config = config;
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
            Button b = Button.builder(Component.literal(display), btn -> {
                                                if (!config.targets.remove(match)) {
                    config.targets.add(match);
                }
                config.save();
                rebuildButtons();
                            })
                    .bounds(this.width / 2 - 180, y, 360, 20)
                    .tooltip(Tooltip.create(Component.translatable(selected
                            ? "holyswap.screen.selector.tip_remove" : "holyswap.screen.selector.tip_add")))
                    .build();
            addRenderableWidget(b);
            y += 22;
        }
        addRenderableWidget(Button.builder(Component.translatable("holyswap.screen.keys_button"),
                        btn -> minecraft.setScreen(new KeysScreen(config)))
                .bounds(this.width / 2 - 180, this.height - 30, 120, 20)
                .tooltip(Tooltip.create(Component.translatable("holyswap.screen.selector.keys_tooltip")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("holyswap.screen.close"), btn -> onClose())
                .bounds(this.width / 2 + 60, this.height - 30, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredString(font, title, this.width / 2, 15, 0xFFFFAA00);
        ctx.drawCenteredString(font, Component.translatable("holyswap.screen.selector.hint", config.targets.size()),
                this.width / 2, 27, 0xAAAAAA);
        ctx.drawCenteredString(font, Component.translatable("holyswap.screen.selector.keyline",
                        keyOf(SwapConfig.ACT_TALISMAN), keyOf(SwapConfig.ACT_SPHERE),
                        keyOf(SwapConfig.ACT_TALISMAN_PLUS), keyOf(SwapConfig.ACT_TOTEM)),
                this.width / 2, 37, 0xAAAAAA);
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
