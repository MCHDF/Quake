package moe.mchdf.quake.item;

import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * 플러그인 아이템을 문자열 id로 구분한다. id는 아이템 PDC의 {@code quake:item_id}에 저장한다.
 * 장비·상점 품목과 (예정) 모드 패킷도 같은 id를 사용한다.
 */
public final class QuakeItems {
    public static final NamespacedKey ITEM_ID = new NamespacedKey("quake", "item_id");

    public static final String RAILGUN = "railgun";
    public static final String RAILGUN_TEST = "railgun_test";
    public static final String MENU_BOOK = "menu_book";
    public static final String SETTINGS = "settings";

    // item_id 도입 전에 지급된 아이템을 알아보기 위한 예전 표시
    private static final NamespacedKey LEGACY_TEST_RAILGUN = new NamespacedKey("quake", "test_railgun");
    private static final NamespacedKey LEGACY_MENU_BOOK = new NamespacedKey("quake", "minigame_menu_book");
    private static final Component LEGACY_RAILGUN_NAME =
            Component.text("RailGun", NamedTextColor.GOLD, TextDecoration.BOLD);

    private QuakeItems() {
    }

    /** 아이템에 id를 기록한다. */
    public static void setId(ItemMeta meta, String id) {
        meta.getPersistentDataContainer().set(ITEM_ID, PersistentDataType.STRING, id);
    }

    /** 아이템의 id. 플러그인 아이템이 아니면 {@code null} */
    public static String idOf(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        PersistentDataContainerView data = item.getPersistentDataContainer();
        String id = data.get(ITEM_ID, PersistentDataType.STRING);
        if (id != null) {
            return id;
        }
        return legacyIdOf(item, data);
    }

    public static boolean is(ItemStack item, String id) {
        return id.equals(idOf(item));
    }

    private static String legacyIdOf(ItemStack item, PersistentDataContainerView data) {
        if (item.getType() == Material.BOOK && data.has(LEGACY_MENU_BOOK, PersistentDataType.BYTE)) {
            return MENU_BOOK;
        }
        if (item.getType() != RailGunItem.MATERIAL) {
            return null;
        }
        if (data.has(LEGACY_TEST_RAILGUN, PersistentDataType.BOOLEAN)) {
            return RAILGUN_TEST;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && LEGACY_RAILGUN_NAME.equals(meta.displayName())) {
            return RAILGUN;
        }
        return null;
    }
}
