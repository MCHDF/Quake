package moe.mchdf.quake.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class RailGunItem {
    public static final String DISPLAY_NAME = "RailGun";

    private RailGunItem() {
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.IRON_SHOVEL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(DISPLAY_NAME, NamedTextColor.GOLD, TextDecoration.BOLD));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isRailGun(ItemStack item) {
        if (item == null || item.getType() != Material.IRON_SHOVEL) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        Component displayName = meta.displayName();
        return displayName != null && displayName.equals(Component.text(DISPLAY_NAME, NamedTextColor.GOLD, TextDecoration.BOLD));
    }

    public static void removeFromInventory(Player player) {
        if (player == null) {
            return;
        }
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (isRailGun(item)) {
                inventory.clear(i);
            }
        }
    }
}
