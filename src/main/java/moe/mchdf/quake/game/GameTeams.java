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

final class GameTeams {
    private final Map<UUID, Scoreboard> previousScoreboards = new HashMap<>();
    private Scoreboard scoreboard;

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
        Team red = createTeam("RED", NamedTextColor.RED);
        Team blue = createTeam("BLUE", NamedTextColor.BLUE);
        // The scoreboard is only set for participants, so everyone else keeps the default tab list.
        Objective kills = scoreboard.registerNewObjective("kills", Criteria.PLAYER_KILL_COUNT, Component.text("처치"));
        kills.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Team team = i % 2 == 0 ? red : blue;
            team.addEntry(player.getName());
            previousScoreboards.put(player.getUniqueId(), player.getScoreboard());
            player.setScoreboard(scoreboard);
            kills.getScore(player.getName()).setScore(0);
            updateTabListFooter();
            player.sendMessage(Component.text(team.getName() + " 팀에 배정되었습니다.", team.color()));
        }
    }

    private Team createTeam(String name, NamedTextColor color) {
        Team team = scoreboard.registerNewTeam(name);
        team.displayName(Component.text(name, color));
        team.color(color);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);
        return team;
    }

    private int getTeamKillTotal(Team team) {
        int total = 0;
        for (String entry : team.getEntries()) {
            Player player = Bukkit.getPlayerExact(entry);
            if (player != null) {
                total += player.getStatistic(org.bukkit.Statistic.PLAYER_KILLS);
            }
        }
        return total;
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
        int redTotal = getTeamKillTotal(red);
        int blueTotal = getTeamKillTotal(blue);

        Component header = Component.text("QUAKE", NamedTextColor.GOLD, TextDecoration.BOLD)
                .appendNewline()
                .append(Component.text("RED", NamedTextColor.RED))
                .append(Component.text(" vs ", NamedTextColor.GRAY))
                .append(Component.text("BLUE", NamedTextColor.BLUE))
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
            Component footer = Component.text("RED ", NamedTextColor.RED)
                    .append(Component.text(String.valueOf(redTotal), NamedTextColor.WHITE))
                    .append(Component.text(" | ", NamedTextColor.GRAY))
                    .append(Component.text("BLUE ", NamedTextColor.BLUE))
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
        for (Team team : scoreboard.getTeams()) {
            if (team.getEntries().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    void clear() {
        for (UUID id : new ArrayList<>(previousScoreboards.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                remove(player);
            }
        }
        previousScoreboards.clear();
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
