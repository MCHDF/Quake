package moe.mchdf.quake.world;

import moe.mchdf.quake.menu.MenuBook;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class LobbyArrival {
    private final LobbyWorldManager lobby;
    private final MenuBook book;

    public LobbyArrival(LobbyWorldManager lobby, MenuBook book) {
        this.lobby = lobby;
        this.book = book;
    }

    public void resetAndSend(Player player) {
        player.closeInventory();
        player.getInventory().clear();
        player.setItemOnCursor(null);
        player.getInventory().setHeldItemSlot(0);
        book.giveMenuBook(player);
        player.setGameMode(GameMode.ADVENTURE);

        Location spawn = lobby.spawn();
        if (spawn != null) {
            player.teleport(spawn);
        }
    }
}
