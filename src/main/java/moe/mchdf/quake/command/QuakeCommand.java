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
import moe.mchdf.quake.world.GameWorldStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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
        if (args.length == 4 && args[0].equalsIgnoreCase("world")
                && args[1].equalsIgnoreCase("status")) {
            try {
                GameWorldStatus status;
                try {
                    status = GameWorldStatus.valueOf(args[3].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException("상태는 ready, maintenance, disabled 중 하나로 입력해 주세요.");
                }
                gameWorld.setStatus(args[2], status);
                game.onWorldSettingsChanged();
                player.sendMessage(Component.text("월드 상태를 변경했습니다: " + args[2] + " → " + status,
                        NamedTextColor.GREEN));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
            }
            return true;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("world")
                && args[1].equalsIgnoreCase("spawn")) {
            try {
                if (game.isRunning() && game.getCurrentWorld().name().equals(args[2])) {
                    throw new IllegalArgumentException("게임 진행 중인 월드의 스폰은 변경할 수 없습니다.");
                }
                String team = args[3].toUpperCase(Locale.ROOT);
                gameWorld.setSpawn(args[2], team, player.getLocation());
                player.sendMessage(Component.text(args[2] + "의 " + team
                        + " 스폰을 현재 위치와 시선 방향으로 저장했습니다.", NamedTextColor.GREEN));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
            }
            return true;
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("world")
                && args[1].equalsIgnoreCase("players")) {
            try {
                int min = Integer.parseInt(args[3]);
                int max = Integer.parseInt(args[4]);
                gameWorld.setPlayerLimits(args[2], min, max);
                game.onWorldSettingsChanged();
                player.sendMessage(Component.text("월드 인원 설정을 저장했습니다: " + args[2]
                        + " (최소 " + min + "명, 최대 " + max + "명)", NamedTextColor.GREEN));
            } catch (NumberFormatException exception) {
                player.sendMessage(Component.text("최소 인원은 2명 또는 4명, 최대 인원은 2~16 사이의 짝수로 입력해 주세요.", NamedTextColor.RED));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
            }
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
            player.sendMessage(Component.text("등록된 게임 월드 목록 (클릭하여 이동 가능):", NamedTextColor.GREEN));
            for (var world : gameWorld.list()) {
                player.sendMessage(worldListEntry(world));
            }
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("world")
                && args[1].equalsIgnoreCase("create")) {
            try {
                gameWorld.create(args[2], player.getUniqueId());
                player.sendMessage(Component.text("점검 중인 월드를 생성했습니다: " + args[2], NamedTextColor.GREEN));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
            }
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("world")
                && args[1].equalsIgnoreCase("delete")) {
            try {
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
            } catch (IllegalStateException exception) {
                player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
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
            player.sendMessage(Component.text("사용법: /quake get book, /quake setTimer <seconds>, /quake setScore <score>, /quake world <create|delete|tp> ..., /quake world players <name> <min> <max>, /quake world status <name> <ready|maintenance|disabled>, /quake world spawn <name> <red|blue>", NamedTextColor.YELLOW));
            return true;
        }
        if (game.isRunning() && game.contains(player)) {
            player.sendMessage(Component.text("게임 참여 중에는 메뉴 책을 받을 수 없습니다.", NamedTextColor.RED));
            return true;
        }
        book.giveMenuBook(player, true);
        return true;
    }

    private Component worldListEntry(org.bukkit.World world) {
        Component worldName = Component.text(gameWorld.displayName(world), NamedTextColor.WHITE)
                .clickEvent(ClickEvent.callback(audience -> {
                    if (!(audience instanceof Player target)) {
                        return;
                    }
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        org.bukkit.World current = org.bukkit.Bukkit.getWorld(world.getName());
                        if (current == null) {
                            target.sendMessage(Component.text(
                                    "월드가 언로드되었습니다: " + world.getName(), NamedTextColor.RED));
                            return;
                        }
                        if (current.equals(target.getWorld())) {
                            target.sendMessage(Component.text(
                                    "이미 해당 월드에 있습니다: " + current.getName(), NamedTextColor.YELLOW));
                            return;
                        }
                        target.teleport(current.getSpawnLocation());
                        target.sendMessage(Component.text(
                                "월드로 이동했습니다: " + current.getName(), NamedTextColor.GREEN));
                    });
                }, ClickCallback.Options.builder().uses(1).build()))
                .hoverEvent(HoverEvent.showText(Component.text("클릭하여 이동", NamedTextColor.AQUA)));
        return Component.text("- ", NamedTextColor.GRAY)
                .append(worldName)
                .append(Component.text(" [" + gameWorld.statusLabel(world) + "]", NamedTextColor.GRAY));
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
                                                gameWorld.create(name, owner.getUniqueId());
                                                owner.sendMessage(Component.text(
                                                        "점검 중인 월드를 생성했습니다: " + name, NamedTextColor.GREEN));
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
            return List.of("create", "delete", "tp", "list", "players", "status", "spawn").stream()
                    .filter(option -> option.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("world")
                && List.of("status", "spawn", "players").contains(args[1].toLowerCase(Locale.ROOT))) {
            return gameWorld.list().stream().map(org.bukkit.World::getName)
                    .filter(name -> name.startsWith(args[2])).toList();
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("world")) {
            List<String> options = args[1].equalsIgnoreCase("status")
                    ? List.of("ready", "maintenance", "disabled")
                    : args[1].equalsIgnoreCase("spawn") ? List.of("red", "blue") : List.of();
            return options.stream().filter(option -> option.startsWith(args[3].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}