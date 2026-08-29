package com.holyswap;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран-селектор: показывает предметы по категориям (талисманы, талисманы+, сферы, тотемы).
 * Клик — добавить/убрать предмет из списка свапа. Выбор сохраняется в конфиг.
 */
public class SelectorScreen extends Screen {
    private final SwapConfig config;
    private final List<SwapLogic.Row> rows = new ArrayList<>();
    private int scroll = 0;
    private static final int ROWS = 8;

    public SelectorScreen(SwapConfig config) {
        super(new TranslatableText("holyswap.screen.selector.title"));
        this.config = config;
    }

    @Override
    protected void init() {
        rows.clear();
        if (client.player != null) {
            rows.addAll(SwapLogic.listDistinct(client.player));
        }
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearChildren();
        int y = 52;
        SwapLogic.Category lastCat = null;
        for (int i = scroll; i < Math.min(rows.size(), scroll + ROWS); i++) {
            final SwapLogic.Row row = rows.get(i);
            // матчим по точному имени (часть до скобки)
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
            ButtonWidget b = new ButtonWidget(
                    this.width / 2 - 180, y, 360, 20,
                    new LiteralText(catPrefix + prefix + row.label() + " §8x" + row.count()),
                    btn -> {
                        if (config.targets.remove(match)) {
                            btn.setMessage(new LiteralText(catPrefix + "§8   " + row.label() + " §8x" + row.count()));
                        } else {
                            config.targets.add(match);
                            btn.setMessage(new LiteralText(catPrefix + "§a✔ " + row.label() + " §8x" + row.count()));
                        }
                        config.save();
                    });
            addDrawableChild(b);
            y += 22;
        }
        addDrawableChild(new ButtonWidget(this.width / 2 - 180, this.height - 30, 120, 20,
                new TranslatableText("holyswap.screen.keys_button"),
                btn -> client.setScreen(new KeysScreen(config))));
        addDrawableChild(new ButtonWidget(this.width / 2 + 60, this.height - 30, 120, 20,
                new TranslatableText("holyswap.screen.close"), btn -> onClose()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        drawCenteredTextWithShadow(matrices, textRenderer, title.asOrderedText(), this.width / 2, 15, 0xFFFFAA00);
        Text hint = new TranslatableText("holyswap.screen.selector.hint", config.targets.size());
        Text keys = new TranslatableText("holyswap.screen.selector.keyline",
                keyOf(SwapConfig.ACT_TALISMAN), keyOf(SwapConfig.ACT_SPHERE),
                keyOf(SwapConfig.ACT_TALISMAN_PLUS), keyOf(SwapConfig.ACT_TOTEM));
        drawCenteredTextWithShadow(matrices, textRenderer, hint.asOrderedText(), this.width / 2, 27, 0xAAAAAA);
        drawCenteredTextWithShadow(matrices, textRenderer, keys.asOrderedText(), this.width / 2, 37, 0xAAAAAA);
    }

    private String keyOf(String action) {
        int code = config.keyFor(action, HolySwapClient.defKey(action));
        if (code == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) return "—";
        String s = net.minecraft.client.util.InputUtil.Type.KEYSYM.createFromCode(code).getLocalizedText().getString();
        return s.isEmpty() ? ("#" + code) : s.toUpperCase();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        int max = Math.max(0, rows.size() - ROWS);
        int old = scroll;
        scroll = Math.max(0, Math.min(max, scroll - (int) vertical));
        if (scroll != old) rebuildButtons();
        return true;
    }

    public void onClose() {
        config.save();
        client.setScreen(null);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
