package moe.mchdf.quake.menu;

import java.util.HashMap;
import java.util.Map;
import moe.mchdf.quake.game.GameWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldDeleteMenu implements Listener {
    private static final int INVENTORY_SIZE = 54;

    private final JavaPlugin plugin;
    private final GameWorld gameWorld;

    public WorldDeleteMenu(JavaPlugin plugin, GameWorld gameWorld) {
        this.plugin = plugin;
        this.gameWorld = gameWorld;
    }

    public void open(Player player) {
        DeleteMenuHolder holder = new DeleteMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE,
                Component.text("삭제할 월드 선택", NamedTextColor.RED, TextDecoration.BOLD));
        int slot = 0;
        for (World world : gameWorld.list()) {
            if (slot >= INVENTORY_SIZE) {
                break;
            }
            holder.worlds.put(slot, world.getName());
            inventory.setItem(slot++, createWorldItem(world, player));
        }
        player.openInventory(inventory);
    }

    private ItemStack createWorldItem(World world, Player player) {
        Material material = world.equals(player.getWorld())
                ? Material.LIME_STAINED_GLASS
                : Material.WHITE_STAINED_GLASS;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String suffix = world.equals(player.getWorld()) ? " (현재 월드)" : "";
        meta.displayName(Component.text(world.getName() + suffix, NamedTextColor.WHITE));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof DeleteMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        String worldName = holder.worlds.get(event.getRawSlot());
        if (worldName == null) {
            return;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(Component.text("월드가 이미 언로드되었습니다: " + worldName, NamedTextColor.RED));
            player.closeInventory();
            return;
        }
        if (world.equals(player.getWorld())) {
            player.sendMessage(Component.text("현재 위치한 월드는 삭제할 수 없습니다.", NamedTextColor.RED));
            return;
        }
        if (!world.getPlayers().isEmpty()) {
            player.sendMessage(Component.text("다른 플레이어가 있는 월드는 삭제할 수 없습니다.", NamedTextColor.RED));
            return;
        }
        try {
            if (gameWorld.delete(world)) {
                player.sendMessage(Component.text("월드를 삭제했습니다: " + worldName, NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("월드를 언로드하지 못했습니다: " + worldName, NamedTextColor.RED));
            }
        } catch (IllegalStateException exception) {
            player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
        }
        player.closeInventory();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof DeleteMenuHolder) {
            event.setCancelled(true);
        }
    }

    private static final class DeleteMenuHolder implements InventoryHolder {
        private final Map<Integer, String> worlds = new HashMap<>();

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
