package moe.mchdf.quake.menu;

import java.util.List;
import java.util.UUID;
import moe.mchdf.quake.world.GameWorld;
import moe.mchdf.quake.world.LobbyWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldTeleportMenu implements Listener {
    private static final int ROW_SIZE = 9;
    private static final int MAX_SIZE = 54;
    private static final int PAGE_SIZE = MAX_SIZE - ROW_SIZE;
    private static final NamespacedKey OVERWORLD = NamespacedKey.minecraft("overworld");

    private final JavaPlugin plugin;
    private final GameWorld gameWorld;
    private final LobbyWorld lobby;

    public WorldTeleportMenu(JavaPlugin plugin, GameWorld gameWorld, LobbyWorld lobby) {
        this.plugin = plugin;
        this.gameWorld = gameWorld;
        this.lobby = lobby;
    }

    public void open(Player player) {
        open(player, 0);
    }

    private void open(Player player, int requestedPage) {
        Location lobbySpawn = lobby.spawn();
        World lobbyWorld = lobbySpawn == null ? null : lobbySpawn.getWorld();
        List<World> worlds = gameWorld.listTeleportWorlds(lobbyWorld);
        boolean paginated = worlds.size() > MAX_SIZE;
        int lastPage = paginated ? (worlds.size() - 1) / PAGE_SIZE : 0;
        int page = Math.min(requestedPage, lastPage);
        int start = page * PAGE_SIZE;
        List<World> entries = worlds.subList(start, Math.min(worlds.size(), start + (paginated ? PAGE_SIZE : MAX_SIZE)));
        int entriesSize = Math.max(ROW_SIZE, ((entries.size() + ROW_SIZE - 1) / ROW_SIZE) * ROW_SIZE);
        TeleportMenuHolder holder = new TeleportMenuHolder(entries.stream().map(World::getUID).toList(), page,
                lastPage, entriesSize);
        holder.inventory = Bukkit.createInventory(holder, entriesSize + (paginated ? ROW_SIZE : 0),
                Component.text("이동할 월드 선택", NamedTextColor.AQUA));
        for (int slot = 0; slot < entries.size(); slot++) {
            World world = entries.get(slot);
            holder.inventory.setItem(slot, createWorldItem(world, player, world.equals(lobbyWorld)));
        }
        if (page > 0) {
            holder.inventory.setItem(entriesSize, namedItem(Material.ARROW, "이전 페이지", NamedTextColor.WHITE));
        }
        if (page < lastPage) {
            holder.inventory.setItem(entriesSize + ROW_SIZE - 1, namedItem(Material.ARROW, "다음 페이지", NamedTextColor.WHITE));
        }
        player.openInventory(holder.inventory);
    }

    private ItemStack createWorldItem(World world, Player player, boolean isLobby) {
        Material material = Material.GRAY_STAINED_GLASS_PANE;
        NamedTextColor color = NamedTextColor.GRAY;
        String label = "기본 오버월드";
        if (isLobby) {
            material = Material.BLUE_STAINED_GLASS_PANE;
            color = NamedTextColor.BLUE;
            label = "로비";
        } else if (!OVERWORLD.equals(world.getKey())) {
            var status = gameWorld.requireInfo(world.getName()).status();
            material = switch (status) {
                case READY -> Material.GREEN_STAINED_GLASS_PANE;
                case MAINTENANCE -> Material.YELLOW_STAINED_GLASS_PANE;
                case DISABLED -> Material.RED_STAINED_GLASS_PANE;
            };
            color = switch (status) {
                case READY -> NamedTextColor.GREEN;
                case MAINTENANCE -> NamedTextColor.YELLOW;
                case DISABLED -> NamedTextColor.RED;
            };
            label = gameWorld.statusLabel(world);
        }
        String suffix = world.equals(player.getWorld()) ? " (현재 월드)" : "";
        return namedItem(material, gameWorld.displayName(world) + " [" + label + "]" + suffix, color);
    }

    private ItemStack namedItem(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false)));
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof TeleportMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (!(event.getWhoClicked() instanceof Player player) || slot < 0 || slot >= holder.inventory.getSize()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(holder.inventory)) {
                return;
            }
            if (slot < holder.worlds.size()) {
                teleport(player, holder.worlds.get(slot));
                player.closeInventory();
            } else if (slot == holder.navigationStart && holder.page > 0) {
                open(player, holder.page - 1);
            } else if (slot == holder.navigationStart + ROW_SIZE - 1 && holder.page < holder.lastPage) {
                open(player, holder.page + 1);
            }
        });
    }

    private void teleport(Player player, UUID worldId) {
        World world = Bukkit.getWorld(worldId);
        if (world == null) {
            player.sendMessage(Component.text("월드가 언로드되었습니다.", NamedTextColor.RED));
            return;
        }
        Location lobbySpawn = lobby.spawn();
        boolean isLobby = lobbySpawn != null && world.equals(lobbySpawn.getWorld());
        if (!isLobby && !gameWorld.isTeleportWorld(world)) {
            player.sendMessage(Component.text("이동 목록에 없는 월드입니다: " + world.getName(), NamedTextColor.RED));
        } else if (player.teleport(isLobby ? lobbySpawn : world.getSpawnLocation())) {
            player.sendMessage(Component.text("월드로 이동했습니다: " + world.getName(), NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("월드 이동에 실패했습니다: " + world.getName(), NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof TeleportMenuHolder) {
            event.setCancelled(true);
        }
    }

    private static final class TeleportMenuHolder implements InventoryHolder {
        private final List<UUID> worlds;
        private final int page;
        private final int lastPage;
        private final int navigationStart;
        private Inventory inventory;

        private TeleportMenuHolder(List<UUID> worlds, int page, int lastPage, int navigationStart) {
            this.worlds = worlds;
            this.page = page;
            this.lastPage = lastPage;
            this.navigationStart = navigationStart;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
