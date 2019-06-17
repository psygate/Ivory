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

package com.psygate.minecraft.spigot.sovereignty.ivory.groups;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by psygate (https://github.com/psygate) on 29.01.2016.
 */
public interface Group {
    String getName();

    long getGroupID();

    long getCreation();

    UUID getCreator();

    void setCreator(UUID creator);

    Map<UUID, Member> getMembers();

    boolean hasMember(UUID member);

    boolean hasMemberWithRank(UUID member, Rank rank);

    /**
     * Returns true if a member with Rank rank or greater Rank than rank is present.
     *
     * @param member
     * @param rank
     * @return
     */
    boolean hasMemberWithRankGE(UUID member, Rank rank);

    void delete();

    boolean addMember(UUID member, Rank rank, UUID invitedBy, boolean hidden);

    boolean removeMember(UUID member);

    boolean promoteMember(UUID member, Rank newrank);

    boolean demoteMember(UUID member, Rank newrank);

    void addToken(String token, Rank rank, UUID creator, int usages);

    void invite(UUID playerUUID, Rank guest, UUID invitedBy);

    boolean hasInvite(UUID uniqueId);

    void joinPerInvite(UUID uniqueId);

    void joinPerToken(UUID uniqueId, String token);

    boolean hasToken(String token);

    Optional<Member> getMember(UUID uniqueId);
}
