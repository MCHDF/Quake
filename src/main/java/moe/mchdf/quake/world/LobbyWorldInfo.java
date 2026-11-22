package moe.mchdf.quake.world;

import java.time.Instant;
import java.util.UUID;

public record LobbyWorldInfo(String name, UUID worldUuid, Instant updatedAt, UUID updatedBy, Spawn spawn) {
    public record Spawn(double x, double y, double z, float yaw, float pitch) {}

    public LobbyWorldInfo(String name, UUID worldUuid, Instant updatedAt, UUID updatedBy) {
        this(name, worldUuid, updatedAt, updatedBy, null);
    }
}

