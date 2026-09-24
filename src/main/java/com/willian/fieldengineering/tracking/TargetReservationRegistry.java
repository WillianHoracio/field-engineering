package com.willian.fieldengineering.tracking;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class TargetReservationRegistry {

    private static final long RESERVATION_TIMEOUT_TICKS = 600L;

    private static final Map<ServerLevel, Map<UUID, Reservation>> RESERVATIONS_BY_LEVEL =
            new WeakHashMap<>();

    private TargetReservationRegistry() {
    }

    public static boolean tryReserve(
            ServerLevel level,
            UUID targetUuid,
            BlockPos launcherPos
    ) {
        cleanupExpired(level);

        Map<UUID, Reservation> reservations =
                RESERVATIONS_BY_LEVEL.computeIfAbsent(
                        level,
                        ignored -> new HashMap<>()
                );

        long launcherKey = launcherPos.asLong();
        Reservation current = reservations.get(targetUuid);

        if (current != null && current.launcherKey != launcherKey) {
            return false;
        }

        reservations.put(
                targetUuid,
                new Reservation(
                        launcherKey,
                        level.getGameTime() + RESERVATION_TIMEOUT_TICKS
                )
        );

        return true;
    }

    public static boolean isReservedByAnother(
            ServerLevel level,
            UUID targetUuid,
            BlockPos launcherPos
    ) {
        cleanupExpired(level);

        Map<UUID, Reservation> reservations =
                RESERVATIONS_BY_LEVEL.get(level);

        if (reservations == null) {
            return false;
        }

        Reservation reservation = reservations.get(targetUuid);

        return reservation != null
                && reservation.launcherKey != launcherPos.asLong();
    }

    public static void release(
            ServerLevel level,
            UUID targetUuid,
            BlockPos launcherPos
    ) {
        if (targetUuid == null) {
            return;
        }

        Map<UUID, Reservation> reservations =
                RESERVATIONS_BY_LEVEL.get(level);

        if (reservations == null) {
            return;
        }

        long launcherKey = launcherPos.asLong();
        Reservation reservation = reservations.get(targetUuid);

        if (reservation != null && reservation.launcherKey == launcherKey) {
            reservations.remove(targetUuid);
        }

        if (reservations.isEmpty()) {
            RESERVATIONS_BY_LEVEL.remove(level);
        }
    }

    public static void releaseAllForLauncher(
            ServerLevel level,
            BlockPos launcherPos
    ) {
        Map<UUID, Reservation> reservations =
                RESERVATIONS_BY_LEVEL.get(level);

        if (reservations == null) {
            return;
        }

        long launcherKey = launcherPos.asLong();

        reservations.entrySet().removeIf(
                entry -> entry.getValue().launcherKey == launcherKey
        );

        if (reservations.isEmpty()) {
            RESERVATIONS_BY_LEVEL.remove(level);
        }
    }

    private static void cleanupExpired(ServerLevel level) {
        Map<UUID, Reservation> reservations =
                RESERVATIONS_BY_LEVEL.get(level);

        if (reservations == null) {
            return;
        }

        long gameTime = level.getGameTime();
        Iterator<Map.Entry<UUID, Reservation>> iterator =
                reservations.entrySet().iterator();

        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAtTick <= gameTime) {
                iterator.remove();
            }
        }

        if (reservations.isEmpty()) {
            RESERVATIONS_BY_LEVEL.remove(level);
        }
    }

    private record Reservation(
            long launcherKey,
            long expiresAtTick
    ) {
    }
}
