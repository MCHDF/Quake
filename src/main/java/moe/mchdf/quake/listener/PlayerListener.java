package moe.mchdf.quake.listener;

import moe.mchdf.quake.game.GameSession;
import moe.mchdf.quake.menu.MenuBook;
import moe.mchdf.quake.menu.MinigameMenu;
import moe.mchdf.quake.world.LobbyArrival;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {
    private final MenuBook book;
    private final MinigameMenu menu;
    private final GameSession game;
    private final LobbyArrival lobbyArrival;

    public PlayerListener(MenuBook book, MinigameMenu menu, GameSession game, LobbyArrival lobbyArrival) {
        this.book = book;
        this.menu = menu;
        this.game = game;
        this.lobbyArrival = lobbyArrival;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!event.getPlayer().isOp() && book.isMenuBook(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("미니게임 메뉴 책은 버릴 수 없습니다.", NamedTextColor.RED, TextDecoration.BOLD));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        lobbyArrival.resetAndSend(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        game.leave(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if ((event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
                || !book.isMenuBook(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        menu.open(event.getPlayer());
    }
}