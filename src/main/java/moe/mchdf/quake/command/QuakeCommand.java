package moe.mchdf.quake.command;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import java.util.Locale;
import moe.mchdf.quake.game.GameSession;
import moe.mchdf.quake.menu.MenuBook;
import moe.mchdf.quake.menu.WorldDeleteMenu;
import moe.mchdf.quake.menu.WorldTeleportMenu;
import moe.mchdf.quake.world.GameWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class QuakeCommand implements TabExecutor {
    private final MenuBook book;
    private final GameSession game;
    private final GameWorld gameWorld;
    private final JavaPlugin plugin;
    private final WorldDeleteMenu worldDeleteMenu;
    private final WorldTeleportMenu worldTeleportMenu;

    public QuakeCommand(JavaPlugin plugin, MenuBook book, GameSession game, GameWorld gameWorld,
                        WorldDeleteMenu worldDeleteMenu, WorldTeleportMenu worldTeleportMenu) {
        this.plugin = plugin;
        this.book = book;
        this.game = game;
        this.gameWorld = gameWorld;
        this.worldDeleteMenu = worldDeleteMenu;
        this.worldTeleportMenu = worldTeleportMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("플레이어만 사용할 수 있는 명령어입니다.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setTimer")) {
            int seconds;
            try {
                seconds = Integer.parseInt(args[1]);
                if (seconds <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException exception) {
                player.sendMessage(Component.text("시간은 1~2147483647 사이의 정수(초)로 입력해 주세요.", NamedTextColor.RED));
                return true;
            }
            game.setGameDurationSeconds(seconds);
            player.sendMessage(Component.text("게임 시간을 " + seconds + "초로 설정했습니다. 다음 게임부터 적용됩니다.", NamedTextColor.GREEN));
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setScore")) {
            int score;
            try {
                score = Integer.parseInt(args[1]);
                if (score <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException exception) {
                player.sendMessage(Component.text("점수는 1~2147483647 사이의 정수로 입력해 주세요.", NamedTextColor.RED));
                return true;
            }
            game.setTargetScore(score);
            player.sendMessage(Component.text("승점 기준을 " + score + "점으로 설정했습니다. 다음 게임부터 적용됩니다.", NamedTextColor.GREEN));
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("world") && args[1].equalsIgnoreCase("create")) {
            openCreateWorldDialog(player);
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("world")
                && args[1].equalsIgnoreCase("delete")) {
            worldDeleteMenu.open(player);
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("world")
                && args[1].equalsIgnoreCase("tp")) {
            worldTeleportMenu.open(player);
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("world")
                && args[1].equalsIgnoreCase("list")) {
            player.sendMessage(Component.text("로드된 월드 목록:", NamedTextColor.GREEN));
            for (var world : gameWorld.list()) {
                player.sendMessage(Component.text("- " + world.getName() + " (" + world.getEnvironment() + ")",
                        NamedTextColor.GRAY));
            }
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("world")
                && args[1].equalsIgnoreCase("create")) {
            try {
                gameWorld.create(args[2]);
                player.sendMessage(Component.text("월드를 생성했습니다: " + args[2], NamedTextColor.GREEN));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
            }
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("world")
                && args[1].equalsIgnoreCase("delete")) {
            var world = org.bukkit.Bukkit.getWorld(args[2]);
            if (world == null) {
                player.sendMessage(Component.text("로드된 월드를 찾을 수 없습니다: " + args[2], NamedTextColor.RED));
            } else if (world.equals(player.getWorld())) {
                player.sendMessage(Component.text("현재 위치한 월드는 삭제할 수 없습니다.", NamedTextColor.RED));
            } else if (gameWorld.delete(world)) {
                player.sendMessage(Component.text("월드를 삭제했습니다: " + args[2], NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("월드를 언로드하지 못했습니다: " + args[2], NamedTextColor.RED));
            }
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("world")
                && args[1].equalsIgnoreCase("tp")) {
            var world = org.bukkit.Bukkit.getWorld(args[2]);
            if (world == null) {
                player.sendMessage(Component.text("로드된 월드를 찾을 수 없습니다: " + args[2], NamedTextColor.RED));
            } else {
                player.teleport(world.getSpawnLocation());
                player.sendMessage(Component.text("월드로 이동했습니다: " + args[2], NamedTextColor.GREEN));
            }
            return true;
        }
        if (args.length != 2 || !args[0].equalsIgnoreCase("get") || !args[1].equalsIgnoreCase("book")) {
            player.sendMessage(Component.text("사용법: /quake get book, /quake setTimer <seconds>, /quake setScore <score>, /quake world <create|delete|tp> ...", NamedTextColor.YELLOW));
            return true;
        }
        if (game.isRunning() && game.contains(player)) {
            player.sendMessage(Component.text("게임 참여 중에는 메뉴 책을 받을 수 없습니다.", NamedTextColor.RED));
            return true;
        }
        book.giveMenuBook(player, true);
        return true;
    }

    private void openCreateWorldDialog(Player player) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("게임 월드 생성", NamedTextColor.GOLD))
                        .canCloseWithEscape(true)
                        .body(List.of(DialogBody.plainMessage(Component.text(
                                "영문, 숫자, _, -로 월드 이름을 입력하세요.",
                                NamedTextColor.GRAY))))
                        .inputs(List.of(DialogInput.text(
                                "world_name",
                                Component.text("월드 이름", NamedTextColor.WHITE)).build()))
                        .build())
                .type(DialogType.multiAction(List.of(
                        ActionButton.builder(Component.text("생성", NamedTextColor.GREEN))
                                .action(DialogAction.customClick((response, audience) -> {
                                    if (audience instanceof Player owner
                                            && owner.getUniqueId().equals(player.getUniqueId())) {
                                        String name = response.getText("world_name");
                                        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                                            try {
                                                gameWorld.create(name);
                                                owner.sendMessage(Component.text(
                                                        "월드를 생성했습니다: " + name, NamedTextColor.GREEN));
                                            } catch (IllegalArgumentException | IllegalStateException exception) {
                                                owner.sendMessage(Component.text(
                                                        exception.getMessage(), NamedTextColor.RED));
                                            }
                                        });
                                    }
                                }, ClickCallback.Options.builder().uses(1).build()))
                                .build()))
                        .columns(1)
                        .exitAction(ActionButton.builder(Component.text("취소")).build())
                        .build()));
        player.showDialog(dialog);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player) || !command.testPermissionSilent(sender)) {
            return List.of();
        }
        if (args.length == 1) {
            return List.of("get", "setTimer", "setScore", "world").stream()
                    .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setTimer")) {
            return List.of("180").stream().filter(option -> option.startsWith(args[1])).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setScore")) {
            return List.of("5").stream().filter(option -> option.startsWith(args[1])).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("get")
                && "book".startsWith(args[1].toLowerCase(Locale.ROOT))) {
            return List.of("book");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("world")) {
            return List.of("create", "delete", "tp", "list").stream()
                    .filter(option -> option.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}