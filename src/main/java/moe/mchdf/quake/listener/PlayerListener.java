package moe.mchdf.quake.listener;

import moe.mchdf.quake.game.GameSession;
import moe.mchdf.quake.menu.MenuBook;
import moe.mchdf.quake.menu.MinigameMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    public PlayerListener(MenuBook book, MinigameMenu menu, GameSession game) {
        this.book = book;
        this.menu = menu;
        this.game = game;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!event.getPlayer().isOp() && book.isMenuBook(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("미니게임 메뉴 책은 버릴 수 없습니다.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        book.giveMenuBook(event.getPlayer());
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
