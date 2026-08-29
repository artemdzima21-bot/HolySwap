package com.holyswap;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HolySwap для Forge 1.19.3. Та же логика, что и Fabric-версия:
 * GLFW-поллинг клавиш с edge-detection, свап = ClickType.SWAP кнопка 40.
 */
@Mod("holyswap")
public class HolySwapForge {
    public static final SwapConfig CONFIG = SwapConfig.load("1.19.3");

    public static int defKey(String action) {
        if (SwapConfig.ACT_TALISMAN.equals(action)) return GLFW.GLFW_KEY_G;
        if (SwapConfig.ACT_SPHERE.equals(action)) return GLFW.GLFW_KEY_R;
        if (SwapConfig.ACT_TALISMAN_PLUS.equals(action)) return GLFW.GLFW_KEY_V;
        if (SwapConfig.ACT_TOTEM.equals(action)) return GLFW.GLFW_KEY_B;
        if (SwapConfig.ACT_SELECTOR.equals(action)) return GLFW.GLFW_KEY_H;
        return GLFW.GLFW_KEY_UNKNOWN;
    }

    public static SwapLogic.Category categoryOf(String action) {
        if (SwapConfig.ACT_TALISMAN.equals(action)) return SwapLogic.Category.TALISMAN;
        if (SwapConfig.ACT_SPHERE.equals(action)) return SwapLogic.Category.SPHERE;
        if (SwapConfig.ACT_TALISMAN_PLUS.equals(action)) return SwapLogic.Category.TALISMAN_PLUS;
        if (SwapConfig.ACT_TOTEM.equals(action)) return SwapLogic.Category.TOTEM;
        return null;
    }

    private static final Set<Integer> heldKeys = new HashSet<>();

    public HolySwapForge() {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.screen != null) {
            heldKeys.clear();
            return;
        }
        long window = client.getWindow().getWindow();
        Map<Integer, List<String>> groups = new HashMap<>();
        for (String act : List.of(SwapConfig.ACT_TALISMAN, SwapConfig.ACT_SPHERE,
                SwapConfig.ACT_TALISMAN_PLUS, SwapConfig.ACT_TOTEM, SwapConfig.ACT_SELECTOR)) {
            int code = CONFIG.keyFor(act, defKey(act));
            if (code == GLFW.GLFW_KEY_UNKNOWN) continue;
            List<String> g = groups.get(code);
            if (g == null) { g = new ArrayList<>(); groups.put(code, g); }
            g.add(act);
        }
        for (Map.Entry<Integer, List<String>> e : groups.entrySet()) {
            boolean down = GLFW.glfwGetKey(window, e.getKey()) == GLFW.GLFW_PRESS;
            if (down && heldKeys.add(e.getKey())) {
                run(client, e.getValue());
            } else if (!down) {
                heldKeys.remove(e.getKey());
            }
        }
    }

    private static void run(Minecraft client, List<String> actions) {
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
