package moe.mchdf.quake.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

    private final Map<UUID, Scoreboard> previousScoreboards = new HashMap<>();
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Map<String, Integer> teamScores = new HashMap<>();
    // 처치 수는 플러그인이 직접 보관하고, 스코어보드 objective는 탭 목록 표시용으로만 쓴다.
    private final Map<UUID, Integer> kills = new HashMap<>();
    private Objective killsObjective;
    private Scoreboard scoreboard;
    private int targetScore = 5;
    private Runnable targetReachedListener;
    private Runnable scoreChangedListener;

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
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Team red = createTeam("RED", RED_COLOR);
        Team blue = createTeam("BLUE", BLUE_COLOR);
        teamScores.put("RED", 0);
        teamScores.put("BLUE", 0);
        // The scoreboard is only set for participants, so everyone else keeps the default tab list.
        killsObjective = scoreboard.registerNewObjective("kills", Criteria.DUMMY, Component.text("처치"));
        killsObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Team team = i % 2 == 0 ? red : blue;
            team.addEntry(player.getName());
            previousScoreboards.put(player.getUniqueId(), player.getScoreboard());
            playerTeams.put(player.getUniqueId(), team.getName());
            player.setScoreboard(scoreboard);
            kills.put(player.getUniqueId(), 0);
            killsObjective.getScore(player.getName()).setScore(0);
            updateTabListFooter();
            player.sendMessage(Component.text(team.getName() + " 팀에 배정되었습니다.", team.color()));
        }
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

    void setScoreChangedListener(Runnable listener) {
        scoreChangedListener = listener;
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
        if (killsObjective != null) {
            killsObjective.getScore(shooter.getName()).setScore(shooterKills);
        }
        if (scoreChangedListener != null) {
            scoreChangedListener.run();
        }
        updateTabListFooter();

        NamedTextColor shooterColor = colorOf(team);
        NamedTextColor targetColor = colorOf(targetTeam);
        Component message = Component.text("[", NamedTextColor.GRAY)
                .append(Component.text(team, shooterColor, TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.GRAY))
                .append(Component.text(shooter.getName(), NamedTextColor.WHITE))
                .append(Component.text(" -> ", NamedTextColor.GRAY))
                .append(Component.text("[", NamedTextColor.GRAY))
                .append(Component.text(targetTeam, targetColor, TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.GRAY))
                .append(Component.text(target.getName(), NamedTextColor.WHITE));

        for (UUID id : new ArrayList<>(playerTeams.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.sendMessage(message);
            }
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

    private Team createTeam(String name, NamedTextColor color) {
        Team team = scoreboard.registerNewTeam(name);
        team.displayName(Component.text(name, color));
        team.color(color);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
        return team;
    }

    public static NamedTextColor colorOf(String team) {
        return "RED".equals(team) ? RED_COLOR : BLUE_COLOR;
    }

    void updateTabListFooter() {
        if (scoreboard == null) {
            return;
        }
        Team red = null;
        Team blue = null;
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().equals("RED")) {
                red = team;
            } else if (team.getName().equals("BLUE")) {
                blue = team;
            }
        }
        if (red == null || blue == null) {
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

        for (String entry : scoreboard.getEntries()) {
            Player player = Bukkit.getPlayerExact(entry);
            if (player == null || player.getScoreboard() != scoreboard) {
                continue;
            }
            Team playerTeam = scoreboard.getEntryTeam(entry);
            if (playerTeam == null) {
                continue;
            }
            Component footer = Component.text("RED ", RED_COLOR)
                    .append(Component.text(String.valueOf(redTotal), NamedTextColor.WHITE))
                    .append(Component.text(" | ", NamedTextColor.GRAY))
                    .append(Component.text("BLUE ", BLUE_COLOR))
                    .append(Component.text(String.valueOf(blueTotal), NamedTextColor.WHITE))
                    .appendNewline()
                    .appendNewline()
                    .append(Component.text("내 팀: ", NamedTextColor.GRAY))
                    .append(Component.text(playerTeam.getName(), playerTeam.color()))
                    .appendNewline();
            player.sendPlayerListHeaderAndFooter(header, footer);
        }
    }

    void remove(Player player) {
        Scoreboard previous = previousScoreboards.remove(player.getUniqueId());
        playerTeams.remove(player.getUniqueId());
        if (previous == null) {
            return;
        }
        Team team = scoreboard.getEntryTeam(player.getName());
        if (team != null) {
            team.removeEntry(player.getName());
        }
        scoreboard.resetScores(player.getName());
        player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
        if (player.getScoreboard() == scoreboard) {
            player.setScoreboard(previous);
        }
    }

    boolean hasEmptyTeam() {
        if (scoreboard == null) {
            return false;
        }
        return !playerTeams.containsValue("RED") || !playerTeams.containsValue("BLUE");
    }

    void clear() {
        for (UUID id : new ArrayList<>(previousScoreboards.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                remove(player);
            }
        }
        previousScoreboards.clear();
        playerTeams.clear();
        teamScores.clear();
        kills.clear();
        killsObjective = null;
        scoreChangedListener = null;
        targetReachedListener = null;
        if (scoreboard != null) {
            for (Team team : scoreboard.getTeams()) {
                team.unregister();
            }
            for (Objective objective : scoreboard.getObjectives()) {
                objective.unregister();
            }
            scoreboard = null;
        }
    }
}
