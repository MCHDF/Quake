package moe.mchdf.quake.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import moe.mchdf.quake.item.RailGunItem;
import moe.mchdf.quake.mod.ModSettingsScreen;
import moe.mchdf.quake.settings.PlayerSettings;
import moe.mchdf.quake.settings.PlayerSettingsService;
import moe.mchdf.quake.settings.SoundLevel;
import moe.mchdf.quake.settings.SoundType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * 내 설정 화면. 모드 사용자는 모드 전용 화면을, 바닐라 사용자는 상자형 GUI를 연다.
 * 두 사용자가 바꿀 수 있는 항목이 달라 화면을 나눈다.
 *
 * <p>상자형 GUI는 항목을 클릭할 때마다 값을 바꿔 바로 저장하고, 화면을 다시 그려 현재 값을 보여 준다.
 * 효과음은 종류가 많아 「효과음」 버튼으로 따로 여는 분류 화면에 둔다. 음량을 바꾸면 그 소리를 새 음량으로
 * 들려준다({@link moe.mchdf.quake.sound.SoundPreview}).
 */
public final class SettingsMenu implements Listener {
    static final int ROWS = 3;
    static final int SIDEBAR_SLOT = 11;
    static final int SOUND_SLOT = 13;
    static final int AUTO_REJOIN_SLOT = 15;
    static final int BACK_SLOT = (ROWS - 1) * 9;
    static final int CLOSE_SLOT = ROWS * 9 - 1;

    /** 효과음 분류 화면의 칸. 가운데 줄에 종류마다 하나 */
    static final Map<Integer, SoundType> SOUND_SLOTS = Map.of(
            11, SoundType.HIT_CONFIRM,
            12, SoundType.KILL,
            13, SoundType.OWN_SHOT,
            14, SoundType.OTHER_SHOT,
            15, SoundType.COUNTDOWN);

    enum Page {
        MAIN("설정"),
        SOUND("설정 · 효과음");

        private final String title;

        Page(String title) {
            this.title = title;
        }
    }

    private final ModSettingsScreen modScreen;
    private final PlayerSettingsService settings;

    public SettingsMenu(ModSettingsScreen modScreen, PlayerSettingsService settings) {
        this.modScreen = modScreen;
        this.settings = settings;
    }

    public void open(Player player) {
        if (modScreen.open(player)) {
            return;
        }
        openVanilla(player, Page.MAIN);
    }

    private void openVanilla(Player player, Page page) {
        SettingsHolder holder = new SettingsHolder(page);
        holder.inventory = Bukkit.createInventory(holder, ROWS * 9, Component.text(page.title, NamedTextColor.DARK_AQUA));
        render(holder.inventory, page, settings.get(player));
        player.openInventory(holder.inventory);
    }

