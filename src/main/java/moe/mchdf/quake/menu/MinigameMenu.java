package moe.mchdf.quake.menu;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import java.util.UUID;
import moe.mchdf.quake.game.GameSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class MinigameMenu {
    private final JavaPlugin plugin;
    private final GameSession game;

    public MinigameMenu(JavaPlugin plugin, GameSession game) {
        this.plugin = plugin;
        this.game = game;
    }

    public void open(Player player) {
        UUID playerId = player.getUniqueId();
        boolean waiting = game.contains(player);
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Quake 미니게임", NamedTextColor.GOLD))
                        .canCloseWithEscape(true)
                        .body(List.of(DialogBody.plainMessage(Component.text(
                                game.isRunning() ? (waiting ? "현재 게임에 참여 중입니다." : "게임이 진행 중입니다. 종료 후 참여해 주세요.")
                                        : waiting ? "현재 참여 대기 중입니다." : "참여 버튼을 눌러 미니게임 대기 명단에 등록하세요.",
                                NamedTextColor.WHITE, TextDecoration.BOLD))))
                        .build())
                .type(DialogType.multiAction(List.of(
                        ActionButton.builder(Component.text(waiting ? "참여 대기 중" : "참여하기", NamedTextColor.GREEN, TextDecoration.BOLD))
                                .action(participationAction(playerId, true))
                                .build(),
                        ActionButton.builder(Component.text("참여 취소", NamedTextColor.RED, TextDecoration.BOLD))
                                .action(participationAction(playerId, false))
                                .build()))
                        .columns(2)
                        .exitAction(ActionButton.builder(Component.text("닫기")).build())
                        .build()));
        player.showDialog(dialog);
    }

    private DialogAction participationAction(UUID ownerId, boolean joining) {
        return DialogAction.customClick((response, audience) -> {
            if (!(audience instanceof Player player) || !player.getUniqueId().equals(ownerId)
                    || !plugin.isEnabled()) {
                return;
            }
            // Keep waiting-list mutations and player access on the server thread.
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (joining) {
                    game.join(player);
                } else {
                    boolean removed = game.leave(player);
                    player.sendMessage(Component.text(removed ? "미니게임 참여를 취소했습니다." : "참여 대기 중이 아닙니다.", NamedTextColor.YELLOW, TextDecoration.BOLD));
                }
            });
        }, ClickCallback.Options.builder().uses(1).build());
    }
}
