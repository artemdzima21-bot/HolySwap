package com.holyswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
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
        return switch (action) {
            case SwapConfig.ACT_TALISMAN -> GLFW.GLFW_KEY_G;
            case SwapConfig.ACT_SPHERE -> GLFW.GLFW_KEY_R;
            case SwapConfig.ACT_SPHERE_PLUS -> GLFW.GLFW_KEY_T;
            case SwapConfig.ACT_TALISMAN_PLUS -> GLFW.GLFW_KEY_V;
            case SwapConfig.ACT_TOTEM -> GLFW.GLFW_KEY_B;
            case SwapConfig.ACT_SELECTOR -> GLFW.GLFW_KEY_H;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
    }

    /** Порядок категорий в общем цикле совпадает с порядком действий. */
    public static SwapLogic.Category categoryOf(String action) {
        return switch (action) {
            case SwapConfig.ACT_TALISMAN -> SwapLogic.Category.TALISMAN;
            case SwapConfig.ACT_SPHERE -> SwapLogic.Category.SPHERE;
            case SwapConfig.ACT_SPHERE_PLUS -> SwapLogic.Category.SPHERE_PLUS;
            case SwapConfig.ACT_TALISMAN_PLUS -> SwapLogic.Category.TALISMAN_PLUS;
            case SwapConfig.ACT_TOTEM -> SwapLogic.Category.TOTEM;
            default -> null;
        };
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
            for (String act : List.of(SwapConfig.ACT_TALISMAN, SwapConfig.ACT_SPHERE,
                    SwapConfig.ACT_SPHERE_PLUS, SwapConfig.ACT_TALISMAN_PLUS,
                    SwapConfig.ACT_TOTEM, SwapConfig.ACT_SELECTOR)) {
                int code = CONFIG.keyFor(act, defKey(act));
                if (code == GLFW.GLFW_KEY_UNKNOWN) continue;
                groups.computeIfAbsent(code, k -> new ArrayList<>()).add(act);
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
            client.setScreen(new SelectorScreen(CONFIG));
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
