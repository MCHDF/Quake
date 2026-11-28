package moe.mchdf.quake.menu;

import java.util.List;
import moe.mchdf.quake.mod.ModSettingsScreen;
import moe.mchdf.quake.settings.PlayerSettings;
import moe.mchdf.quake.settings.PlayerSettingsService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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
 * 두 사용자가 바꿀 수 있는 항목이 달라 화면을 나눈다.
 *
 * <p>상자형 GUI는 항목을 클릭할 때마다 값을 바꿔 바로 저장하고, 화면을 다시 그려 현재 값을 보여 준다.
 */
public final class SettingsMenu implements Listener {
    static final int ROWS = 3;
    static final int SIDEBAR_SLOT = 11;
    static final int AUTO_REJOIN_SLOT = 15;
    static final int CLOSE_SLOT = ROWS * 9 - 1;

    private final ModSettingsScreen modScreen;
    private final PlayerSettingsService settings;

    public SettingsMenu(ModSettingsScreen modScreen, PlayerSettingsService settings) {
        this.modScreen = modScreen;
        this.settings = settings;
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
        render(holder.inventory, settings.get(player));
        player.openInventory(holder.inventory);
    }

    static void render(Inventory inventory, PlayerSettings current) {
        inventory.setItem(SIDEBAR_SLOT, toggle(Material.MAP, "사이드바", current.sidebar(),
                "화면 오른쪽의 점수·서버 정보를 보여 줍니다."));
        inventory.setItem(AUTO_REJOIN_SLOT, toggle(Material.REPEATER, "자동 다시 참여", current.autoRejoin(),
                "게임이 끝나면 다음 게임에 자동으로 참여합니다."));
        inventory.setItem(CLOSE_SLOT, item(Material.BARRIER, "닫기", NamedTextColor.RED, List.of()));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SettingsHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        switch (event.getRawSlot()) {
            case SIDEBAR_SLOT -> settings.update(player, current -> current.withSidebar(!current.sidebar()));
            case AUTO_REJOIN_SLOT -> settings.update(player, current -> current.withAutoRejoin(!current.autoRejoin()));
            case CLOSE_SLOT -> {
                player.closeInventory();
                return;
            }
            default -> {
                return;
            }
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5F, 1.0F);
        render(holder.inventory, settings.get(player));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof SettingsHolder) {
            event.setCancelled(true);
        }
    }

    private static ItemStack toggle(Material material, String name, boolean on, String description) {
        ItemStack item = item(material, name, on ? NamedTextColor.GREEN : NamedTextColor.GRAY, List.of(
                Component.text(description, NamedTextColor.GRAY),
                Component.empty(),
                Component.text("현재: ", NamedTextColor.GRAY)
                        .append(on ? Component.text("켜짐", NamedTextColor.GREEN)
                                : Component.text("꺼짐", NamedTextColor.RED)),
                Component.text("클릭하여 " + (on ? "끄기" : "켜기"), NamedTextColor.YELLOW)));
        item.editMeta(meta -> meta.setEnchantmentGlintOverride(on));
        return item;
    }

    private static ItemStack item(Material material, String name, NamedTextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
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
