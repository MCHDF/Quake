package moe.mchdf.quake.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import moe.mchdf.quake.database.GameWorldRepository;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

public final class GameWorld {
    private static final String VALID_NAME_PATTERN = "[A-Za-z0-9_-]+";

    private final JavaPlugin plugin;
    private final GameWorldRepository repository;
    private final LobbyWorldManager lobby;
    private final Map<String, Map<String, TeamSpawn>> spawns;
    private final Map<String, GameWorldInfo> gameWorlds = new HashMap<>();

    public GameWorld(JavaPlugin plugin, GameWorldRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
        this.lobby = new LobbyWorldManager(repository);
        this.spawns = new HashMap<>(repository.findSpawns());
        repository.findAll().forEach(info -> gameWorlds.put(info.name(), info));
    }

    public LobbyWorldManager lobby() {
        return lobby;
    }

    public void loadAll() {
        for (String name : gameWorlds.keySet()) {
            if (!name.matches(VALID_NAME_PATTERN)) {
                plugin.getLogger().warning("잘못된 게임 월드 이름을 건너뜁니다: " + name);
                continue;
            }
            World loaded = Bukkit.getWorld(name);
            try {
                WorldCreator creator = worldCreator(name);
                if (loaded == null && !prepareWorldFolder(creator, false)) {
                    plugin.getLogger().warning("게임 월드 파일을 찾을 수 없어 로드를 건너뜁니다: " + name);
                    continue;
                }
                World world = loaded != null ? loaded : Bukkit.createWorld(creator);
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
            } catch (IOException | RuntimeException exception) {
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
        WorldCreator creator = worldCreator(name);
        if (Bukkit.getWorld(creator.key()) != null) {
            throw new IllegalArgumentException("이미 로드된 월드입니다: " + name);
        }
        try {
            prepareWorldFolder(creator, true);
        } catch (IOException exception) {
            throw new IllegalStateException("월드 저장 폴더를 준비하지 못했습니다: " + name, exception);
        }
        World world = Bukkit.createWorld(creator);
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

    private WorldCreator worldCreator(String name) {
        return new WorldCreator(name).generator(new VoidWorldGenerator());
    }

    private Path storageFolder(String name) {
        return Path.of("worlds", name).toAbsolutePath().normalize();
    }

    private boolean prepareWorldFolder(WorldCreator creator, boolean creating) throws IOException {
        NamespacedKey key = creator.key();
        if (key.equals(NamespacedKey.minecraft("overworld"))
                || key.equals(NamespacedKey.minecraft("the_nether"))
                || key.equals(NamespacedKey.minecraft("the_end"))) {
            throw new IllegalArgumentException("기본 월드에 예약된 이름입니다: " + creator.name());
        }
        Path container = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path legacy = container.resolve(creator.name());
        Path paperFolder = container.resolve(Bukkit.getUnsafe().getMainLevelName())
                .resolve("dimensions").resolve(key.getNamespace()).resolve(key.getKey());
        Path storage = storageFolder(creator.name());
        boolean paperExists = Files.exists(paperFolder, LinkOption.NOFOLLOW_LINKS);
        boolean storageExists = Files.exists(storage, LinkOption.NOFOLLOW_LINKS);
        boolean legacyExists = Files.isRegularFile(legacy.resolve("level.dat"))
                || Files.isRegularFile(legacy.resolve("level.dat_old"));

        if (creating && (paperExists || storageExists || legacyExists)) {
            throw new IllegalArgumentException("이미 월드 파일이 존재합니다: " + creator.name());
        }
        if (legacyExists && storageExists) {
            throw new IOException("이전 월드와 새 저장 경로가 모두 존재합니다: " + creator.name());
        }
        if (legacyExists) {
            // Let Paper upgrade the legacy format before relocating it on the next load.
            return true;
        }
        if (!creating && !paperExists && !storageExists) {
            return false;
        }
        Path linkTarget = paperFolder.getParent().relativize(storage);
        if (Files.isSymbolicLink(paperFolder)) {
            if (!linkTarget.equals(Files.readSymbolicLink(paperFolder))
                    || !Files.isDirectory(storage, LinkOption.NOFOLLOW_LINKS) || !Files.isSameFile(paperFolder, storage)) {
                throw new IOException("월드 저장 경로가 일치하지 않습니다: " + paperFolder);
            }
            allowLinkTarget(container, linkTarget);
            return true;
        }
        if (storageExists && (paperExists || !Files.isDirectory(storage, LinkOption.NOFOLLOW_LINKS))) {
            throw new IOException("월드 저장 경로가 충돌합니다: " + storage);
        }
        allowLinkTarget(container, linkTarget);
        Files.createDirectories(storage.getParent());
        Files.createDirectories(paperFolder.getParent());
        if (paperExists) {
            Files.move(paperFolder, storage);
        } else if (creating) {
            Files.createDirectory(storage);
        }
        try {
            // Paper resolves dimensions under the primary world. Only Quake dimensions get links.
            Files.createSymbolicLink(paperFolder, linkTarget);
        } catch (IOException | RuntimeException failure) {
            try {
                if (paperExists) {
                    Files.move(storage, paperFolder);
                } else if (creating) {
                    Files.delete(storage);
                }
            } catch (IOException recoveryFailure) {
                failure.addSuppressed(recoveryFailure);
            }
            throw failure;
        }
        return true;
    }

    private void allowLinkTarget(Path container, Path target) throws IOException {
        // Paper validates links before plugins load, so allow this exact target for the next restart.
        Path allowList = container.resolve("allowed_symlinks.txt");
        String entry = "[regex]" + Pattern.quote(target.toString());
        if (!Files.exists(allowList) || !Files.readAllLines(allowList).contains(entry)) {
            Files.writeString(allowList, "\n" + entry + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    public List<World> list() {
        return Bukkit.getWorlds().stream().filter(this::isGameWorld).toList();
    }

    public List<World> listTeleportWorlds(World lobbyWorld) {
        return Bukkit.getWorlds().stream()
                .filter(world -> isTeleportWorld(world) || world.equals(lobbyWorld))
                .sorted(Comparator.comparing(world -> !NamespacedKey.minecraft("overworld").equals(world.getKey())))
                .toList();
    }

    public boolean isTeleportWorld(World world) {
        return NamespacedKey.minecraft("overworld").equals(world.getKey()) || isGameWorld(world);
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
            if (world != null && lobby.isConfiguredWorld(world)) {
                throw new IllegalStateException("로비를 다른 월드로 이전한 뒤 READY 상태로 변경해 주세요.");
            }
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
        if (!isGameWorld(world)) {
            return "일반 월드";
        }
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
        if (NamespacedKey.minecraft("overworld").equals(world.getKey())) {
            throw new IllegalStateException("기본 오버월드는 삭제할 수 없습니다.");
        }
        if (!isGameWorld(world)) {
            throw new IllegalStateException("등록된 게임 월드만 삭제할 수 있습니다: " + world.getName());
        }
        if (lobby.isConfiguredWorld(world)) {
            throw new IllegalStateException("현재 로비입니다. 다른 월드로 로비를 이전한 뒤 삭제해 주세요.");
        }
        if (!world.getPlayers().isEmpty()) {
            throw new IllegalStateException("플레이어가 있는 월드는 삭제할 수 없습니다.");
        }
        String name = world.getName();
        Path folder = world.getWorldFolder().toPath();
        boolean linked = Files.isSymbolicLink(folder);
        Path storage = linked ? storageFolder(name) : folder;
        if (linked) {
            try {
                if (!Files.isDirectory(storage, LinkOption.NOFOLLOW_LINKS) || !Files.isSameFile(folder, storage)) {
                    throw new IOException("월드 저장 경로가 일치하지 않습니다: " + folder);
                }
            } catch (IOException exception) {
                throw new IllegalStateException("월드 저장 경로를 확인하지 못했습니다: " + name, exception);
            }
        }
        if (!Bukkit.unloadWorld(world, true)) {
            return false;
        }
        try {
            repository.remove(name);
        } catch (RuntimeException failure) {
            try {
                World restored = Bukkit.createWorld(worldCreator(name));
                if (restored == null || !isGameWorld(restored)) {
                    throw new IllegalStateException("등록된 월드를 복구하지 못했습니다: " + name);
                }
            } catch (RuntimeException recoveryFailure) {
                failure.addSuppressed(recoveryFailure);
            }
            throw failure;
        }

        // Keep partially deleted folders out of the registry.
        gameWorlds.remove(name);
        spawns.remove(name);
        deleteWorldFolder(name, storage);
        if (linked) {
            try {
                Files.delete(folder);
            } catch (IOException exception) {
                throw new IllegalStateException("월드 저장 링크를 삭제하지 못했습니다: " + folder, exception);
            }
        }
        plugin.getComponentLogger().info("Quake 게임 월드를 삭제했습니다: " + name);
        return true;
    }

    private void deleteWorldFolder(String name, Path folder) {
        try (var paths = Files.walk(folder)) {
            var entries = paths.sorted(Comparator.reverseOrder()).iterator();
            while (entries.hasNext()) {
                Files.deleteIfExists(entries.next());
            }
        } catch (IOException | java.io.UncheckedIOException exception) {
            throw new IllegalStateException(
                    "월드 등록은 삭제했지만 남은 파일을 정리하지 못했습니다: " + name + " (" + folder + ")", exception);
        }
    }
}
