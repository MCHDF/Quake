package moe.mchdf.quake.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;
import moe.mchdf.quake.game.GameSession;
import moe.mchdf.quake.world.GameWorld;
import moe.mchdf.quake.world.GameWorldInfo;
import moe.mchdf.quake.world.GameWorldStatus;
import moe.mchdf.quake.world.LobbyWorldManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class QuakeCommandLobbyTest {
    private final GameWorld worlds = mock(GameWorld.class);
    private final LobbyWorldManager lobby = mock(LobbyWorldManager.class);
    private final Command command = mock(Command.class);
    private final Player player = mock(Player.class);
    private final World destination = mock(World.class);
    private final UUID playerId = UUID.randomUUID();
    private final QuakeCommand executor = new QuakeCommand(null, null, mock(GameSession.class), worlds, null, null);

    QuakeCommandLobbyTest() {
        when(worlds.lobby()).thenReturn(lobby);
        when(player.getWorld()).thenReturn(destination);
        when(player.getUniqueId()).thenReturn(playerId);
        when(destination.getName()).thenReturn("hub");
        when(command.testPermission(player)).thenReturn(true);
        when(command.testPermissionSilent(player)).thenReturn(true);
    }

    @Test
    void movesLobbyToCurrentPositionAndResetClearsTheSetting() {
        var location = new Location(destination, 10.25, 64, -5.5, 135, -20);
        when(player.getLocation()).thenReturn(location);
        World oldWorld = mock(World.class);
        Player visitor = mock(Player.class);
        when(oldWorld.getPlayers()).thenReturn(List.of(visitor));
        when(lobby.spawn()).thenReturn(new Location(oldWorld, 0, 64, 0));
        when(visitor.teleport(location)).thenReturn(true);

        execute("move");
        verify(lobby).setLobbyWorld(location, playerId);
        verify(visitor).teleport(location);

        execute("clear");
        verify(lobby).clearLobbyWorld();
    }

    @Test
    void registeredWorldRequiresConfirmationAndReadyWorldIsRejected() {
        UUID worldId = UUID.randomUUID();
        when(destination.getUID()).thenReturn(worldId);
        when(worlds.isGameWorld(destination)).thenReturn(true);
        when(worlds.requireInfo("hub")).thenReturn(
                new GameWorldInfo("hub", worldId, "Hub", GameWorldStatus.MAINTENANCE, null, null));
        execute("move");
        verifyNoInteractions(lobby);

        when(worlds.requireInfo("hub")).thenReturn(
                new GameWorldInfo("hub", worldId, "Hub", GameWorldStatus.READY, null, null));
        execute("move");
        assertEquals(List.of(), complete("set", ""));
        verifyNoInteractions(lobby);
    }

    @Test
    void completesLobbyCommandsAndTheCurrentEligibleWorld() {
        assertEquals(List.of("lobby"), executor.onTabComplete(player, command, "quake", new String[]{"world", "lo"}));
        assertEquals(List.of("set", "move", "clear"), complete(""));
        assertEquals(List.of("hub"), complete("set", "hu"));
    }

    private void execute(String action) {
        executor.onCommand(player, command, "quake", new String[]{"world", "lobby", action});
    }

    private List<String> complete(String... suffix) {
        var args = new String[suffix.length + 2];
        args[0] = "world";
        args[1] = "lobby";
        System.arraycopy(suffix, 0, args, 2, suffix.length);
        return executor.onTabComplete(player, command, "quake", args);
    }
}
