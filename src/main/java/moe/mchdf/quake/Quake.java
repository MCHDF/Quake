package moe.mchdf.quake;

import java.util.Objects;
import moe.mchdf.quake.command.QuakeCommand;
import moe.mchdf.quake.menu.MinigameMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Quake extends JavaPlugin {
    private MinigameMenu menu;

    @Override
    public void onEnable() {
        menu = new MinigameMenu(this);
        var command = Objects.requireNonNull(getCommand("quake"), "quake command is missing from plugin.yml");
        QuakeCommand executor = new QuakeCommand(menu);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        getServer().getPluginManager().registerEvents(menu, this);
        getServer().getOnlinePlayers().forEach(menu::giveMenuBook);
        // Plugin startup logic
        getComponentLogger().info(
                Component.text("MCHDF Quake 플러그인이 활성화 되었습니다.", NamedTextColor.GREEN, TextDecoration.BOLD)
        );
    }

    @Override
    public void onDisable() {
        if (menu != null) {
            menu.shutdown();
        }
        // Plugin shutdown logic
        getSLF4JLogger().info("<red>MCHDF Quake 플러그인 비활성화</red>");
        getComponentLogger().info(
                Component.text("MCHDF Quake 플러그인이 비활성화 되었습니다.", NamedTextColor.RED, TextDecoration.BOLD)
        );
    }
}
