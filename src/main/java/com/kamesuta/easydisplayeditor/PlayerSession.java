package com.kamesuta.easydisplayeditor;

import org.bukkit.entity.BlockDisplay;

import java.util.*;

public class PlayerSession {
    public static final Map<UUID, PlayerSession> sessions = new HashMap<>();

    public static PlayerSession get(final UUID uuid) {
        return sessions.computeIfAbsent(uuid, (s) -> new PlayerSession());
    }

    public Set<BlockDisplay> selected = new HashSet<>();
}
