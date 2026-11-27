package moe.mchdf.quake.command;

import moe.mchdf.quake.menu.SettingsMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** {@code /settings}(별칭 {@code /설정}): 내 설정 화면을 연다. 게임 중에도, 모든 플레이어가 쓸 수 있다. */
public final class SettingsCommand implements CommandExecutor {
    private final SettingsMenu menu;

    public SettingsCommand(SettingsMenu menu) {
        this.menu = menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("플레이어만 사용할 수 있는 명령어입니다.", NamedTextColor.RED));
            return true;
        }
        menu.open(player);
        return true;
    }
}
