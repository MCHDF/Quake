package moe.mchdf.quake;

import java.util.Objects;
import moe.mchdf.quake.command.QuakeCommand;
import moe.mchdf.quake.command.SettingsCommand;
import moe.mchdf.quake.database.DatabaseManager;
import moe.mchdf.quake.database.GameWorldRepository;
import moe.mchdf.quake.game.GameSession;
import moe.mchdf.quake.listener.PlayerListener;
import moe.mchdf.quake.menu.GameChatNotifier;
import moe.mchdf.quake.menu.LobbyItems;
import moe.mchdf.quake.menu.MenuBook;
import moe.mchdf.quake.menu.MinigameMenu;
import moe.mchdf.quake.menu.SettingsMenu;
import moe.mchdf.quake.menu.WorldDeleteMenu;
import moe.mchdf.quake.menu.WorldTeleportMenu;
import moe.mchdf.quake.mod.ModHandshake;
import moe.mchdf.quake.mod.ModHitMarker;
import moe.mchdf.quake.mod.ModPlayerSettings;
import moe.mchdf.quake.mod.ModSettingsScreen;
import moe.mchdf.quake.mod.ModSidebar;
import moe.mchdf.quake.scoreboard.PlayerBoards;
import moe.mchdf.quake.scoreboard.ServerSidebar;
import moe.mchdf.quake.settings.PlayerSettingsRepository;
import moe.mchdf.quake.settings.PlayerSettingsService;
import moe.mchdf.quake.world.GameWorld;
import moe.mchdf.quake.world.GameWorldListener;
import moe.mchdf.quake.world.LobbyArrival;
import moe.mchdf.quake.world.LobbyWorldManager;
import moe.mchdf.quake.weapon.RailGunListener;
import moe.mchdf.quake.weapon.RailGunWeapon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Quake extends JavaPlugin {
    private GameSession game;
    private GameWorld gameWorld;
    private DatabaseManager database;
    private ModHandshake modHandshake;
    private PlayerBoards playerBoards;
    private ModSidebar modSidebar;

    @Override
    public void onEnable() {
        database = new DatabaseManager(this);
        PlayerSettingsService playerSettings = new PlayerSettingsService(
                new PlayerSettingsRepository(database), getLogger());
        getServer().getPluginManager().registerEvents(playerSettings, this);
        getServer().getOnlinePlayers().forEach(playerSettings::load);
        gameWorld = new GameWorld(this, new GameWorldRepository(database));
        getServer().getPluginManager().registerEvents(new GameWorldListener(gameWorld), this);
        gameWorld.loadAll();
        LobbyWorldManager lobby = gameWorld.lobby();
        MenuBook book = new MenuBook();
        LobbyItems lobbyItems = new LobbyItems(book);
        LobbyArrival lobbyArrival = new LobbyArrival(lobby, lobbyItems);
        playerBoards = new PlayerBoards();
        getServer().getPluginManager().registerEvents(playerBoards, this);
        game = new GameSession(this, lobbyItems, gameWorld, lobbyArrival, playerBoards);
        game.setAutoRejoin(player -> playerSettings.get(player).autoRejoin());
        game.addListener(new GameChatNotifier());
        modHandshake = new ModHandshake(this);
        modHandshake.register();
        modSidebar = new ModSidebar(this, modHandshake);
        modSidebar.register();
        ModHitMarker modHitMarker = new ModHitMarker(this, modHandshake);
        modHitMarker.register();
        game.addListener(modHitMarker);
        ModSettingsScreen modSettingsScreen = new ModSettingsScreen(this, modHandshake);
        modSettingsScreen.register();
        new ModPlayerSettings(this, modHandshake, playerSettings).register();
        SettingsMenu settingsMenu = new SettingsMenu(modSettingsScreen, playerSettings);
        ServerSidebar.addDefaults(getConfig());
        saveConfig();
        ServerSidebar sidebar = new ServerSidebar(game, playerBoards, modSidebar, getConfig(),
                player -> playerSettings.get(player).sidebar());
        modHandshake.addAcceptListener(sidebar::onModAccepted);
        playerSettings.addChangeListener(sidebar::onSettingsChanged);
        game.addListener(sidebar);
        getServer().getPluginManager().registerEvents(sidebar, this);
        sidebar.start(this);
        MinigameMenu menu = new MinigameMenu(this, game);
        WorldDeleteMenu worldDeleteMenu = new WorldDeleteMenu(this, gameWorld);
        WorldTeleportMenu worldTeleportMenu = new WorldTeleportMenu(this, gameWorld, lobby);
        var command = Objects.requireNonNull(getCommand("quake"), "quake command is missing from plugin.yml");
        QuakeCommand executor = new QuakeCommand(this, book, game, gameWorld, worldDeleteMenu, worldTeleportMenu);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        Objects.requireNonNull(getCommand("settings"), "settings command is missing from plugin.yml")
                .setExecutor(new SettingsCommand(settingsMenu));
        RailGunWeapon railGunWeapon = new RailGunWeapon(this, game.getGameTeams(), modHitMarker);
        getServer().getPluginManager().registerEvents(new PlayerListener(book, menu, settingsMenu, game, lobbyArrival), this);
        getServer().getPluginManager().registerEvents(settingsMenu, this);
        getServer().getPluginManager().registerEvents(worldDeleteMenu, this);
        getServer().getPluginManager().registerEvents(worldTeleportMenu, this);
        getServer().getPluginManager().registerEvents(game.getDoubleJump(), this);
        getServer().getPluginManager().registerEvents(new RailGunListener(railGunWeapon), this);
        getServer().getOnlinePlayers().forEach(lobbyItems::give);
        // Plugin startup logic
        getComponentLogger().info(
                Component.text("MCHDF Quake 플러그인이 활성화 되었습니다.", NamedTextColor.GREEN, TextDecoration.BOLD)
        );
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.shutdown();
        }
        if (modSidebar != null) {
            modSidebar.hideAll();
        }
        if (playerBoards != null) {
            playerBoards.shutdown();
        }
        if (gameWorld != null) {
            gameWorld.saveAll();
        }
        if (database != null) {
            database.close();
        }
        gameWorld = null;
        database = null;
        playerBoards = null;
        modSidebar = null;
        // Plugin shutdown logic
        getComponentLogger().info(
                Component.text("MCHDF Quake 플러그인이 비활성화 되었습니다.", NamedTextColor.RED, TextDecoration.BOLD)
        );
    }

    public GameWorld getGameWorld() {
        if (gameWorld == null) {
            throw new IllegalStateException("Game world has not been loaded yet.");
        }
        return gameWorld;
    }

    public ModHandshake getModHandshake() {
        if (modHandshake == null) {
            throw new IllegalStateException("Mod handshake has not been initialized yet.");
        }
        return modHandshake;
    }

    public DatabaseManager getDatabase() {
        if (database == null) {
            throw new IllegalStateException("Database has not been initialized yet.");
        }
        return database;
    }
}
