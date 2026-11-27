package moe.mchdf.quake.menu;

import moe.mchdf.quake.item.SettingsItem;
import org.bukkit.entity.Player;

/**
 * 로비에서 들고 있는 아이템: 첫 칸의 메뉴 책과 맨 끝 칸의 설정 아이템.
 * 게임을 시작하면 함께 빼고, 로비로 돌아오면 함께 돌려준다.
 */
public final class LobbyItems {
    private final MenuBook book;

    public LobbyItems(MenuBook book) {
        this.book = book;
    }

    public void give(Player player) {
        book.giveMenuBook(player);
        SettingsItem.give(player);
    }

    public void remove(Player player) {
        book.removeMenuBooks(player);
        SettingsItem.remove(player);
    }
}
