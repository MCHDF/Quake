package moe.mchdf.quake.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Team team = i % 2 == 0 ? red : blue;
            team.addEntry(player.getName());
            previousScoreboards.put(player.getUniqueId(), player.getScoreboard());
            player.setScoreboard(scoreboard);
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

    void remove(Player player) {
        Scoreboard previous = previousScoreboards.remove(player.getUniqueId());
        if (previous == null) {
            return;
        }
        Team team = scoreboard.getEntryTeam(player.getName());
        if (team != null) {
            team.removeEntry(player.getName());
        }
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
            scoreboard = null;
        }
    }
}
