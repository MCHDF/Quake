package moe.mchdf.quake.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import moe.mchdf.quake.scoreboard.PlayerBoards;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class GameTeams {
    /** 팀 표시 색. 이름표, 채팅, 탭 목록, 보스바 글자 등 팀 색이 쓰이는 모든 곳의 기준. */
    public static final NamedTextColor RED_COLOR = NamedTextColor.RED;
    public static final NamedTextColor BLUE_COLOR = NamedTextColor.AQUA;
    private static final String KILLS_OBJECTIVE = "kills";

    private final PlayerBoards boards;
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Map<String, Integer> teamScores = new HashMap<>();
    // 처치 수는 플러그인이 직접 보관하고, 스코어보드 objective는 탭 목록 표시용으로만 쓴다.
    private final Map<UUID, Integer> kills = new HashMap<>();
    private boolean active;
    private int targetScore = 5;
    private Runnable targetReachedListener;
    private Consumer<GameEvent.PointScored> pointScoredListener;

    /**
     * 팀 표시는 플레이어마다 가진 개인 스코어보드({@link PlayerBoards})에 등록한다.
     * 사이드바도 같은 스코어보드를 쓰므로 게임 중에 스코어보드를 바꿔 끼우지 않는다.
     */
    GameTeams(PlayerBoards boards) {
        this.boards = boards;
    }

    void assign(Collection<UUID> participants) {
        clear();
        var players = new ArrayList<Player>();
        for (UUID id : participants) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                players.add(player);
            }
        }
        if (players.size() < 2) {
            throw new IllegalStateException("At least two players are required for teams");
        }
        Collections.shuffle(players);
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            playerTeams.put(player.getUniqueId(), i % 2 == 0 ? "RED" : "BLUE");
            kills.put(player.getUniqueId(), 0);
        }
        teamScores.put("RED", 0);
        teamScores.put("BLUE", 0);
        active = true;
        // 참가자 각자의 스코어보드에 두 팀과 전체 참가자를 등록해야 팀 색과 이름표 숨김이 보인다.
        // 참가자가 아닌 플레이어의 스코어보드에는 등록하지 않으므로 그들은 기본 탭 목록을 그대로 본다.
        for (Player viewer : players) {
            Scoreboard board = boards.boardOf(viewer);
            Team red = createTeam(board, "RED", RED_COLOR);
            Team blue = createTeam(board, "BLUE", BLUE_COLOR);
            Objective objective = board.registerNewObjective(KILLS_OBJECTIVE, Criteria.DUMMY, Component.text("처치"));
            objective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            for (Player member : players) {
                ("RED".equals(getTeamOf(member)) ? red : blue).addEntry(member.getName());
                objective.getScore(member.getName()).setScore(0);
            }
        }
        updateTabListFooter();
    }

    public String getTeamOf(Player player) {
        if (player == null) {
            return null;
        }
        return playerTeams.get(player.getUniqueId());
    }

    public boolean areEnemies(Player a, Player b) {
        if (a == null || b == null) {
            return false;
        }
        String teamA = getTeamOf(a);
        String teamB = getTeamOf(b);
        return teamA != null && teamB != null && !teamA.equals(teamB);
    }

    public void setTargetScore(int score) {
        if (score <= 0) {
            throw new IllegalArgumentException("Target score must be positive");
        }
        targetScore = score;
    }

    public void setTargetReachedListener(Runnable listener) {
        targetReachedListener = listener;
    }

    /** 득점할 때마다 호출된다. 알림 문구와 화면 갱신은 이 이벤트를 받는 쪽이 담당한다. */
    void setPointScoredListener(Consumer<GameEvent.PointScored> listener) {
        pointScoredListener = listener;
    }

    public void awardPoint(Player shooter, Player target) {
        String team = getTeamOf(shooter);
        String targetTeam = getTeamOf(target);
        if (team == null || target == null || targetTeam == null) {
            return;
        }
        int newScore = teamScores.getOrDefault(team, 0) + 1;
        teamScores.put(team, newScore);
        int shooterKills = kills.merge(shooter.getUniqueId(), 1, Integer::sum);
        for (Scoreboard board : participantBoards()) {
            Objective objective = board.getObjective(KILLS_OBJECTIVE);
            if (objective != null) {
                objective.getScore(shooter.getName()).setScore(shooterKills);
            }
        }
        updateTabListFooter();
        if (pointScoredListener != null) {
            pointScoredListener.accept(new GameEvent.PointScored(shooter, team, target, targetTeam,
                    List.copyOf(playerTeams.keySet())));
        }

        if (newScore >= targetScore && targetReachedListener != null) {
            targetReachedListener.run();
        }
    }

    /** 이번 게임에서 이 플레이어가 맞힌(처치한) 횟수 */
    public int getKills(Player player) {
        return player == null ? 0 : kills.getOrDefault(player.getUniqueId(), 0);
    }

    int getTeamScore(String teamName) {
        if (teamName == null) {
            return 0;
        }
        return teamScores.getOrDefault(teamName, 0);
    }

    GameTeams getGameTeams() {
        return this;
    }

    private static Team createTeam(Scoreboard board, String name, NamedTextColor color) {
        Team team = board.registerNewTeam(name);
        team.displayName(Component.text(name, color));
        team.color(color);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
        return team;
    }

    public static NamedTextColor colorOf(String team) {
        return "RED".equals(team) ? RED_COLOR : BLUE_COLOR;
    }

    /** 지금 접속해 있는 참가자들의 개인 스코어보드 */
    private List<Scoreboard> participantBoards() {
        List<Scoreboard> result = new ArrayList<>();
        for (UUID id : playerTeams.keySet()) {
            Scoreboard board = boards.find(id);
            if (board != null) {
                result.add(board);
            }
        }
        return result;
    }

    void updateTabListFooter() {
        if (!active) {
            return;
        }
        int redTotal = getTeamScore("RED");
        int blueTotal = getTeamScore("BLUE");

        Component header = Component.text("QUAKE", NamedTextColor.GOLD, TextDecoration.BOLD)
                .appendNewline()
                .append(Component.text("RED", RED_COLOR))
                .append(Component.text(" vs ", NamedTextColor.GRAY))
                .append(Component.text("BLUE", BLUE_COLOR))
                .appendNewline();

        playerTeams.forEach((id, team) -> {
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                return;
            }
            Component footer = Component.text("RED ", RED_COLOR)
                    .append(Component.text(String.valueOf(redTotal), NamedTextColor.WHITE))
                    .append(Component.text(" | ", NamedTextColor.GRAY))
                    .append(Component.text("BLUE ", BLUE_COLOR))
                    .append(Component.text(String.valueOf(blueTotal), NamedTextColor.WHITE))
                    .appendNewline()
                    .appendNewline()
                    .append(Component.text("내 팀: ", NamedTextColor.GRAY))
                    .append(Component.text(team, colorOf(team)))
                    .appendNewline();
            player.sendPlayerListHeaderAndFooter(header, footer);
        });
    }

    void remove(Player player) {
        if (playerTeams.remove(player.getUniqueId()) == null) {
            return;
        }
        Scoreboard own = boards.find(player.getUniqueId());
        if (own != null) {
            removeTeamDisplay(own);
        }
        // 남은 참가자들의 스코어보드에서도 이 플레이어를 뺀다.
        for (Scoreboard board : participantBoards()) {
            Team team = board.getEntryTeam(player.getName());
            if (team != null) {
                team.removeEntry(player.getName());
            }
            Objective objective = board.getObjective(KILLS_OBJECTIVE);
            if (objective != null) {
                objective.getScore(player.getName()).resetScore();
            }
        }
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
    }

    boolean hasEmptyTeam() {
        if (!active) {
            return false;
        }
        return !playerTeams.containsValue("RED") || !playerTeams.containsValue("BLUE");
    }

    void clear() {
        for (UUID id : new ArrayList<>(playerTeams.keySet())) {
            Scoreboard board = boards.find(id);
            if (board != null) {
                removeTeamDisplay(board);
            }
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            }
        }
        playerTeams.clear();
        teamScores.clear();
        kills.clear();
        active = false;
        pointScoredListener = null;
        targetReachedListener = null;
    }

    /** 개인 스코어보드에서 게임용 팀과 처치 수 objective만 지운다. 사이드바는 그대로 둔다. */
    private static void removeTeamDisplay(Scoreboard board) {
        for (String name : List.of("RED", "BLUE")) {
            Team team = board.getTeam(name);
            if (team != null) {
                team.unregister();
            }
        }
        Objective objective = board.getObjective(KILLS_OBJECTIVE);
        if (objective != null) {
            objective.unregister();
        }
    }
}
