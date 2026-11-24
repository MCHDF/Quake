package moe.mchdf.quake.menu;

import java.util.UUID;
import moe.mchdf.quake.game.GameEvent;
import moe.mchdf.quake.game.GameListener;
import moe.mchdf.quake.game.GameTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** 게임 이벤트를 받아 채팅 안내 메시지를 보낸다. */
public final class GameChatNotifier implements GameListener {

    @Override
    public void onGameEvent(GameEvent event) {
        switch (event) {
            case GameEvent.TeamAssigned e -> e.player().sendMessage(
                    Component.text(e.team() + " 팀에 배정되었습니다.", GameTeams.colorOf(e.team())));
            case GameEvent.PointScored e -> {
                Component message = pointMessage(e);
                for (UUID id : e.recipients()) {
                    Player player = Bukkit.getPlayer(id);
                    if (player != null) {
                        player.sendMessage(message);
                    }
                }
            }
            case GameEvent.TeleportFailed e -> {
                for (UUID id : e.applicants()) {
                    Player player = Bukkit.getPlayer(id);
                    if (player != null) {
                        player.sendMessage(Component.text("월드 이동에 실패하여 다음 월드를 선택합니다.", NamedTextColor.RED));
                    }
                }
            }
            case GameEvent.NotAdmitted e -> e.player().sendMessage(
                    Component.text("이번 월드의 정원을 초과하여 다음 게임을 기다립니다.", NamedTextColor.YELLOW));
            case GameEvent.GameFinished e -> e.player().sendMessage(Component.text(e.cancelled()
                    ? "참여 인원 부족으로 게임이 취소되었습니다."
                    : "게임이 종료되었습니다.", NamedTextColor.GOLD));
            case GameEvent.AutoJoined e -> e.player().sendMessage(Component.text(e.previousGameCancelled()
                    ? "이전 게임이 인원 부족으로 취소되어 다음 게임에 자동 참여했습니다."
                    : "이전 게임이 종료되어 다음 게임에 자동 참여했습니다.", NamedTextColor.GREEN));
            default -> {
                // 보스바 등 채팅과 관계없는 이벤트
            }
        }
    }

    private static Component pointMessage(GameEvent.PointScored e) {
        return Component.text("[", NamedTextColor.GRAY)
                .append(Component.text(e.shooterTeam(), GameTeams.colorOf(e.shooterTeam()), TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.GRAY))
                .append(Component.text(e.shooter().getName(), NamedTextColor.WHITE))
                .append(Component.text(" -> ", NamedTextColor.GRAY))
                .append(Component.text("[", NamedTextColor.GRAY))
                .append(Component.text(e.targetTeam(), GameTeams.colorOf(e.targetTeam()), TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.GRAY))
                .append(Component.text(e.target().getName(), NamedTextColor.WHITE));
    }
}
