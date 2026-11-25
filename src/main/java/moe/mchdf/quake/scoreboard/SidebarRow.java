package moe.mchdf.quake.scoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * 사이드바 한 줄. 문구와 색은 서버가 정하고, 줄의 형태(값 줄, 막대, 구분선 등)만 구분한다.
 *
 * <p>모드는 형태에 맞춰 꾸며서 그리고(값 오른쪽 정렬, 진행 막대 등), 바닐라 사이드바는
 * {@link #vanillaLine()}으로 바꾼 글자 한 줄을 보여 준다.
 */
public sealed interface SidebarRow {
    NamedTextColor VANILLA_LABEL = NamedTextColor.WHITE;

    /** 바닐라 사이드바에 표시할 글자 한 줄 */
    Component vanillaLine();

    /** 구분선. 바닐라에서는 빈 줄 */
    record Spacer() implements SidebarRow {
        @Override
        public Component vanillaLine() {
            return Component.empty();
        }
    }

    /** 글자만 있는 줄. {@code centered}이면 모드에서 가운데 정렬 */
    record Text(Component text, boolean centered) implements SidebarRow {
        @Override
        public Component vanillaLine() {
            return text;
        }
    }

    /** 이름과 값. 모드에서는 이름을 왼쪽, 값을 오른쪽 끝에 그린다. */
    record Value(Component label, Component value) implements SidebarRow {
        @Override
        public Component vanillaLine() {
            return labeled(label, value);
        }
    }

    /** 이름과 값 아래에 진행 막대를 그리는 줄. {@code ratio}는 0~1 */
    record Bar(Component label, Component value, float ratio, TextColor color) implements SidebarRow {
        public Bar {
            ratio = Math.clamp(ratio, 0f, 1f);
        }

        @Override
        public Component vanillaLine() {
            return labeled(label, value);
        }
    }

    private static Component labeled(Component label, Component value) {
        Component name = label.color() == null ? label.color(VANILLA_LABEL) : label;
        return Component.textOfChildren(name, Component.text(": ", VANILLA_LABEL), value);
    }
}
