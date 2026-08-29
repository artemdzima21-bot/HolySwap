package com.holyswap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Список целей свапа и назначенные клавиши. У каждой версии Minecraft
 * свой конфиг — «заводские настройки» на каждую версию.
 */
public class SwapConfig {
    public List<String> targets = new ArrayList<>();
    public Map<String, Integer> keys = new LinkedHashMap<>();

    public static final String ACT_TALISMAN = "talisman";
    public static final String ACT_SPHERE = "sphere";
    public static final String ACT_TALISMAN_PLUS = "talisman_plus";
    public static final String ACT_TOTEM = "totem";
    public static final String ACT_SELECTOR = "selector";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private transient Path path;

    public SwapConfig(String mcVersion) {
        this.path = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get()
                .resolve("holyswap-" + mcVersion + ".json");
    }

    public int keyFor(String action, int def) {
        return keys.getOrDefault(action, def);
    }

    public void setKey(String action, int code) {
        keys.put(action, code);
        save();
    }

    public static SwapConfig load(String mcVersion) {
        SwapConfig c = new SwapConfig(mcVersion);
        try {
            if (Files.exists(c.path)) {
                SwapConfig loaded = GSON.fromJson(new String(Files.readAllBytes(c.path), "UTF-8"), SwapConfig.class);
                if (loaded != null) {
                    if (loaded.targets == null) loaded.targets = new ArrayList<>();
                    if (loaded.keys == null) loaded.keys = new LinkedHashMap<>();
                    loaded.path = c.path;
                    return loaded;
                }
            }
        } catch (IOException ignored) {
        }
        return c;
    }

    public void save() {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, GSON.toJson(this).getBytes("UTF-8"));
        } catch (IOException ignored) {
        }
    }
}
