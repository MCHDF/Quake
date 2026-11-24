package moe.mchdf.quake.item;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

class QuakeItemsTest {

    private static ItemStack item(Material material, PersistentDataContainerView data) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        when(item.getPersistentDataContainer()).thenReturn(data);
        return item;
    }

    @Test
    void readsStoredItemId() {
        PersistentDataContainerView data = mock(PersistentDataContainerView.class);
        when(data.get(QuakeItems.ITEM_ID, PersistentDataType.STRING)).thenReturn(QuakeItems.RAILGUN);
        ItemStack railGun = item(Material.IRON_HOE, data);
        assertEquals(QuakeItems.RAILGUN, QuakeItems.idOf(railGun));
        assertTrue(RailGunItem.isRailGun(railGun));
        assertFalse(RailGunItem.isTestRailGun(railGun));
    }

    @Test
    void plainItemsHaveNoId() {
        ItemStack hoe = item(Material.IRON_HOE, mock(PersistentDataContainerView.class));
        when(hoe.getItemMeta()).thenReturn(mock(ItemMeta.class));
        assertNull(QuakeItems.idOf(hoe));
        assertNull(QuakeItems.idOf(null));
        assertFalse(RailGunItem.isRailGun(hoe));
    }

    @Test
    void recognizesItemsGivenBeforeItemIdsExisted() {
        PersistentDataContainerView bookData = mock(PersistentDataContainerView.class);
        when(bookData.has(new NamespacedKey("quake", "minigame_menu_book"), PersistentDataType.BYTE)).thenReturn(true);
        assertEquals(QuakeItems.MENU_BOOK, QuakeItems.idOf(item(Material.BOOK, bookData)));

        PersistentDataContainerView testData = mock(PersistentDataContainerView.class);
        when(testData.has(new NamespacedKey("quake", "test_railgun"), PersistentDataType.BOOLEAN)).thenReturn(true);
        assertEquals(QuakeItems.RAILGUN_TEST, QuakeItems.idOf(item(Material.IRON_HOE, testData)));

        ItemStack oldRailGun = item(Material.IRON_HOE, mock(PersistentDataContainerView.class));
        ItemMeta meta = mock(ItemMeta.class);
        when(meta.displayName()).thenReturn(Component.text("RailGun", NamedTextColor.GOLD, TextDecoration.BOLD));
        when(oldRailGun.getItemMeta()).thenReturn(meta);
        assertEquals(QuakeItems.RAILGUN, QuakeItems.idOf(oldRailGun));
    }
}
