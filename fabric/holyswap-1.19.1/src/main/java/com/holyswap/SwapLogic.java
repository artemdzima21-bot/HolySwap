package com.holyswap;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.Registry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Свап эмулирует ровно то, что делает ванильная клавиша F:
 * clickSlot с SlotActionType.SWAP и кнопкой 40 (оффхенд) по playerScreenHandler.
 * Для сервера это неотличимо от обычного игрока.
 * Все тексты игрока — через переводы (holyswap.* в lang-файлах).
 */
public final class SwapLogic {

    public record Candidate(int handlerSlot, String label, Category category) {}

    /** Категории предметов HolyWorld. */
    public enum Category {
        TALISMAN("§a", "holyswap.category.talisman"),
        TALISMAN_PLUS("§e", "holyswap.category.talisman_plus"),
        SPHERE("§b", "holyswap.category.sphere"),
        TOTEM("§d", "holyswap.category.totem"),
        OTHER("§7", "holyswap.category.other");

        public final String colorTag;
        public final String titleKey;

        Category(String colorTag, String titleKey) {
            this.colorTag = colorTag;
            this.titleKey = titleKey;
        }

        public String title() {
            return Text.translatable(titleKey).getString();
        }
    }

    /**
     * Различение категорий: тотем — по типу предмета (ванильный тотем без кастомного имени),
     * талисманы/сферы — по имени; "+" в имени означает улучшенный талисман.
     */
    public static Category classify(ItemStack stack) {
        String id = Registry.ITEM.getId(stack.getItem()).toString();
        String name = stack.getName().getString().toLowerCase();
        if (id.equals("minecraft:totem_of_undying") && !name.contains("талисман") && !name.contains("сфера")
                && !name.contains("talisman") && !name.contains("sphere")) {
            return Category.TOTEM;
        }
        if (name.contains("сфера") || name.contains("sphere")) return Category.SPHERE;
        if (name.contains("талисман") || name.contains("talisman")) {
            return name.contains("+") ? Category.TALISMAN_PLUS : Category.TALISMAN;
        }
        return Category.OTHER;
    }

    /** Все слоты инвентаря (кроме оффхенда), в порядке "хотбар -> основной инвентарь". */
    public static List<Candidate> findMatching(ClientPlayerEntity player, SwapConfig config, Category cat) {
        List<Candidate> out = new ArrayList<>();
        var handler = player.currentScreenHandler;
        // сортируем по имени — стабильный цикл внутри категории
        Map<String, Candidate> sorted = new TreeMap<>();
        for (int inv = 0; inv < 36; inv++) {
            // в PlayerScreenHandler: 36..44 хотбар, 9..35 основной инвентарь
            int handlerSlot = inv < 9 ? 36 + inv : inv;
            ItemStack stack = handler.getSlot(handlerSlot).getStack();
            if (stack.isEmpty() || classify(stack) != cat) continue;
            if (!config.targets.isEmpty() && !config.targets.contains(stack.getName().getString())) continue;
            sorted.put(describe(stack), new Candidate(handlerSlot, describe(stack), cat));
        }
        out.addAll(sorted.values());
        return out;
    }

    /** Циклический свап внутри одной категории. */
    public static void cycleSwap(MinecraftClient client, SwapConfig config, Category cat) {
        cycleSwap(client, config, List.of(cat));
    }

