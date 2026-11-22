package moe.mchdf.quake.command;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.Arrays;
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
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class QuakeCommand implements TabExecutor {
    private static final int MAX_TIMER_SECONDS = 180;

    private enum WorldSubcommand {
        CREATE, DELETE, TP, PLAYERS, STATUS, SPAWN, LOBBY;

        static WorldSubcommand parse(String input) {
            if (input.equalsIgnoreCase("list")) {
                return TP;
            }
            for (WorldSubcommand subcommand : values()) {
                if (subcommand.name().equalsIgnoreCase(input)) {
                    return subcommand;
                }
            }
            return null;
        }
    }

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
                if (seconds < 1 || seconds > MAX_TIMER_SECONDS) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException exception) {
                player.sendMessage(Component.text("시간은 1~" + MAX_TIMER_SECONDS + " 사이의 정수(초)로 입력해 주세요.", NamedTextColor.RED));
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
        if (args.length >= 2 && args[0].equalsIgnoreCase("world") && executeWorldCommand(player, command, args)) {
            return true;
        }
        if (args.length != 2 || !args[0].equalsIgnoreCase("get") || !args[1].equalsIgnoreCase("book")) {
            sendHelp(player);
            return true;
        }
        if (game.isRunning() && game.contains(player)) {
            player.sendMessage(Component.text("게임 참여 중에는 메뉴 책을 받을 수 없습니다.", NamedTextColor.RED));
            return true;
        }
        book.giveMenuBook(player, true);
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("Quake 명령어", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text("  ·  클릭하여 채팅 입력창에 넣기", NamedTextColor.GRAY)
                        .decoration(TextDecoration.BOLD, false)));
        player.sendMessage(Component.text("미니게임", NamedTextColor.YELLOW));
        sendHelpEntry(player, "/quake get book", "", "참여 메뉴 책 받기");
        sendHelpEntry(player, "/quake setTimer", " <초>", "경기 시간 설정 (1~" + MAX_TIMER_SECONDS + "초)");
        sendHelpEntry(player, "/quake setScore", " <점수>", "목표 점수 설정");

        player.sendMessage(Component.text("월드 관리", NamedTextColor.YELLOW));
        sendHelpEntry(player, "/quake world tp", "", "월드 이동 메뉴 열기");
        sendHelpEntry(player, "/quake world create", " [월드명]", "게임 월드 생성");
        sendHelpEntry(player, "/quake world delete", " [월드명]", "게임 월드 삭제");
        sendHelpEntry(player, "/quake world players", " <최소> <최대>", "현재 게임 월드의 참가 인원 설정");
        sendHelpEntry(player, "/quake world status", " <ready|maintenance|disabled>", "현재 게임 월드의 상태 변경");
        sendHelpEntry(player, "/quake world spawn", " <red|blue>", "현재 위치를 팀 스폰으로 저장");
        sendHelpEntry(player, "/quake world lobby move", "", "현재 위치로 로비 이전");
        sendHelpEntry(player, "/quake world lobby set", " [현재 월드명]", "현재 위치로 로비 지정");
        sendHelpEntry(player, "/quake world lobby clear", "", "로비 설정 초기화");
    }

    private void sendHelpEntry(Player player, String command, String arguments, String description) {
        String suggestion = arguments.isEmpty() ? command : command + " ";
        Component syntax = Component.text(command, NamedTextColor.AQUA)
                .append(Component.text(arguments, NamedTextColor.GREEN))
                .clickEvent(ClickEvent.suggestCommand(suggestion))
                .hoverEvent(HoverEvent.showText(Component.text(description, NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  ").append(syntax)
                .append(Component.text("  ·  " + description, NamedTextColor.GRAY)));
    }

    private boolean executeWorldCommand(Player player, Command command, String[] args) {
        WorldSubcommand subcommand = WorldSubcommand.parse(args[1]);
        if (subcommand == null) {
            return false;
        }
        return switch (subcommand) {
            case CREATE -> createWorld(player, args);
            case DELETE -> deleteWorld(player, args);
            case TP -> tpWorld(player);
            case PLAYERS -> playersWorld(player, args);
            case STATUS -> statusWorld(player, args);
            case SPAWN -> spawnWorld(player, args);
            case LOBBY -> manageLobby(player, command, args);
        };
    }

    private boolean manageLobby(Player player, Command command, String[] args) {
        if (!command.testPermission(player)) {
            return true;
        }
        try {
            if (args.length == 3 && args[2].equalsIgnoreCase("clear")) {
                gameWorld.lobby().clearLobbyWorld();
                player.sendMessage(Component.text("로비 설정을 초기화했습니다. 기본 오버월드 스폰을 사용합니다.",
                        NamedTextColor.GREEN));
                return true;
            }
            World destination = player.getWorld();
            if (args.length == 4 && args[2].equalsIgnoreCase("set")) {
                if (!destination.getName().equals(args[3])) {
                    throw new IllegalArgumentException("로비로 지정할 월드의 스폰 위치에 직접 서서 실행해 주세요.");
                }
            } else if (args.length != 2 && !(args.length == 3
                    && (args[2].equalsIgnoreCase("set") || args[2].equalsIgnoreCase("move")))) {
                player.sendMessage(Component.text("사용법: /quake world lobby <set [현재 월드 이름]|move|clear>",
                        NamedTextColor.YELLOW));
                return true;
            }

            validateLobbyDestination(destination);
            if (gameWorld.isGameWorld(destination)) {
                confirmLobbyMove(player, command, destination);
            } else {
                moveLobby(player);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    private void validateLobbyDestination(World world) {
        if (gameWorld.isGameWorld(world) && gameWorld.requireInfo(world.getName()).status() == GameWorldStatus.READY) {
            throw new IllegalStateException("READY 상태인 게임 월드는 로비로 지정할 수 없습니다.");
        }
    }

    private void confirmLobbyMove(Player player, Command command, World destination) {
        var ownerId = player.getUniqueId();
        var destinationId = destination.getUID();
        player.sendMessage(Component.text("현재 게임 월드 " + destination.getName() + "을(를) 로비로 지정할까요? ",
                NamedTextColor.YELLOW).append(Component.text("[확인]", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.callback(audience -> {
                    if (!(audience instanceof Player owner) || !ownerId.equals(owner.getUniqueId())) {
                        return;
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!owner.isOnline() || !command.testPermission(owner)) {
                            return;
                        }
                        if (!destinationId.equals(owner.getWorld().getUID())) {
                            owner.sendMessage(Component.text("월드가 바뀌었습니다. 이전 명령을 다시 실행해 주세요.",
                                    NamedTextColor.RED));
                            return;
                        }
                        try {
                            moveLobby(owner);
                        } catch (IllegalStateException exception) {
                            owner.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
                        }
                    });
                }, ClickCallback.Options.builder().uses(1).build()))));
    }

    private void moveLobby(Player player) {
        var location = player.getLocation();
        validateLobbyDestination(location.getWorld());
        var lobby = gameWorld.lobby();
        var previousSpawn = lobby.spawn();
        var previousPlayers = previousSpawn == null ? List.<Player>of()
                : List.copyOf(previousSpawn.getWorld().getPlayers());
        lobby.setLobbyWorld(location, player.getUniqueId());

        int failed = 0;
        for (Player previousPlayer : previousPlayers) {
            if (game.isRunning() && game.contains(previousPlayer)) {
                continue;
            }
            if (!previousPlayer.teleport(location)) {
                failed++;
            }
        }
        player.sendMessage(Component.text(location.getWorld().getName() + "의 현재 위치와 시선 방향을 로비 스폰으로 저장했습니다.",
                NamedTextColor.GREEN));
        if (failed > 0) {
            player.sendMessage(Component.text("기존 로비에서 " + failed + "명을 이동시키지 못했습니다.",
                    NamedTextColor.YELLOW));
        }
    }

    private boolean createWorld(Player player, String[] args) {
        if (args.length == 2) {
            openCreateWorldDialog(player);
            return true;
        }
        if (args.length != 3) {
            return false;
        }
        try {
            gameWorld.create(args[2], player.getUniqueId());
            player.sendMessage(Component.text("점검 중인 월드를 생성했습니다: " + args[2], NamedTextColor.GREEN));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    private boolean deleteWorld(Player player, String[] args) {
        if (args.length == 2) {
            worldDeleteMenu.open(player);
            return true;
        }
        if (args.length != 3) {
            return false;
        }
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

    private boolean tpWorld(Player player) {
        worldTeleportMenu.open(player);
        return true;
    }

    private boolean playersWorld(Player player, String[] args) {
        if (args.length != 4) {
            return false;
        }
        String worldName = player.getWorld().getName();
        try {
            int min = Integer.parseInt(args[2]);
            int max = Integer.parseInt(args[3]);
            gameWorld.setPlayerLimits(worldName, min, max);
            game.onWorldSettingsChanged();
            player.sendMessage(Component.text("월드 인원 설정을 저장했습니다: " + worldName
                    + " (최소 " + min + "명, 최대 " + max + "명)", NamedTextColor.GREEN));
        } catch (NumberFormatException exception) {
            player.sendMessage(Component.text("최소 인원은 2명 또는 4명, 최대 인원은 2~16 사이의 짝수로 입력해 주세요.", NamedTextColor.RED));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    private boolean statusWorld(Player player, String[] args) {
        if (args.length != 3) {
            return false;
        }
        String worldName = player.getWorld().getName();
        try {
            GameWorldStatus status;
            try {
                status = GameWorldStatus.valueOf(args[2].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("상태는 ready, maintenance, disabled 중 하나로 입력해 주세요.");
            }
            gameWorld.setStatus(worldName, status);
            game.onWorldSettingsChanged();
            player.sendMessage(Component.text("월드 상태를 변경했습니다: " + worldName + " → " + status,
                    NamedTextColor.GREEN));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
        }
        return true;
    }

    private boolean spawnWorld(Player player, String[] args) {
        if (args.length != 3) {
            return false;
        }
        String worldName = player.getWorld().getName();
        try {
            if (game.isRunning() && game.getCurrentWorld().name().equals(worldName)) {
                throw new IllegalArgumentException("게임 진행 중인 월드의 스폰은 변경할 수 없습니다.");
            }
            String team = args[2].toUpperCase(Locale.ROOT);
            gameWorld.setSpawn(worldName, team, player.getLocation());
            player.sendMessage(Component.text(worldName + "의 " + team
                    + " 스폰을 현재 위치와 시선 방향으로 저장했습니다.", NamedTextColor.GREEN));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            player.sendMessage(Component.text(exception.getMessage(), NamedTextColor.RED));
        }
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
        if (!(sender instanceof Player player) || !command.testPermissionSilent(sender)) {
            return List.of();
        }
        if (args.length == 1) {
            return List.of("get", "setTimer", "setScore", "world").stream()
                    .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setTimer")) {
            return List.of(Integer.toString(MAX_TIMER_SECONDS)).stream().filter(option -> option.startsWith(args[1])).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setScore")) {
            return List.of("5").stream().filter(option -> option.startsWith(args[1])).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("get")
                && "book".startsWith(args[1].toLowerCase(Locale.ROOT))) {
            return List.of("book");
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("world")) {
            return completeWorldCommand(player, args);
        }
        return List.of();
    }

    private List<String> completeWorldCommand(Player player, String[] args) {
        if (args.length == 2) {
            return Arrays.stream(WorldSubcommand.values())
                    .map(subcommand -> subcommand.name().toLowerCase(Locale.ROOT))
                    .filter(option -> option.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        WorldSubcommand subcommand = WorldSubcommand.parse(args[1]);
        if (args.length == 3) {
            List<String> options = switch (subcommand) {
                case PLAYERS -> List.of("2", "4");
                case STATUS -> List.of("ready", "maintenance", "disabled");
                case SPAWN -> List.of("red", "blue");
                case LOBBY -> List.of("set", "move", "clear");
                case null, default -> List.of();
            };
            return options.stream().filter(option -> option.startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 4 && subcommand == WorldSubcommand.LOBBY && args[2].equalsIgnoreCase("set")) {
            World world = player.getWorld();
            String name = world.getName();
            if (name.startsWith(args[3]) && (!gameWorld.isGameWorld(world)
                    || gameWorld.requireInfo(name).status() != GameWorldStatus.READY)) {
                return List.of(name);
            }
        }
        return List.of();
    }
}
