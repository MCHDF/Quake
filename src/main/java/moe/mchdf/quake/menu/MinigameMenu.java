package moe.mchdf.quake.menu;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class MinigameMenu implements Listener {
    private static final int MIN_PLAYERS = 2;
    private static final int COUNTDOWN_SECONDS = 30;
    private final JavaPlugin plugin;
    private final NamespacedKey bookKey;
    // Waiting registrations last until the player disconnects or the plugin stops.
    private final Set<UUID> waitingPlayers = new HashSet<>();
    private final BossBar waitingBar = BossBar.bossBar(
            Component.text("게임 시작 최소 인원 0 / " + MIN_PLAYERS), 0,
            BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    private BukkitTask countdownTask;
    // -1 means waiting for participants; 0 means the display-only countdown finished.
    private int secondsRemaining = -1;

    public MinigameMenu(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bookKey = new NamespacedKey(plugin, "minigame_menu_book");
    }

    public void giveMenuBook(Player player) {
        giveMenuBook(player, false);
    }

    public void giveMenuBook(Player player, boolean notify) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isMenuBook(item)) {
                if (notify) {
                    player.sendMessage(Component.text("이미 미니게임 메뉴 책을 가지고 있습니다.", NamedTextColor.YELLOW));
                }
                return;
            }
        }
        int slot = player.getInventory().firstEmpty();
        if (slot == -1) {
            player.sendMessage(Component.text(notify
                    ? "미니게임 책을 받을 공간이 없습니다. 인벤토리를 비우고 명령어를 다시 실행해 주세요."
                    : "미니게임 책을 받을 공간이 없습니다. 인벤토리를 비우고 재접속해 주세요.", NamedTextColor.YELLOW));
            return;
        }
        ItemStack book = item(Material.BOOK, "미니게임 메뉴", NamedTextColor.GOLD,
                "우클릭하여 미니게임 참여 메뉴를 엽니다.");
        book.editMeta(meta -> meta.getPersistentDataContainer().set(bookKey, PersistentDataType.BYTE, (byte) 1));
        player.getInventory().setItem(slot, book);
        if (notify) {
            player.sendMessage(Component.text("미니게임 메뉴 책을 지급했습니다.", NamedTextColor.GREEN));
        }
    }

    private boolean isMenuBook(ItemStack item) {
        return item != null && item.getType() == Material.BOOK
                && item.getPersistentDataContainer().has(bookKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!event.getPlayer().isOp() && isMenuBook(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("미니게임 메뉴 책은 버릴 수 없습니다.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        giveMenuBook(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        leaveWaitingList(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if ((event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)
                || !isMenuBook(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        open(event.getPlayer());
    }

    private void open(Player player) {
        UUID playerId = player.getUniqueId();
        boolean waiting = waitingPlayers.contains(playerId);
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Quake 미니게임", NamedTextColor.GOLD))
                        .canCloseWithEscape(true)
                        .body(List.of(DialogBody.plainMessage(Component.text(
                                waiting ? "현재 참여 대기 중입니다." : "참여 버튼을 눌러 미니게임 대기 명단에 등록하세요.",
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
                    boolean added = waitingPlayers.add(ownerId);
                    if (added) {
                        updateAfterJoin();
                        player.showBossBar(waitingBar);
                    }
                    player.sendMessage(Component.text(added ? "미니게임 참여 대기 명단에 등록했습니다." : "이미 참여 대기 중입니다.", NamedTextColor.GREEN,TextDecoration.BOLD));
                } else {
                    boolean removed = leaveWaitingList(player);
                    player.sendMessage(Component.text(removed ? "미니게임 참여를 취소했습니다." : "참여 대기 중이 아닙니다.", NamedTextColor.YELLOW,TextDecoration.BOLD));
                }
            });
        }, ClickCallback.Options.builder().uses(1).build());
    }

    private void updateAfterJoin() {
        if (waitingPlayers.size() >= MIN_PLAYERS && secondsRemaining == -1) {
            secondsRemaining = COUNTDOWN_SECONDS;
            showCountdown();
            countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                secondsRemaining--;
                showCountdown();
                if (secondsRemaining == 0) {
                    countdownTask.cancel();
                    countdownTask = null;
                }
            }, 20L, 20L);
        } else if (secondsRemaining == -1) {
            showPlayerCount();
        }
    }

    private boolean leaveWaitingList(Player player) {
        if (!waitingPlayers.remove(player.getUniqueId())) {
            return false;
        }
        player.hideBossBar(waitingBar);
        // Any participant leaving resets the countdown, even if the minimum is still met.
        cancelCountdown();
        showPlayerCount();
        return true;
    }

    private void showPlayerCount() {
        waitingBar.name(Component.text("게임 시작 최소 인원 " + waitingPlayers.size() + " / " + MIN_PLAYERS));
        waitingBar.progress(Math.min(1.0f, waitingPlayers.size() / (float) MIN_PLAYERS));
        waitingBar.color(BossBar.Color.GREEN);
    }

    private void showCountdown() {
        waitingBar.name(Component.text("게임을 시작합니다! | 시작까지 " + secondsRemaining + "초"));
        waitingBar.progress(secondsRemaining / (float) COUNTDOWN_SECONDS);
        waitingBar.color(BossBar.Color.YELLOW);
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        secondsRemaining = -1;
    }

    public void shutdown() {
        cancelCountdown();
        for (UUID playerId : waitingPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.hideBossBar(waitingBar);
            }
        }
        waitingPlayers.clear();
    }

    private static ItemStack item(Material material, String name, NamedTextColor color, String description) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text(description, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        });
        return item;
    }
}