    /**
     * Свап по нескольким категориям сразу (когда на одну кнопку назначено несколько действий).
     * Кнопка листает КАТЕГОРИИ по кругу: если в левой руке уже предмет этой группы —
     * он НЕ заменяется однородным (талисман не меняется на другой талисман, сфера на сферу),
     * а берётся первая кандидатка следующей категории: талисман → сфера → талисман → ...
     */
    public static void cycleSwap(MinecraftClient client, SwapConfig config, List<Category> cats) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        // убираем дубли категорий, сохраняя порядок
        List<Category> catList = new ArrayList<>(new LinkedHashSet<>(cats));
        if (catList.size() > 1) {
            cycleByCategory(client, config, player, catList);
        } else {
            cycleWithinCategory(client, config, player, catList.get(0));
        }
    }

    /** Общая кнопка: цикл по категориям, предмет в руке не заменяется однородным. */
    private static void cycleByCategory(MinecraftClient client, SwapConfig config,
                                        ClientPlayerEntity player, List<Category> catList) {
        ItemStack off = player.getOffHandStack();
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
            // если этот предмет уже в руке — категория закрыта, идём дальше по кругу
            if (!off.isEmpty() && describe(off).equals(chosen.label())) continue;
            swapToOffhand(client, chosen.handlerSlot());
            player.sendMessage(msg().append(Text.translatable("holyswap.msg.swapped",
                    Text.literal(cat.colorTag + cat.title() + "§r"), chosen.label())), false);
            return;
        }
        player.sendMessage(msg().append(Text.translatable("holyswap.msg.none_selected", selectorKeyName())), false);
        return;
    }

    /** Одиночная кнопка: цикл по предметам внутри одной категории. */
    private static void cycleWithinCategory(MinecraftClient client, SwapConfig config,
                                            ClientPlayerEntity player, Category cat) {
        List<Candidate> candidates = findMatching(player, config, cat);
        if (candidates.isEmpty()) {
            ItemStack off = player.getOffHandStack();
            if (!off.isEmpty() && classify(off) == cat) {
                player.sendMessage(msg().append(Text.translatable("holyswap.msg.only_in_offhand")), false);
            } else {
                player.sendMessage(msg().append(Text.translatable("holyswap.msg.none",
                        Text.literal(cat.colorTag + cat.title() + "§r"))), false);
            }
            return;
        }
        int next = 0;
        ItemStack off = player.getOffHandStack();
        boolean offAccounted = false;
        if (!off.isEmpty()) {
            String cur = describe(off);
            for (int i = 0; i < candidates.size(); i++) {
                if (candidates.get(i).label().equals(cur)) {
                    next = (i + 1) % candidates.size();
                    offAccounted = true;
                    break;
                }
            }
            if (!offAccounted && classify(off) == cat) {
                // предмет в руке из этой категории, но не выбран в селекторе —
                // цикл начнётся с первого, подскажем пользователю
                player.sendMessage(msg().append(Text.translatable("holyswap.msg.offhand_unselected",
                        off.getName().getString(), selectorKeyName())), false);
            }
        }
        Candidate chosen = candidates.get(next);
        swapToOffhand(client, chosen.handlerSlot());
        player.sendMessage(msg().append(Text.translatable("holyswap.msg.swapped",
                Text.literal(cat.colorTag + cat.title() + "§r"), chosen.label())), false);
    }

    /** Префикс чата: [HolySwap] золотом. */
    private static net.minecraft.text.MutableText msg() {
        return Text.literal("§6[HolySwap]§r ");
    }

    /** Имя клавиши селектора для подсказок в чате. */
    public static String selectorKeyName() {
        int code = HolySwapClient.CONFIG.keyFor(SwapConfig.ACT_SELECTOR, HolySwapClient.defKey(SwapConfig.ACT_SELECTOR));
        if (code == GLFW.GLFW_KEY_UNKNOWN) return "H";
        String s = InputUtil.Type.KEYSYM.createFromCode(code).getLocalizedText().getString();
        return s.isEmpty() ? "H" : s.toUpperCase();
    }

    public static void swapToOffhand(MinecraftClient client, int handlerSlot) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;
        client.interactionManager.clickSlot(
                player.currentScreenHandler.syncId, handlerSlot, 40, SlotActionType.SWAP, player);
    }

    /** Строка селектора: предмет + его категория. */
    public record Row(String label, Category category, int count) {}

    /** Уникальные HolyWorld-предметы в инвентаре, отсортированные по категориям. */
    public static List<Row> listDistinct(ClientPlayerEntity player) {
        Map<String, Row> byLabel = new TreeMap<>();
        var handler = player.currentScreenHandler;
        for (int handlerSlot = 9; handlerSlot <= 45; handlerSlot++) {
            ItemStack stack = handler.getSlot(handlerSlot).getStack();
            if (stack.isEmpty()) continue;
            Category cat = classify(stack);
            if (cat == Category.OTHER) continue; // только HolyWorld-предметы и тотемы
            String label = describe(stack);
            Row old = byLabel.get(label);
            byLabel.put(label, old == null ? new Row(label, cat, stack.getCount())
                                           : new Row(label, cat, old.count() + stack.getCount()));
        }
        List<Row> rows = new ArrayList<>(byLabel.values());
        rows.sort(java.util.Comparator.comparingInt(r -> r.category().ordinal()));
        return rows;
    }

    public static String describe(ItemStack stack) {
        return stack.getName().getString() + " (" + Registry.ITEM.getId(stack.getItem()) + ")";
    }

    private SwapLogic() {}
}
