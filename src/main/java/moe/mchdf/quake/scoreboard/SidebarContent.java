package moe.mchdf.quake.scoreboard;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * 사이드바 내용.
 *
 * @param accent 모드 패널의 강조색(위쪽 선). {@code null}이면 모드 기본색
 * @param rows   줄 목록
 */
public record SidebarContent(TextColor accent, List<SidebarRow> rows) {
    public SidebarContent {
        rows = List.copyOf(rows);
    }

    /** 바닐라 사이드바 줄. 제목 바로 아래에 빈 줄을 하나 둔다. */
    public List<Component> vanillaLines() {
        List<Component> lines = new ArrayList<>(rows.size() + 1);
        lines.add(Component.empty());
        for (SidebarRow row : rows) {
            lines.add(row.vanillaLine());
        }
        return lines;
    }
}
