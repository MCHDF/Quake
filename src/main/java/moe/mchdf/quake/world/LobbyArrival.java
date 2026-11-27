package moe.mchdf.quake.world;

import moe.mchdf.quake.menu.LobbyItems;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class LobbyArrival {
    private final LobbyWorldManager lobby;
    private final LobbyItems lobbyItems;

    public LobbyArrival(LobbyWorldManager lobby, LobbyItems lobbyItems) {
        this.lobby = lobby;
        this.lobbyItems = lobbyItems;
    }

    public void resetAndSend(Player player) {
        player.closeInventory();
        player.getInventory().clear();
        player.setItemOnCursor(null);
        player.getInventory().setHeldItemSlot(0);
        lobbyItems.give(player);
        player.setGameMode(GameMode.ADVENTURE);

        Location spawn = lobby.spawn();
        if (spawn != null) {
            player.teleport(spawn);
        }
    }
}
