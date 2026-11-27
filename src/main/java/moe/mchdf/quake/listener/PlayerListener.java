package moe.mchdf.quake.listener;

import moe.mchdf.quake.game.GameSession;
import moe.mchdf.quake.item.SettingsItem;
import moe.mchdf.quake.menu.MenuBook;
import moe.mchdf.quake.menu.MinigameMenu;
import moe.mchdf.quake.menu.SettingsMenu;
import moe.mchdf.quake.world.LobbyArrival;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.ItemStack;

public final class PlayerListener implements Listener {
    private static final Component SERVER_MOTD = MiniMessage.miniMessage().deserialize(
            "<gold><bold>QUAKE</bold></gold><newline><gray>레일건 팀 대전</gray>");

    private final MenuBook book;
    private final MinigameMenu menu;
    private final SettingsMenu settingsMenu;
    private final GameSession game;
    private final LobbyArrival lobbyArrival;

    public PlayerListener(MenuBook book, MinigameMenu menu, SettingsMenu settingsMenu, GameSession game,
                          LobbyArrival lobbyArrival) {
        this.book = book;
        this.menu = menu;
        this.settingsMenu = settingsMenu;
        this.game = game;
        this.lobbyArrival = lobbyArrival;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (!event.getPlayer().isOp() && (book.isMenuBook(dropped) || SettingsItem.isSettingsItem(dropped))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(MiniMessage.miniMessage().deserialize("[<b><green>+</green></b>] <b><gray>"+event.getPlayer().getName()+"</gray></b>"));
        lobbyArrival.resetAndSend(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(MiniMessage.miniMessage().deserialize("[<b><red>-</red></b>] <b><gray>"+event.getPlayer().getName()+"</gray></b>"));
        game.leave(event.getPlayer());
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        event.motd(SERVER_MOTD);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (book.isMenuBook(event.getItem())) {
            event.setCancelled(true);
            menu.open(event.getPlayer());
        } else if (SettingsItem.isSettingsItem(event.getItem())) {
            event.setCancelled(true);
            settingsMenu.open(event.getPlayer());
        }
    }
}
