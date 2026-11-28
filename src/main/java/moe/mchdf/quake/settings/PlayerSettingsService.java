package moe.mchdf.quake.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 접속한 플레이어의 설정을 메모리에 들고 있다가 바뀌면 DB에 저장한다.
 *
 * <p>접속 직전 비동기 단계({@link AsyncPlayerPreLoginEvent})에서 DB를 읽어 두므로, 게임 중에 설정을
 * 확인할 때는 DB를 거치지 않는다. 읽지 못했으면 기본값을 쓴다.
 */
public final class PlayerSettingsService implements Listener {
    private final PlayerSettingsRepository repository;
    private final Logger logger;
    private final Map<UUID, PlayerSettings> cache = new ConcurrentHashMap<>();
    private final List<Consumer<Player>> changeListeners = new ArrayList<>();

    public PlayerSettingsService(PlayerSettingsRepository repository, Logger logger) {
        this.repository = repository;
        this.logger = logger;
    }

    public PlayerSettings get(Player player) {
        return cache.getOrDefault(player.getUniqueId(), PlayerSettings.DEFAULTS);
    }

    /** 설정을 바꿔 저장하고, 바로 적용해야 하는 쪽(사이드바 등)에 알린다. */
    public PlayerSettings update(Player player, UnaryOperator<PlayerSettings> change) {
        PlayerSettings updated = change.apply(get(player));
        cache.put(player.getUniqueId(), updated);
        try {
            repository.save(player.getUniqueId(), updated);
        } catch (IllegalStateException exception) {
            // 저장에 실패해도 이번 접속 동안은 바뀐 설정을 쓴다.
            logger.log(Level.WARNING, exception.getMessage(), exception);
        }
        for (Consumer<Player> listener : changeListeners) {
            listener.accept(player);
        }
        return updated;
    }

    public void addChangeListener(Consumer<Player> listener) {
        changeListeners.add(listener);
    }

    /** 플러그인을 다시 켰을 때처럼 이미 접속해 있는 플레이어의 설정을 읽는다. */
    public void load(Player player) {
        cache.put(player.getUniqueId(), loadOrDefault(player.getUniqueId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            cache.put(event.getUniqueId(), loadOrDefault(event.getUniqueId()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cache.remove(event.getPlayer().getUniqueId());
    }

    private PlayerSettings loadOrDefault(UUID player) {
        try {
            return repository.load(player);
        } catch (IllegalStateException exception) {
            logger.log(Level.WARNING, exception.getMessage(), exception);
            return PlayerSettings.DEFAULTS;
        }
    }
}