    static void render(Inventory inventory, Page page, PlayerSettings current) {
        switch (page) {
            case MAIN -> {
                inventory.setItem(SIDEBAR_SLOT, toggle(Material.MAP, "사이드바", current.sidebar(),
                        "화면 오른쪽의 점수·서버 정보를 보여 줍니다."));
                inventory.setItem(SOUND_SLOT, item(Material.NOTE_BLOCK, "효과음", NamedTextColor.AQUA, List.of(
                        Component.text("적중음·처치음·발사음·카운트다운의", NamedTextColor.GRAY),
                        Component.text("음량을 종류별로 정합니다.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("클릭하여 열기", NamedTextColor.YELLOW))));
                inventory.setItem(AUTO_REJOIN_SLOT, toggle(Material.REPEATER, "자동 다시 참여", current.autoRejoin(),
                        "게임이 끝나면 다음 게임에 자동으로 참여합니다."));
            }
            case SOUND -> {
                SOUND_SLOTS.forEach((slot, type) -> inventory.setItem(slot, sound(type, current.sound(type))));
                inventory.setItem(BACK_SLOT, item(Material.ARROW, "뒤로", NamedTextColor.WHITE, List.of()));
            }
        }
        inventory.setItem(CLOSE_SLOT, item(Material.BARRIER, "닫기", NamedTextColor.RED, List.of()));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SettingsHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        switch (holder.page) {
            case MAIN -> {
                switch (slot) {
                    case SIDEBAR_SLOT -> settings.update(player, current -> current.withSidebar(!current.sidebar()));
                    case AUTO_REJOIN_SLOT ->
                            settings.update(player, current -> current.withAutoRejoin(!current.autoRejoin()));
                    case SOUND_SLOT -> {
                        click(player);
                        openVanilla(player, Page.SOUND);
                        return;
                    }
                    default -> {
                        return;
                    }
                }
                click(player);
            }
            case SOUND -> {
                if (slot == BACK_SLOT) {
                    click(player);
                    openVanilla(player, Page.MAIN);
                    return;
                }
                SoundType type = SOUND_SLOTS.get(slot);
                if (type == null) {
                    return;
                }
                // 좌클릭은 다음 단계, 우클릭은 이전 단계. 바뀐 음량은 미리 듣기 소리로 알려 주므로 클릭음은 내지 않는다.
                boolean back = event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT;
                settings.update(player, current -> current.withSound(type,
                        back ? current.sound(type).previous() : current.sound(type).next()));
            }
        }
        render(holder.inventory, holder.page, settings.get(player));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof SettingsHolder) {
            event.setCancelled(true);
        }
    }

    private static void click(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5F, 1.0F);
    }

    private static ItemStack toggle(Material material, String name, boolean on, String description) {
        ItemStack item = item(material, name, on ? NamedTextColor.GREEN : NamedTextColor.GRAY, List.of(
                Component.text(description, NamedTextColor.GRAY),
                Component.empty(),
                Component.text("현재: ", NamedTextColor.GRAY)
                        .append(on ? Component.text("켜짐", NamedTextColor.GREEN)
                                : Component.text("꺼짐", NamedTextColor.RED)),
                Component.text("클릭하여 " + (on ? "끄기" : "켜기"), NamedTextColor.YELLOW)));
        item.editMeta(meta -> meta.setEnchantmentGlintOverride(on));
        return item;
    }

    private static ItemStack sound(SoundType type, SoundLevel level) {
        // 단계를 한눈에 보이게 "끄기 · 작게 · [보통] · 크게"처럼 지금 단계만 밝게 칠한다.
        Component levels = Component.empty();
        for (SoundLevel each : SoundLevel.values()) {
            if (each.ordinal() > 0) {
                levels = levels.append(Component.text(" · ", NamedTextColor.DARK_GRAY));
            }
            levels = levels.append(each == level
                    ? Component.text(each.label(), NamedTextColor.GREEN, TextDecoration.BOLD)
                    : Component.text(each.label(), NamedTextColor.DARK_GRAY));
        }
        List<Component> lore = new ArrayList<>(List.of(
                Component.text(type.description(), NamedTextColor.GRAY),
                Component.empty(),
                levels,
                Component.empty(),
                Component.text("좌클릭: " + level.next().label() + " · 우클릭: " + level.previous().label(),
                        NamedTextColor.YELLOW)));
        boolean on = level != SoundLevel.OFF;
        ItemStack item = item(soundIcon(type), type.label(), on ? NamedTextColor.GREEN : NamedTextColor.GRAY, lore);
        // 칸의 숫자로도 단계를 보여 준다(끄기 1, 작게 1, 보통 2, 크게 3).
        item.setAmount(Math.max(1, level.ordinal()));
        item.editMeta(meta -> meta.setEnchantmentGlintOverride(on));
        return item;
    }

    private static Material soundIcon(SoundType type) {
        return switch (type) {
            case HIT_CONFIRM -> Material.TARGET;
            case KILL -> Material.SKELETON_SKULL;
            case OWN_SHOT -> RailGunItem.MATERIAL;
            case OTHER_SHOT -> Material.ARMOR_STAND;
            case COUNTDOWN -> Material.CLOCK;
        };
    }

    private static ItemStack item(Material material, String name, NamedTextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        });
        return item;
    }

    private static final class SettingsHolder implements InventoryHolder {
        private final Page page;
        private Inventory inventory;

        private SettingsHolder(Page page) {
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
