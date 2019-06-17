/*
 *     Copyright (C) 2016 psygate (https://github.com/psygate)
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 */

package com.psygate.minecraft.spigot.sovereignty.ivory.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by psygate (https://github.com/psygate) on 01.02.2016.
 */
public class PlayerSettings {
    private UUID player;
    private boolean autoAccept = false;
    private Set<UUID> ignoredPlayers = new HashSet<>();
    private Set<Long> ignoredGroups = new HashSet<>();
    private Set<String> ignoredGroupsName = new HashSet<>();

    public PlayerSettings() {
    }

    public UUID getPlayer() {
        return player;
    }

    public void setPlayer(UUID player) {
        this.player = player;
    }

    public boolean isAutoAccept() {
        return autoAccept;
    }

    public void setAutoAccept(boolean autoAccept) {
        this.autoAccept = autoAccept;
    }

    public Set<UUID> getIgnoredPlayers() {
        return ignoredPlayers;
    }

    public void setIgnoredPlayers(Set<UUID> ignoredPlayers) {
        this.ignoredPlayers = ignoredPlayers;
    }

    public Set<String> getIgnoredGroupsName() {
        return ignoredGroupsName;
    }

    public void setIgnoredGroupsName(Set<String> ignoredGroupsName) {
        this.ignoredGroupsName = ignoredGroupsName;
    }

    public Set<Long> getIgnoredGroups() {
        return ignoredGroups;
    }

    public void setIgnoredGroups(Set<Long> ignoredGroups) {
        this.ignoredGroups = ignoredGroups;
    }

    public boolean isIgnored(UUID player) {
        return ignoredPlayers.contains(player);
    }

    public boolean isIgnored(Long id) {
        return ignoredGroups.contains(id);
    }

    public boolean isIgnored(String groupname) {
        return ignoredGroupsName.contains(groupname);
    }
}
