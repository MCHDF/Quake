package moe.mchdf.quake;

import java.util.Objects;
import moe.mchdf.quake.command.QuakeCommand;
import moe.mchdf.quake.game.GameSession;
import moe.mchdf.quake.game.GameWorld;
import moe.mchdf.quake.listener.PlayerListener;
import moe.mchdf.quake.menu.MenuBook;
import moe.mchdf.quake.menu.MinigameMenu;
import moe.mchdf.quake.menu.WorldDeleteMenu;
import moe.mchdf.quake.menu.WorldTeleportMenu;
import moe.mchdf.quake.weapon.RailGunListener;
import moe.mchdf.quake.weapon.RailGunWeapon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Quake extends JavaPlugin {
    private GameSession game;
    private GameWorld gameWorld;

    @Override
    public void onEnable() {
        gameWorld = new GameWorld(this);
        getServer().getPluginManager().registerEvents(gameWorld, this);
        MenuBook book = new MenuBook(this);
        game = new GameSession(this, book);
        MinigameMenu menu = new MinigameMenu(this, game);
        WorldDeleteMenu worldDeleteMenu = new WorldDeleteMenu(this, gameWorld);
        WorldTeleportMenu worldTeleportMenu = new WorldTeleportMenu(gameWorld);
        var command = Objects.requireNonNull(getCommand("quake"), "quake command is missing from plugin.yml");
        QuakeCommand executor = new QuakeCommand(this, book, game, gameWorld, worldDeleteMenu, worldTeleportMenu);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        RailGunWeapon railGunWeapon = new RailGunWeapon(game.getGameTeams());
        getServer().getPluginManager().registerEvents(new PlayerListener(book, menu, game), this);
        getServer().getPluginManager().registerEvents(worldDeleteMenu, this);
        getServer().getPluginManager().registerEvents(worldTeleportMenu, this);
        getServer().getPluginManager().registerEvents(game.getDoubleJump(), this);
        getServer().getPluginManager().registerEvents(new RailGunListener(railGunWeapon), this);
        getServer().getOnlinePlayers().forEach(book::giveMenuBook);
        // Plugin startup logic
        getComponentLogger().info(
                Component.text("MCHDF Quake 플러그인이 활성화 되었습니다.", NamedTextColor.GREEN, TextDecoration.BOLD)
        );
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.shutdown();
        }
        gameWorld = null;
        // Plugin shutdown logic
        getComponentLogger().info(
                Component.text("MCHDF Quake 플러그인이 비활성화 되었습니다.", NamedTextColor.RED, TextDecoration.BOLD)
        );
    }

    public GameWorld getGameWorld() {
        if (gameWorld == null) {
            throw new IllegalStateException("Game world has not been loaded yet.");
        }
        return gameWorld;
    }
}
