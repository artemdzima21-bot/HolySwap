package com.holyswap;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Экран назначения клавиш: клик по строке -> нажми любую клавишу -> она привязывается. Порт под 1.17/1.17.1. */
public class KeysScreen extends Screen {
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
        super(new TranslatableComponent("holyswap.screen.keys.title"));
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
            String keyName = listening == idx ? "§e" + new TranslatableComponent("holyswap.screen.keys.listening").getString()
                                              : SwapLogic.keyName(code, "—");
            String shared = "";
            if (listening != idx && code != GLFW.GLFW_KEY_UNKNOWN) {
                List<String> others = new ArrayList<>();
                for (int j = 0; j < ACTIONS.size(); j++) {
                    if (j != idx && config.keyFor(ACTIONS.get(j).id, HolySwapForge.defKey(ACTIONS.get(j).id)) == code) {
                        others.add(new TranslatableComponent(ACTIONS.get(j).titleKey).getString());
                    }
                }
                if (!others.isEmpty()) {
                    shared = " §8(" + new TranslatableComponent("holyswap.screen.keys.shared",
                            String.join(", ", others)).getString() + ")";
                }
            }
            final String display = new TranslatableComponent(act.titleKey).getString() + ": §b" + keyName + shared;
            Button b = new Button(this.width / 2 - 130, y, 260, 20, new TextComponent(display), btn -> {
                listening = (listening == idx ? -1 : idx);
                rebuild();
            });
            addRenderableWidget(b);
            y += 24;
        }
        Button resetBtn = new Button(this.width / 2 - 130, y + 8, 260, 20,
                new TranslatableComponent("holyswap.screen.keys.reset"), btn -> {
            for (Act act : ACTIONS) config.setKey(act.id, HolySwapForge.defKey(act.id));
            rebuild();
        });
        addRenderableWidget(resetBtn);
        addRenderableWidget(new Button(this.width / 2 - 60, this.height - 30, 120, 20,
                new TranslatableComponent("holyswap.screen.back"), btn -> onClose()));
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
    public void render(PoseStack pose, int mouseX, int mouseY, float delta) {
        renderBackground(pose);
        drawCenteredString(pose, font, title, this.width / 2, 15, 0xFFFFAA00);
        drawCenteredString(pose, font, new TranslatableComponent("holyswap.screen.keys.hint"), this.width / 2, 28, 0xAAAAAA);
        super.render(pose, mouseX, mouseY, delta);
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
