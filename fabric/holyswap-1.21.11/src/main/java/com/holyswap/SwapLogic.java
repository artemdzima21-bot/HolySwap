package com.holyswap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Свап эмулирует ровно то, что делает ванильная клавиша F:
 * handleInventoryMouseClick c ClickType.SWAP и кнопкой 40 (оффхенд) по inventoryMenu.
 * Для сервера это неотличимо от обычного игрока. Порт под 1.21.11 (Mojang-имена).
 */
public final class SwapLogic {

    public record Candidate(int handlerSlot, String label, Category category) {}

    public enum Category {
        TALISMAN("§a", "holyswap.category.talisman"),
        TALISMAN_PLUS("§e", "holyswap.category.talisman_plus"),
        SPHERE("§b", "holyswap.category.sphere"),
        SPHERE_PLUS("§9", "holyswap.category.sphere_plus"),
        TOTEM("§d", "holyswap.category.totem"),
        OTHER("§7", "holyswap.category.other");

        public final String colorTag;
        public final String titleKey;

        Category(String colorTag, String titleKey) {
            this.colorTag = colorTag;
            this.titleKey = titleKey;
        }

        public String title() {
            return Component.translatable(titleKey).getString();
        }
    }

    /** Тотем — только нетронутый totem_of_undying; ключевые слова — из конфига. */
    public static Category classify(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String name = stack.getHoverName().getString().toLowerCase();
        Map<String, List<String>> pat = HolySwapClient.CONFIG.patterns;
        boolean sphere = matches(name, pat.get("sphere"));
        boolean talisman = matches(name, pat.get("talisman"));
        if (id.equals("minecraft:totem_of_undying")) {
            String vanilla = Component.translatable("item.minecraft.totem_of_undying").getString().toLowerCase();
            if (!name.equals(vanilla) || (talisman || sphere)) {
                if (sphere) return name.contains("+") ? Category.SPHERE_PLUS : Category.SPHERE;
                return name.contains("+") ? Category.TALISMAN_PLUS : Category.TALISMAN;
            }
            return Category.TOTEM;
        }
        if (sphere) return name.contains("+") ? Category.SPHERE_PLUS : Category.SPHERE;
        if (talisman) return name.contains("+") ? Category.TALISMAN_PLUS : Category.TALISMAN;
        return Category.OTHER;
    }

    private static boolean matches(String name, List<String> keywords) {
        if (keywords == null) return false;
        for (String kw : keywords) {
            if (!kw.isEmpty() && name.contains(kw.toLowerCase())) return true;
        }
        return false;
    }


