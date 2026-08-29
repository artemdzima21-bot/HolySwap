package com.holyswap;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран назначения клавиш: клик по строке -> нажми любую клавишу -> она привязывается.
 * Порт под 1.21.11 (Mojang-имена, KeyEvent вместо трёх int).
 */
public class KeysScreen extends Screen {
    private record Act(String id, String titleKey) {}
    private static final List<Act> ACTIONS = List.of(
            new Act(SwapConfig.ACT_TALISMAN, "holyswap.category.talisman"),
            new Act(SwapConfig.ACT_SPHERE, "holyswap.category.sphere"),
            new Act(SwapConfig.ACT_TALISMAN_PLUS, "holyswap.category.talisman_plus"),
            new Act(SwapConfig.ACT_TOTEM, "holyswap.category.totem"),
            new Act(SwapConfig.ACT_SELECTOR, "holyswap.action.selector")
    );

    private final SwapConfig config;
    private int listening = -1;

    public KeysScreen(SwapConfig config) {
        super(Component.translatable("holyswap.screen.keys.title"));
        this.config = config;
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int y = 45;
        for (int i = 0; i < ACTIONS.size(); i++) {
            final int idx = i;
            Act act = ACTIONS.get(i);
            int code = config.keyFor(act.id, HolySwapForge.defKey(act.id));
            String keyName = listening == idx ? "§e" + Component.translatable("holyswap.screen.keys.listening").getString()
                                              : SwapLogic.keyName(code, "—");
            String shared = "";
            if (listening != idx && code != GLFW.GLFW_KEY_UNKNOWN) {
                List<String> others = new ArrayList<>();
                for (int j = 0; j < ACTIONS.size(); j++) {
                    if (j != idx && config.keyFor(ACTIONS.get(j).id, HolySwapForge.defKey(ACTIONS.get(j).id)) == code) {
                        others.add(Component.translatable(ACTIONS.get(j).titleKey).getString());
                    }
                }
                if (!others.isEmpty()) {
                    shared = " §8(" + Component.translatable("holyswap.screen.keys.shared",
                            String.join(", ", others)).getString() + ")";
                }
            }
            final String display = Component.translatable(act.titleKey).getString() + ": §b" + keyName + shared;
            Button b = Button.builder(Component.literal(display), btn -> {
                                listening = (listening == idx ? -1 : idx);
                                rebuild();
                            })
                    .bounds(this.width / 2 - 130, y, 260, 20)
                    .build();
            addRenderableWidget(b);
            y += 24;
        }
        addRenderableWidget(Button.builder(Component.translatable("holyswap.screen.keys.reset"), btn -> {
                    for (Act act : ACTIONS) config.setKey(act.id, HolySwapForge.defKey(act.id));
                    rebuild();
                })
                .bounds(this.width / 2 - 130, y + 8, 260, 20)
                .tooltip(Tooltip.create(Component.translatable("holyswap.screen.keys.reset_tooltip")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("holyswap.screen.back"), btn -> onClose())
                .bounds(this.width / 2 - 60, this.height - 30, 120, 20).build());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listening >= 0) {
            if (event.key() != GLFW.GLFW_KEY_ESCAPE) {
                Act act = ACTIONS.get(listening);
                config.setKey(act.id, event.key());
            }
            listening = -1;
            rebuild();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredString(font, title, this.width / 2, 15, 0xFFFFAA00);
        ctx.drawCenteredString(font, Component.translatable("holyswap.screen.keys.hint"), this.width / 2, 28, 0xAAAAAA);
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
