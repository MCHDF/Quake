package moe.mchdf.quake.game;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import moe.mchdf.quake.world.GameWorldInfo;

final class WorldRotation {
    private final Deque<GameWorldInfo> upcomingWorlds = new ArrayDeque<>();

    GameWorldInfo next(List<GameWorldInfo> ready, GameWorldInfo currentWorld) {
        upcomingWorlds.removeIf(queued -> ready.stream().noneMatch(info -> sameRotationEntry(info, queued)));
        for (GameWorldInfo info : ready) {
            if (!sameRotationEntry(info, currentWorld)
                    && upcomingWorlds.stream().noneMatch(queued -> sameRotationEntry(info, queued))) {
                upcomingWorlds.addLast(info);
            }
        }
        if (currentWorld != null) {
            ready.stream().filter(info -> sameRotationEntry(info, currentWorld)).findFirst()
                    .ifPresent(upcomingWorlds::addLast);
        }
        return upcomingWorlds.pollFirst();
    }

    void clear() {
        upcomingWorlds.clear();
    }

    static boolean sameRotationEntry(GameWorldInfo first, GameWorldInfo second) {
        return first != null && second != null && first.name().equals(second.name())
                && first.worldUuid().equals(second.worldUuid()) && first.rotationOrder() == second.rotationOrder();
    }
}
