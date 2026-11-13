package moe.mchdf.quake.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

public final class GameWorld {
    private static final String VALID_NAME_PATTERN = "[A-Za-z0-9_-]+";

    private final JavaPlugin plugin;
    private final Set<String> gameWorlds = new HashSet<>();

    public GameWorld(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public World create(String name) {
        if (!name.matches(VALID_NAME_PATTERN)) {
            throw new IllegalArgumentException("월드 이름은 영문, 숫자, _, -만 사용할 수 있습니다.");
        }
        World existing = Bukkit.getWorld(name);
        if (existing != null) {
            throw new IllegalArgumentException("이미 로드된 월드입니다: " + name);
        }
        World world = Bukkit.createWorld(new WorldCreator(name)
                .generator(new VoidWorldGenerator()));
        if (world == null) {
            throw new IllegalStateException("월드를 생성하지 못했습니다: " + name);
        }
        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        world.setTime(6000);
        world.setSpawnLocation(0, 65, 0);
        gameWorlds.add(world.getName());
        plugin.getComponentLogger().info("Quake 게임 월드를 생성했습니다: " + name);
        return world;
    }

    public List<World> list() {
        return Bukkit.getWorlds();
    }

    public boolean isGameWorld(World world) {
        return gameWorlds.contains(world.getName());
    }

    public boolean delete(World world) {
        if (!Bukkit.unloadWorld(world, false)) {
            return false;
        }
        gameWorlds.remove(world.getName());
        try {
            Path folder = world.getWorldFolder().toPath();
            try (var paths = Files.walk(folder)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException exception) {
                        throw new WorldDeletionException(exception);
                    }
                });
            }
        } catch (WorldDeletionException exception) {
            throw new IllegalStateException("월드 파일을 삭제하지 못했습니다: " + world.getName(), exception.getCause());
        } catch (IOException exception) {
            throw new IllegalStateException("월드 파일을 읽지 못했습니다: " + world.getName(), exception);
        }
        plugin.getComponentLogger().info("Quake 게임 월드를 삭제했습니다: " + world.getName());
        return true;
    }

    private static final class WorldDeletionException extends RuntimeException {
        private WorldDeletionException(IOException cause) {
            super(cause);
        }
    }
}
