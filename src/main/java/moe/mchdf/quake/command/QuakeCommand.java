package moe.mchdf.quake.command;

import java.util.List;
import java.util.Locale;
import moe.mchdf.quake.menu.MinigameMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class QuakeCommand implements TabExecutor {
    private final MinigameMenu menu;

    public QuakeCommand(MinigameMenu menu) {
        this.menu = menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("플레이어만 사용할 수 있는 명령어입니다.", NamedTextColor.RED));
            return true;
        }
        if (!player.isOp()) {
            player.sendMessage(Component.text("OP만 사용할 수 있는 명령어입니다.", NamedTextColor.RED));
            return true;
        }
        if (args.length != 2 || !args[0].equalsIgnoreCase("get") || !args[1].equalsIgnoreCase("book")) {
            player.sendMessage(Component.text("사용법: /quake get book", NamedTextColor.YELLOW));
            return true;
        }
        menu.giveMenuBook(player, true);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !sender.isOp()) {
            return List.of();
        }
        if (args.length == 1 && "get".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            return List.of("get");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("get")
                && "book".startsWith(args[1].toLowerCase(Locale.ROOT))) {
            return List.of("book");
        }
        return List.of();
    }
}
