package moe.mchdf.quake.menu;

import java.util.List;
import moe.mchdf.quake.mod.ModSettingsScreen;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * 내 설정 화면. 모드 사용자는 모드 전용 화면을, 바닐라 사용자는 상자형 GUI를 연다.
 * 두 사용자가 바꿀 수 있는 항목이 달라 화면을 나눈다. (설정 항목은 다음 단계에서 채운다)
 */
public final class SettingsMenu implements Listener {
    static final int ROWS = 3;
    static final int INFO_SLOT = 13;
    static final int CLOSE_SLOT = ROWS * 9 - 1;

    private final ModSettingsScreen modScreen;

    public SettingsMenu(ModSettingsScreen modScreen) {
        this.modScreen = modScreen;
    }

    public void open(Player player) {
        if (modScreen.open(player)) {
            return;
        }
        openVanilla(player);
    }

    private void openVanilla(Player player) {
        SettingsHolder holder = new SettingsHolder();
        holder.inventory = Bukkit.createInventory(holder, ROWS * 9, Component.text("설정", NamedTextColor.DARK_AQUA));
        holder.inventory.setItem(INFO_SLOT, item(Material.PAPER, "설정 항목 준비 중", NamedTextColor.YELLOW,
                "효과음, 사이드바, 처치 알림 같은 설정이 곧 이곳에 추가됩니다."));
        holder.inventory.setItem(CLOSE_SLOT, item(Material.BARRIER, "닫기", NamedTextColor.RED, null));
        player.openInventory(holder.inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SettingsHolder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() == CLOSE_SLOT) {
            event.getWhoClicked().closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof SettingsHolder) {
            event.setCancelled(true);
        }
    }

    private static ItemStack item(Material material, String name, NamedTextColor color, String description) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
            if (description != null) {
                meta.lore(List.of(Component.text(description, NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));
            }
        });
        return item;
    }

    private static final class SettingsHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
