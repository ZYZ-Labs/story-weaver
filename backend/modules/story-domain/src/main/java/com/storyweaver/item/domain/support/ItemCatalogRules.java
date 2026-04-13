package com.storyweaver.item.domain.support;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ItemCatalogRules {

    private static final Map<String, String> CATEGORY_ALIASES = buildCategoryAliases();
    private static final Map<String, String> RARITY_ALIASES = buildRarityAliases();
    private static final Map<String, String> SLOT_ALIASES = buildSlotAliases();
    private static final Set<String> RARE_RARITIES = Set.of("rare", "epic", "legendary", "artifact");

    private ItemCatalogRules() {
    }

    public static String normalizeCategory(String value) {
        return normalizeAlias(value, CATEGORY_ALIASES, "prop");
    }

    public static String normalizeRarity(String value) {
        return normalizeAlias(value, RARITY_ALIASES, "common");
    }

    public static String normalizeSlotType(String value) {
        return normalizeAlias(value, SLOT_ALIASES, "misc");
    }

    public static String normalizeTags(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return new LinkedHashSet<>(java.util.Arrays.stream(value.split("[,，;；|/]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList()).stream().collect(Collectors.joining(", "));
    }

    public static boolean isRare(String rarity) {
        return RARE_RARITIES.contains(normalizeRarity(rarity));
    }

    private static String normalizeAlias(String value, Map<String, String> aliases, String fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        String key = value.trim().toLowerCase(Locale.ROOT);
        return aliases.getOrDefault(key, fallback);
    }

    private static Map<String, String> buildCategoryAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("prop", "prop");
        aliases.put("道具", "prop");
        aliases.put("tool", "prop");
        aliases.put("item", "prop");
        aliases.put("consumable", "consumable");
        aliases.put("药品", "consumable");
        aliases.put("药剂", "consumable");
        aliases.put("消耗品", "consumable");
        aliases.put("potion", "consumable");
        aliases.put("equipment", "equipment");
        aliases.put("装备", "equipment");
        aliases.put("weapon", "equipment");
        aliases.put("gear", "equipment");
        aliases.put("material", "material");
        aliases.put("材料", "material");
        aliases.put("素材", "material");
        aliases.put("quest", "quest");
        aliases.put("任务", "quest");
        aliases.put("任务物品", "quest");
        return aliases;
    }

    private static Map<String, String> buildRarityAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("common", "common");
        aliases.put("普通", "common");
        aliases.put("常见", "common");
        aliases.put("uncommon", "uncommon");
        aliases.put("优秀", "uncommon");
        aliases.put("少见", "uncommon");
        aliases.put("rare", "rare");
        aliases.put("稀有", "rare");
        aliases.put("epic", "epic");
        aliases.put("史诗", "epic");
        aliases.put("legendary", "legendary");
        aliases.put("传说", "legendary");
        aliases.put("artifact", "artifact");
        aliases.put("神器", "artifact");
        return aliases;
    }

    private static Map<String, String> buildSlotAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("weapon", "weapon");
        aliases.put("武器", "weapon");
        aliases.put("head", "head");
        aliases.put("头部", "head");
        aliases.put("helmet", "head");
        aliases.put("body", "body");
        aliases.put("身体", "body");
        aliases.put("armor", "body");
        aliases.put("accessory", "accessory");
        aliases.put("饰品", "accessory");
        aliases.put("ring", "accessory");
        aliases.put("offhand", "offhand");
        aliases.put("副手", "offhand");
        aliases.put("shield", "offhand");
        aliases.put("consumable", "consumable");
        aliases.put("消耗", "consumable");
        aliases.put("misc", "misc");
        aliases.put("其他", "misc");
        aliases.put("none", "misc");
        return aliases;
    }
}
