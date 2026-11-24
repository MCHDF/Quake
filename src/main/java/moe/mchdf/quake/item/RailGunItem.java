package moe.mchdf.quake.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class RailGunItem {
    public static final Material MATERIAL = Material.IRON_HOE;
    public static final String DISPLAY_NAME = "RailGun";
    public static final String TEST_DISPLAY_NAME = "RailGun (테스트)";

    private RailGunItem() {
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(DISPLAY_NAME, NamedTextColor.GOLD, TextDecoration.BOLD));
            QuakeItems.setId(meta, QuakeItems.RAILGUN);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 미니게임 밖에서 시험용으로 쓰는 레일건. 빔만 나가고 점수에는 영향을 주지 않는다.
     */
    public static ItemStack createTest() {
        ItemStack item = new ItemStack(MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(TEST_DISPLAY_NAME, NamedTextColor.GRAY, TextDecoration.BOLD));
            QuakeItems.setId(meta, QuakeItems.RAILGUN_TEST);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isTestRailGun(ItemStack item) {
        return QuakeItems.is(item, QuakeItems.RAILGUN_TEST);
    }

    public static boolean isRailGun(ItemStack item) {
        return QuakeItems.is(item, QuakeItems.RAILGUN);
    }

    public static void removeFromInventory(Player player) {
        if (player == null) {
            return;
        }
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            // 테스트 레일건을 들고 경기에 들어가는 일이 없도록 함께 제거
            if (isRailGun(item) || isTestRailGun(item)) {
                inventory.clear(i);
            }
        }
    }
}
