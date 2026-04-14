package com.example.clientutils.feature;

import com.example.clientutils.ClientUtilsMod;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class WaypointManager {
    private WaypointManager() {
    }

    public static void add(String name, BlockPos pos) {
        if (name == null || name.isBlank() || pos == null) {
            return;
        }
        remove(name);
        ClientUtilsMod.CONFIG.waypoints.add(serialize(new Waypoint(name, pos.getX(), pos.getY(), pos.getZ())));
        ClientUtilsMod.CONFIG.save();
    }

    public static void remove(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        ClientUtilsMod.CONFIG.waypoints.removeIf(raw -> deserialize(raw).name().toLowerCase(Locale.ROOT).equals(lower));
        ClientUtilsMod.CONFIG.save();
    }

    public static List<Waypoint> all() {
        List<Waypoint> out = new ArrayList<>();
        for (String raw : ClientUtilsMod.CONFIG.waypoints) {
            Waypoint waypoint = deserialize(raw);
            if (!waypoint.name().isBlank()) {
                out.add(waypoint);
            }
        }
        out.sort(Comparator.comparing(Waypoint::name));
        return out;
    }

    public static Waypoint nearest(double x, double y, double z) {
        Waypoint best = null;
        double bestDist = Double.MAX_VALUE;
        for (Waypoint waypoint : all()) {
            double dx = waypoint.x() - x;
            double dy = waypoint.y() - y;
            double dz = waypoint.z() - z;
            double dist = dx * dx + dy * dy + dz * dz;
            if (dist < bestDist) {
                bestDist = dist;
                best = waypoint;
            }
        }
        return best;
    }

    private static String serialize(Waypoint waypoint) {
        return waypoint.name() + ":" + waypoint.x() + ":" + waypoint.y() + ":" + waypoint.z();
    }

    private static Waypoint deserialize(String raw) {
        if (raw == null || raw.isBlank()) {
            return new Waypoint("", 0, 0, 0);
        }
        String[] parts = raw.split(":");
        if (parts.length != 4) {
            return new Waypoint("", 0, 0, 0);
        }
        try {
            return new Waypoint(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return new Waypoint("", 0, 0, 0);
        }
    }

    public record Waypoint(String name, int x, int y, int z) {
    }
}

