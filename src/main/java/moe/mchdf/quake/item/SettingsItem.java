package moe.mchdf.quake.item;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 내 설정 화면을 여는 아이템. 로비에서만 핫바 맨 끝 칸에 둔다.
 *
 * <p>회복 나침반은 우클릭해도 바닐라 동작이 없어 메뉴를 여는 데 쓰기 좋고, 죽은 기록이 없는 로비에서는
 * 바늘이 계속 돌아 조작 장치처럼 보인다.
 */
public final class SettingsItem {
    public static final Material MATERIAL = Material.RECOVERY_COMPASS;
    /** 핫바 맨 끝 칸 (0부터 셈) */
    public static final int SLOT = 8;

    private SettingsItem() {
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(MATERIAL);
        item.editMeta(meta -> {
            meta.displayName(Component.text("설정", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("우클릭하여 내 설정을 엽니다.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            QuakeItems.setId(meta, QuakeItems.SETTINGS);
        });
        return item;
    }

    public static boolean isSettingsItem(ItemStack item) {
        return QuakeItems.is(item, QuakeItems.SETTINGS);
    }

    /** 이미 가지고 있으면 그대로 두고, 없으면 맨 끝 칸(차 있으면 빈 칸)에 넣는다. */
    public static void give(Player player) {
        var inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (isSettingsItem(item)) {
                return;
            }
        }
        int slot = inventory.getItem(SLOT) == null ? SLOT : inventory.firstEmpty();
        if (slot != -1) {
            inventory.setItem(slot, create());
        }
    }

    public static void remove(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isSettingsItem(inventory.getItem(slot))) {
                inventory.setItem(slot, null);
            }
        }
        if (isSettingsItem(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
        }
    }
}
