package moe.mchdf.quake.menu;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class MenuBook {
    private final NamespacedKey bookKey;

    public MenuBook(JavaPlugin plugin) {
        bookKey = new NamespacedKey(plugin, "minigame_menu_book");
    }

    public void giveMenuBook(Player player) {
        giveMenuBook(player, false);
    }

    public void giveMenuBook(Player player, boolean notify) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isMenuBook(item)) {
                if (notify) {
                    player.sendMessage(Component.text("이미 미니게임 메뉴 책을 가지고 있습니다.", NamedTextColor.YELLOW));
                }
                return;
            }
        }
        int slot = player.getInventory().firstEmpty();
        if (slot == -1) {
            player.sendMessage(Component.text(notify
                    ? "미니게임 책을 받을 공간이 없습니다. 인벤토리를 비우고 명령어를 다시 실행해 주세요."
                    : "미니게임 책을 받을 공간이 없습니다. 인벤토리를 비우고 재접속해 주세요.", NamedTextColor.YELLOW));
            return;
        }
        ItemStack book = item(Material.BOOK, "미니게임 메뉴", NamedTextColor.GOLD,
                "우클릭하여 미니게임 참여 메뉴를 엽니다.");
        book.editMeta(meta -> meta.getPersistentDataContainer().set(bookKey, PersistentDataType.BYTE, (byte) 1));
        player.getInventory().setItem(slot, book);
        if (notify) {
            player.sendMessage(Component.text("미니게임 메뉴 책을 지급했습니다.", NamedTextColor.GREEN));
        }
    }

    public void removeMenuBooks(Player player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (isMenuBook(inventory.getItem(slot))) {
                inventory.setItem(slot, null);
            }
        }
        if (isMenuBook(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
        }
    }

    public boolean isMenuBook(ItemStack item) {
        return item != null && item.getType() == Material.BOOK
                && item.getPersistentDataContainer().has(bookKey, PersistentDataType.BYTE);
    }

    private static ItemStack item(Material material, String name, NamedTextColor color, String description) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text(description, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        });
        return item;
    }
}
