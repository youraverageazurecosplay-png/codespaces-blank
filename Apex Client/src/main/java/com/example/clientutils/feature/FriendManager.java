package com.example.clientutils.feature;

import com.example.clientutils.ClientUtilsMod;

public final class FriendManager {
    private FriendManager() {
    }

    public static boolean isFriend(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return ClientUtilsMod.CONFIG.friends.contains(name.toLowerCase());
    }

    public static void add(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        ClientUtilsMod.CONFIG.friends.add(name.toLowerCase());
        ClientUtilsMod.CONFIG.save();
    }

    public static void remove(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        ClientUtilsMod.CONFIG.friends.remove(name.toLowerCase());
        ClientUtilsMod.CONFIG.save();
    }

    public static boolean toggle(String name) {
        if (isFriend(name)) {
            remove(name);
            return false;
        }
        add(name);
        return true;
    }
}

