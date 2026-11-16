package moe.mchdf.quake.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import moe.mchdf.quake.database.GameWorldRepository;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

public final class GameWorld {
    private static final String VALID_NAME_PATTERN = "[A-Za-z0-9_-]+";

    private final JavaPlugin plugin;
    private final GameWorldRepository repository;
    private final Map<String, Map<String, TeamSpawn>> spawns;
    private final Map<String, GameWorldInfo> gameWorlds = new HashMap<>();

    public GameWorld(JavaPlugin plugin, GameWorldRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        this.spawns = new HashMap<>(repository.findSpawns());
        repository.findAll().forEach(info -> gameWorlds.put(info.name(), info));
    }

    public void loadAll() {
        for (String name : gameWorlds.keySet()) {
            if (!name.matches(VALID_NAME_PATTERN)) {
                plugin.getLogger().warning("잘못된 게임 월드 이름을 건너뜁니다: " + name);
                continue;
            }
            World loaded = Bukkit.getWorld(name);
            Path folder = Bukkit.getWorldContainer().toPath().resolve(name);
            if (loaded == null && !Files.isRegularFile(folder.resolve("level.dat"))) {
                plugin.getLogger().warning("게임 월드 파일을 찾을 수 없어 로드를 건너뜁니다: " + name);
                continue;
            }
            try {
                World world = loaded != null ? loaded
                        : Bukkit.createWorld(new WorldCreator(name).generator(new VoidWorldGenerator()));
                if (world == null) {
                    throw new IllegalStateException("월드 로드 결과가 없습니다.");
                }
                GameWorldInfo info = gameWorlds.get(name);
                if (info.worldUuid() == null) {
                    repository.fillWorldUuid(name, world.getUID());
                    gameWorlds.put(name, info.withWorldUuid(world.getUID()));
                } else if (!info.worldUuid().equals(world.getUID())) {
                    plugin.getLogger().warning("등록된 UUID와 다른 월드입니다. 게임 월드에서 제외합니다: " + name);
                }
            } catch (RuntimeException exception) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "게임 월드를 불러오지 못했습니다: " + name, exception);
            }
        }
    }

    public World create(String name, UUID createdBy) {
        if (!name.matches(VALID_NAME_PATTERN)) {
            throw new IllegalArgumentException("월드 이름은 영문, 숫자, _, -만 사용할 수 있습니다.");
        }
        if (gameWorlds.containsKey(name)) {
            throw new IllegalArgumentException("이미 등록된 게임 월드입니다: " + name);
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
        try {
            world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
            world.setTime(6000);
            world.setSpawnLocation(0, 65, 0);
            world.save(true);
            GameWorldInfo info = new GameWorldInfo(world.getName(), world.getUID(), world.getName(),
                    GameWorldStatus.MAINTENANCE, Instant.now(), createdBy);
            repository.add(info);
            gameWorlds.put(world.getName(), info);
            plugin.getComponentLogger().info("Quake 게임 월드를 생성했습니다: " + name);
            return world;
        } catch (RuntimeException exception) {
            Bukkit.unloadWorld(world, true);
            throw exception;
        }
    }

    public List<World> list() {
        return Bukkit.getWorlds().stream().filter(this::isGameWorld).toList();
    }

    public boolean isGameWorld(World world) {
        GameWorldInfo info = gameWorlds.get(world.getName());
        return info != null && world.getUID().equals(info.worldUuid());
    }

    public List<GameWorldInfo> readyWorlds() {
        return gameWorlds.values().stream()
                .filter(info -> info.status() == GameWorldStatus.READY && hasSpawns(info.name()))
                .filter(info -> {
                    World world = Bukkit.getWorld(info.name());
                    return world != null && isGameWorld(world);
                })
                .sorted(Comparator.comparingLong(GameWorldInfo::rotationOrder).thenComparing(GameWorldInfo::name))
                .toList();
    }

    private boolean hasSpawns(String name) {
        Map<String, TeamSpawn> points = spawns.getOrDefault(name, Map.of());
        return points.containsKey("RED") && points.containsKey("BLUE");
    }

    public void setStatus(String name, GameWorldStatus status) {
        GameWorldInfo info = requireInfo(name);
        if (status == GameWorldStatus.READY) {
            World world = Bukkit.getWorld(name);
            if (world == null || !isGameWorld(world) || !hasSpawns(name)) {
                throw new IllegalArgumentException("준비 상태로 전환하려면 월드를 로드하고 양 팀 스폰을 설정해야 합니다.");
            }
        }
        long order = repository.setStatus(name, status);
        gameWorlds.put(name, info.withStatus(status, order));
    }

    public void setSpawn(String name, String team, Location location) {
        GameWorldInfo info = requireInfo(name);
        if (!team.equals("RED") && !team.equals("BLUE")) {
            throw new IllegalArgumentException("팀은 red 또는 blue로 입력해 주세요.");
        }
        if (location.getWorld() == null || !location.getWorld().getUID().equals(info.worldUuid())) {
            throw new IllegalArgumentException("스폰을 설정할 게임 월드 안에서 실행해 주세요.");
        }
        if (info.status() != GameWorldStatus.MAINTENANCE) {
            throw new IllegalArgumentException("스폰 변경 전 월드를 점검 상태로 전환해 주세요.");
        }
        TeamSpawn spawn = new TeamSpawn(location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
        repository.setSpawn(name, team, spawn);
        spawns.computeIfAbsent(name, ignored -> new HashMap<>()).put(team, spawn);
    }

    public Location teamSpawn(String name, String team) {
        World world = Bukkit.getWorld(name);
        TeamSpawn spawn = spawns.getOrDefault(name, Map.of()).get(team);
        if (world == null || !isGameWorld(world) || spawn == null) {
            throw new IllegalStateException("팀 스폰을 찾을 수 없습니다: " + name + " / " + team);
        }
        return spawn.location(world);
    }

    public GameWorldInfo requireInfo(String name) {
        GameWorldInfo info = gameWorlds.get(name);
        if (info == null) {
            throw new IllegalArgumentException("등록된 게임 월드를 찾을 수 없습니다: " + name);
        }
        return info;
    }

    public String statusLabel(World world) {
        return switch (requireInfo(world.getName()).status()) {
            case READY -> "준비 완료";
            case MAINTENANCE -> "점검 중";
            case DISABLED -> "사용 중지";
        };
    }

    public void setPlayerLimits(String name, int minPlayers, int maxPlayers) {
        GameWorldInfo info = gameWorlds.get(name);
        if (info == null) {
            throw new IllegalArgumentException("등록된 게임 월드를 찾을 수 없습니다: " + name);
        }
        GameWorldInfo updated = info.withPlayerLimits(minPlayers, maxPlayers);
        repository.setPlayerLimits(name, minPlayers, maxPlayers);
        gameWorlds.put(name, updated);
    }

    public String displayName(World world) {
        GameWorldInfo info = gameWorlds.get(world.getName());
        return info == null ? world.getName() : info.displayName();
    }

    public void saveAll() {
        for (String worldName : gameWorlds.keySet()) {
            World world = Bukkit.getWorld(worldName);
            if (world != null && isGameWorld(world)) {
                world.save(true);
                plugin.getComponentLogger().info("Quake 게임 월드를 저장했습니다: " + worldName);
            }
        }
    }

    public boolean delete(World world) {
        if (!isGameWorld(world)) {
            throw new IllegalStateException("등록된 게임 월드만 삭제할 수 있습니다: " + world.getName());
        }
        if (!world.getPlayers().isEmpty()) {
            throw new IllegalStateException("플레이어가 있는 월드는 삭제할 수 없습니다.");
        }
        if (!Bukkit.unloadWorld(world, false)) {
            return false;
        }
        // Deregister before removing files so a partial deletion cannot be loaded on restart.
        repository.remove(world.getName());
        gameWorlds.remove(world.getName());
        spawns.remove(world.getName());
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
