package moe.mchdf.quake.world;

import java.time.Instant;
import java.util.UUID;

/** Legacy registrations may have unknown UUID, registration time, or creator. */
public record GameWorldInfo(String name, UUID worldUuid, String displayName,
                            GameWorldStatus status, Instant createdAt, UUID createdBy,
                            int minPlayers, int maxPlayers, long rotationOrder) {
    public GameWorldInfo {
        validatePlayerLimits(minPlayers, maxPlayers);
    }

    public GameWorldInfo(String name, UUID worldUuid, String displayName,
                         GameWorldStatus status, Instant createdAt, UUID createdBy) {
        this(name, worldUuid, displayName, status, createdAt, createdBy, 2, 16);
    }

    public GameWorldInfo(String name, UUID worldUuid, String displayName,
                         GameWorldStatus status, Instant createdAt, UUID createdBy, int minPlayers, int maxPlayers) {
        this(name, worldUuid, displayName, status, createdAt, createdBy, minPlayers, maxPlayers, 0);
    }

    public GameWorldInfo withStatus(GameWorldStatus status, long order) {
        return new GameWorldInfo(name, worldUuid, displayName, status, createdAt, createdBy,
                minPlayers, maxPlayers, order);
    }

    public static void validatePlayerLimits(int minPlayers, int maxPlayers) {
        if (minPlayers < 2 || maxPlayers > 16 || minPlayers > maxPlayers
                || minPlayers % 2 != 0 || maxPlayers % 2 != 0) {
            throw new IllegalArgumentException("인원은 2~16 사이의 짝수이며 최소 인원은 최대 인원 이하여야 합니다.");
        }
    }

    public GameWorldInfo withPlayerLimits(int min, int max) {
        return new GameWorldInfo(name, worldUuid, displayName, status, createdAt, createdBy, min, max, rotationOrder);
    }
    public GameWorldInfo withWorldUuid(UUID uuid) {
        return new GameWorldInfo(name, uuid, displayName, status, createdAt, createdBy, minPlayers, maxPlayers, rotationOrder);
    }
}
