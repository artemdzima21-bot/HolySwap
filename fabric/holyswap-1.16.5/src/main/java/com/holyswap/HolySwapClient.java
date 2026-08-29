package com.holyswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Действия группируются по клавише: одна кнопка — одно или несколько действий.
 * Если на кнопке несколько категорий, нажатие листает их по кругу общий цикл
 * (талисман → сфера → талисман...). Опрос клавиатуры напрямую через GLFW
 * с edge-detection — бинды меняются на лету из экрана «Клавиши».
 */
public class HolySwapClient implements ClientModInitializer {
    public static final SwapConfig CONFIG = SwapConfig.load();

    public static int defKey(String action) {
        if (SwapConfig.ACT_TALISMAN.equals(action)) return GLFW.GLFW_KEY_G;
        if (SwapConfig.ACT_SPHERE.equals(action)) return GLFW.GLFW_KEY_R;
        if (SwapConfig.ACT_TALISMAN_PLUS.equals(action)) return GLFW.GLFW_KEY_V;
        if (SwapConfig.ACT_TOTEM.equals(action)) return GLFW.GLFW_KEY_B;
        if (SwapConfig.ACT_SELECTOR.equals(action)) return GLFW.GLFW_KEY_H;
        return GLFW.GLFW_KEY_UNKNOWN;
    }

    /** Порядок категорий в общем цикле совпадает с порядком действий. */
    public static SwapLogic.Category categoryOf(String action) {
        if (SwapConfig.ACT_TALISMAN.equals(action)) return SwapLogic.Category.TALISMAN;
        if (SwapConfig.ACT_SPHERE.equals(action)) return SwapLogic.Category.SPHERE;
        if (SwapConfig.ACT_TALISMAN_PLUS.equals(action)) return SwapLogic.Category.TALISMAN_PLUS;
        if (SwapConfig.ACT_TOTEM.equals(action)) return SwapLogic.Category.TOTEM;
        return null;
    }

    private static final Set<Integer> heldKeys = new HashSet<>();

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null) {
                heldKeys.clear();
                return;
            }
            long window = client.getWindow().getHandle();
            // собираем группы: код клавиши -> действия
            Map<Integer, List<String>> groups = new HashMap<>();
            for (String act : Arrays.asList(SwapConfig.ACT_TALISMAN, SwapConfig.ACT_SPHERE,
                    SwapConfig.ACT_TALISMAN_PLUS, SwapConfig.ACT_TOTEM, SwapConfig.ACT_SELECTOR)) {
                int code = CONFIG.keyFor(act, defKey(act));
                if (code == GLFW.GLFW_KEY_UNKNOWN) continue;
                if (!groups.containsKey(code)) groups.put(code, new ArrayList<String>());
                groups.get(code).add(act);
            }
            for (Map.Entry<Integer, List<String>> e : groups.entrySet()) {
                boolean down = InputUtil.isKeyPressed(window, e.getKey());
                if (down && heldKeys.add(e.getKey())) { // только на нажатие, не на автоповтор
                    run(client, e.getValue());
                } else if (!down) {
                    heldKeys.remove(e.getKey());
                }
            }
        });
    }

    private static void run(MinecraftClient client, List<String> actions) {
        if (actions.contains(SwapConfig.ACT_SELECTOR)) {
            client.openScreen(new SelectorScreen(CONFIG));
            return;
        }
        List<SwapLogic.Category> cats = new ArrayList<>();
        for (String act : actions) {
            SwapLogic.Category c = categoryOf(act);
            if (c != null) cats.add(c);
        }
        if (!cats.isEmpty()) {
            SwapLogic.cycleSwap(client, CONFIG, cats);
        }
    }
}
