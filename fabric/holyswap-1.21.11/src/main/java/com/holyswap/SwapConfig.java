package com.holyswap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Список "целей" свапа и назначенные клавиши.
 * Цель — точное отображаемое имя предмета ("Талисман Воромана");
 * клавиша — GLFW KEYSYM-код, действие -> код.
 */
public class SwapConfig {
    public List<String> targets = new ArrayList<>();
    public Map<String, Integer> keys = new LinkedHashMap<>();

    public static final String ACT_TALISMAN = "talisman";
    public static final String ACT_SPHERE = "sphere";
    public static final String ACT_SPHERE_PLUS = "sphere_plus";
    public static final String ACT_TALISMAN_PLUS = "talisman_plus";
    public static final String ACT_TOTEM = "totem";
    public static final String ACT_SELECTOR = "selector";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // У каждой версии Minecraft свой конфиг — новые версии стартуют с заводских настроек.
    private static final Path PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("holyswap-" + FabricLoader.getInstance().getRawGameVersion() + ".json");

    public int keyFor(String action, int def) {
        return keys.getOrDefault(action, def);
    }

    public void setKey(String action, int code) {
        keys.put(action, code);
        save();
    }

    public static SwapConfig load() {
        try {
            if (Files.exists(PATH)) {
                SwapConfig c = GSON.fromJson(Files.readString(PATH), SwapConfig.class);
                if (c != null) {
                    if (c.targets == null) c.targets = new ArrayList<>();
                    if (c.keys == null) c.keys = new LinkedHashMap<>();
                    return c;
                }
            }
        } catch (IOException ignored) {
        }
        return new SwapConfig();
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(this));
        } catch (IOException ignored) {
        }
    }
}
