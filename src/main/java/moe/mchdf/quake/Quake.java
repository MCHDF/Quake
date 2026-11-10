package moe.mchdf.quake;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Quake extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getComponentLogger().info(
                Component.text("MCHDF Quake 플러그인이 활성화 되었습니다.", NamedTextColor.GREEN, TextDecoration.BOLD)
        );
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getSLF4JLogger().info("<red>MCHDF Quake 플러그인 비활성화</red>");
        getComponentLogger().info(
                Component.text("MCHDF Quake 플러그인이 비활성화 되었습니다.", NamedTextColor.RED, TextDecoration.BOLD)
        );
    }
}
