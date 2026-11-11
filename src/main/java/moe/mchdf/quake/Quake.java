package moe.mchdf.quake;

import java.util.Objects;
import moe.mchdf.quake.command.QuakeCommand;
import moe.mchdf.quake.game.GameSession;
import moe.mchdf.quake.listener.PlayerListener;
import moe.mchdf.quake.menu.MenuBook;
import moe.mchdf.quake.menu.MinigameMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Quake extends JavaPlugin {
    private GameSession game;

    @Override
    public void onEnable() {
        MenuBook book = new MenuBook(this);
        game = new GameSession(this, book);
        MinigameMenu menu = new MinigameMenu(this, game);
        var command = Objects.requireNonNull(getCommand("quake"), "quake command is missing from plugin.yml");
        QuakeCommand executor = new QuakeCommand(book, game);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        getServer().getPluginManager().registerEvents(new PlayerListener(book, menu, game), this);
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
        // Plugin shutdown logic
        getComponentLogger().info(
                Component.text("MCHDF Quake 플러그인이 비활성화 되었습니다.", NamedTextColor.RED, TextDecoration.BOLD)
        );
    }
}