    public static List<Candidate> findMatching(LocalPlayer player, SwapConfig config, Category cat) {
        List<Candidate> out = new ArrayList<>();
        var menu = player.inventoryMenu;
        Map<String, Candidate> sorted = new TreeMap<>();
        for (int inv = 0; inv < 36; inv++) {
            int slot = inv < 9 ? 36 + inv : inv;
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty() || classify(stack) != cat) continue;
            if (!config.targets.isEmpty() && !config.targets.contains(stack.getHoverName().getString())) continue;
            sorted.put(describe(stack), new Candidate(slot, describe(stack), cat));
        }
        out.addAll(sorted.values());
        return out;
    }

    public static void cycleSwap(Minecraft client, SwapConfig config, Category cat) {
        cycleSwap(client, config, List.of(cat));
    }

    /** Общая кнопка листает категории, одиночная — предметы внутри категории. */
    public static void cycleSwap(Minecraft client, SwapConfig config, List<Category> cats) {
        LocalPlayer player = client.player;
        if (player == null) return;
        List<Category> catList = new ArrayList<>(new LinkedHashSet<>(cats));
        if (catList.size() > 1) {
            cycleByCategory(client, config, player, catList);
        } else {
            cycleWithinCategory(client, config, player, catList.get(0));
        }
    }

    private static void cycleByCategory(Minecraft client, SwapConfig config,
                                        LocalPlayer player, List<Category> catList) {
        ItemStack off = player.getOffhandItem();
        int start = 0;
        if (!off.isEmpty()) {
            int idx = catList.indexOf(classify(off));
            if (idx >= 0) start = (idx + 1) % catList.size();
        }
        for (int k = 0; k < catList.size(); k++) {
            Category cat = catList.get((start + k) % catList.size());
            List<Candidate> bucket = findMatching(player, config, cat);
            if (bucket.isEmpty()) continue;
            Candidate chosen = bucket.get(0);
            if (!off.isEmpty() && describe(off).equals(chosen.label())) continue;
            swapToOffhand(client, chosen.handlerSlot());
            player.displayClientMessage(msg().append(Component.translatable("holyswap.msg.swapped",
                    Component.literal(cat.colorTag + cat.title() + "§r"), chosen.label())), true);
            return;
        }
        player.displayClientMessage(msg().append(Component.translatable(
                "holyswap.msg.none_selected", selectorKeyName())), true);
    }

    private static void cycleWithinCategory(Minecraft client, SwapConfig config,
                                            LocalPlayer player, Category cat) {
        List<Candidate> candidates = findMatching(player, config, cat);
        if (candidates.isEmpty()) {
            ItemStack off = player.getOffhandItem();
            if (!off.isEmpty() && classify(off) == cat) {
                player.displayClientMessage(msg().append(Component.translatable("holyswap.msg.only_in_offhand")), true);
            } else {
                player.displayClientMessage(msg().append(Component.translatable("holyswap.msg.none",
                        Component.literal(cat.colorTag + cat.title() + "§r"))), true);
            }
            return;
        }
        int next = 0;
        ItemStack off = player.getOffhandItem();
        if (!off.isEmpty()) {
            String cur = describe(off);
            boolean offAccounted = false;
            for (int i = 0; i < candidates.size(); i++) {
                if (candidates.get(i).label().equals(cur)) {
                    next = (i + 1) % candidates.size();
                    offAccounted = true;
                    break;
                }
            }
            if (!offAccounted && classify(off) == cat) {
                player.displayClientMessage(msg().append(Component.translatable("holyswap.msg.offhand_unselected",
                        off.getHoverName().getString(), selectorKeyName())), true);
            }
        }
        Candidate chosen = candidates.get(next);
        swapToOffhand(client, chosen.handlerSlot());
        player.displayClientMessage(msg().append(Component.translatable("holyswap.msg.swapped",
                Component.literal(cat.colorTag + cat.title() + "§r"), chosen.label())), true);
    }

    private static MutableComponent msg() {
        return Component.literal("§6[HolySwap]§r ");
    }

    public static String selectorKeyName() {
        int code = HolySwapClient.CONFIG.keyFor(SwapConfig.ACT_SELECTOR, HolySwapClient.defKey(SwapConfig.ACT_SELECTOR));
        return keyName(code, "H");
    }

    public static String keyName(int code, String fallback) {
        if (code == GLFW.GLFW_KEY_UNKNOWN) return fallback;
        String s = GLFW.glfwGetKeyName(code, 0);
        return (s == null || s.isEmpty()) ? fallback : s.toUpperCase();
    }

    public static void swapToOffhand(Minecraft client, int slot) {
        HolySwapClient.playSwapSound(client);
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return;
        client.gameMode.handleInventoryMouseClick(
                player.inventoryMenu.containerId, slot, 40, ClickType.SWAP, player);
    }

    /** Свап конкретного предмета (по имени до " (") — своя кнопка на каждый предмет. */
    public static void swapExactItem(Minecraft client, SwapConfig config, String match) {
        LocalPlayer player = client.player;
        if (player == null) return;
        var menu = player.inventoryMenu;
        for (int slot = 9; slot <= 45; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty()) continue;
            String label = stack.getHoverName().getString();
            if (!label.equals(match)) continue;
            swapToOffhand(client, slot);
            return;
        }
        player.displayClientMessage(msg().append(Component.translatable("holyswap.msg.item_missing", match)), true);
    }

    public record Row(String label, Category category, int count, ItemStack icon) {}

    public static List<Row> listDistinct(LocalPlayer player) {
        Map<String, Row> byLabel = new TreeMap<>();
        var menu = player.inventoryMenu;
        for (int slot = 9; slot <= 45; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty()) continue;
            Category cat = classify(stack);
            if (cat == Category.OTHER) continue;
            String label = describe(stack);
            Row old = byLabel.get(label);
            byLabel.put(label, old == null ? new Row(label, cat, stack.getCount(), stack.copy())
                                           : new Row(label, cat, old.count() + stack.getCount(), old.icon()));
        }
        List<Row> rows = new ArrayList<>(byLabel.values());
        rows.sort(java.util.Comparator.comparingInt(r -> r.category().ordinal()));
        return rows;
    }

    public static String describe(ItemStack stack) {
        return stack.getHoverName().getString() + " (" + BuiltInRegistries.ITEM.getKey(stack.getItem()) + ")";
    }

    private SwapLogic() {}
}
