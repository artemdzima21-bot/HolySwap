package com.holyswap;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Экран назначения клавиш: клик по строке -> нажми любую клавишу -> она привязывается.
 * Escape во время прослушивания отменяет выбор.
 */
public class KeysScreen extends Screen {
    /** Действие (id + ключ перевода названия). */
    private static final class Act {
        final String id;
        final String titleKey;

        Act(String id, String titleKey) {
            this.id = id;
            this.titleKey = titleKey;
        }
    }

    private static final List<Act> ACTIONS = Arrays.asList(
            new Act(SwapConfig.ACT_TALISMAN, "holyswap.category.talisman"),
            new Act(SwapConfig.ACT_SPHERE, "holyswap.category.sphere"),
            new Act(SwapConfig.ACT_TALISMAN_PLUS, "holyswap.category.talisman_plus"),
            new Act(SwapConfig.ACT_TOTEM, "holyswap.category.totem"),
            new Act(SwapConfig.ACT_SELECTOR, "holyswap.action.selector")
    );

    private final SwapConfig config;
    private int listening = -1;

    public KeysScreen(SwapConfig config) {
        super(new TranslatableText("holyswap.screen.keys.title"));
        this.config = config;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        this.children.clear();
        this.buttons.clear();
        int y = 45;
        for (int i = 0; i < ACTIONS.size(); i++) {
            final int idx = i;
            Act act = ACTIONS.get(i);
            int code = config.keyFor(act.id, HolySwapClient.defKey(act.id));
            String keyName = listening == idx ? "§e" + new TranslatableText("holyswap.screen.keys.listening").getString()
                                              : keyName(code);
            // если клавиша общая с другим действием — покажем, что они объединятся в один цикл
            String shared = "";
            if (listening != idx && code != GLFW.GLFW_KEY_UNKNOWN) {
                List<String> others = new ArrayList<>();
                for (int j = 0; j < ACTIONS.size(); j++) {
                    if (j != idx && config.keyFor(ACTIONS.get(j).id, HolySwapClient.defKey(ACTIONS.get(j).id)) == code) {
                        others.add(new TranslatableText(ACTIONS.get(j).titleKey).getString());
                    }
                }
                if (!others.isEmpty()) {
                    shared = " §8(" + new TranslatableText("holyswap.screen.keys.shared",
                            String.join(", ", others)).getString() + ")";
                }
            }
            ButtonWidget b = new ButtonWidget(
                    this.width / 2 - 130, y, 260, 20,
                    new LiteralText(new TranslatableText(act.titleKey).getString() + ": §b" + keyName + shared),
                    btn -> {
                        listening = (listening == idx ? -1 : idx);
                        rebuild();
                    });
            addButton(b);
            y += 24;
        }
        addButton(new ButtonWidget(this.width / 2 - 130, y + 8, 260, 20,
                new TranslatableText("holyswap.screen.keys.reset"), btn -> {
                    for (Act act : ACTIONS) config.setKey(act.id, HolySwapClient.defKey(act.id));
                    rebuild();
                }));
        addButton(new ButtonWidget(this.width / 2 - 60, this.height - 30, 120, 20,
                new TranslatableText("holyswap.screen.back"), btn -> close()));
    }

    private static String keyName(int code) {
        if (code == GLFW.GLFW_KEY_UNKNOWN) return "—";
        String s = InputUtil.fromKeyCode(code, 0).getLocalizedText().getString();
        return s.isEmpty() ? new TranslatableText("holyswap.screen.keys.unknown", code).getString()
                           : s;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening >= 0) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                Act act = ACTIONS.get(listening);
                config.setKey(act.id, keyCode);
            }
            listening = -1;
            rebuild();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        drawCenteredText(matrices, textRenderer, title.getString(), this.width / 2, 15, 0xFFFFAA00);
        drawCenteredText(matrices, textRenderer,
                new TranslatableText("holyswap.screen.keys.hint").getString(), this.width / 2, 28, 0xAAAAAA);
    }

    public void close() {
        config.save();
        client.openScreen(new SelectorScreen(config));
    }

        public boolean shouldPause() {
        return false;
    }
}
