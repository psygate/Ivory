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

package com.psygate.minecraft.spigot.sovereignty.ivory.managment;

import com.psygate.cache.state.ACachedObject;
import com.psygate.cache.state.CachedState;
import com.psygate.minecraft.spigot.sovereignty.ivory.Ivory;
import com.psygate.minecraft.spigot.sovereignty.ivory.db.model.tables.records.IvoryGroupInvitesRecord;
import com.psygate.minecraft.spigot.sovereignty.ivory.db.model.tables.records.IvoryGroupMembersRecord;
import com.psygate.minecraft.spigot.sovereignty.ivory.db.model.tables.records.IvoryGroupsRecord;
import com.psygate.minecraft.spigot.sovereignty.ivory.db.model.tables.records.IvoryTokensRecord;
import com.psygate.minecraft.spigot.sovereignty.ivory.groups.Group;
import com.psygate.minecraft.spigot.sovereignty.ivory.groups.Member;
import com.psygate.minecraft.spigot.sovereignty.ivory.groups.Rank;

import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by psygate (https://github.com/psygate) on 07.04.2016.
 */
class ManagedGroup extends ACachedObject implements Group {
    private final static Logger LOG = Ivory.getLogger(ManagedGroup.class.getName());
    private Long groupid;
    private final String name;
    private Map<UUID, ManagedMember> members = new HashMap<>();
    private UUID creator;
    private long creation;

    public ManagedGroup(IvoryGroupsRecord rec, Collection<IvoryGroupMembersRecord> members) {
        super(CachedState.CLEAN);
        this.groupid = rec.getGroupId();
        this.name = rec.getGroupName();
        this.creation = rec.getCreated().getTime();
        this.creator = rec.getCreatorPuuid();
        this.members.putAll(members.stream().map(m -> new ManagedMember(m, this)).collect(Collectors.toMap(m -> m.getUUID(), m -> m)));
    }

    public ManagedGroup(String name, UUID playerUUID) {
        super(CachedState.NEW);
        this.name = name;
        members.put(playerUUID, new ManagedMember(this, playerUUID, Rank.CREATOR, false, playerUUID));
        this.creator = playerUUID;
        this.creation = System.currentTimeMillis();
    }

    @Override
    public long getGroupID() {
        if (groupid == null) {
            GroupIO.getInstance().persist(this);
        }
        return groupid;
    }

    public void setGroupID(Long groupid) {
        checkStateOrThrow();
        this.groupid = groupid;
//        setState(CachedState.DIRTY);
    }

    @Override
    public String getName() {
        checkStateOrThrow();
        return name;
    }

    @Override
    public long getCreation() {
        checkStateOrThrow();
        return creation;
    }

    @Override
    public UUID getCreator() {
        checkStateOrThrow();
        return creator;
    }

    @Override
    public void setCreator(UUID creator) {
        checkStateOrThrow();
        if (!creator.equals(this.creator)) {
            this.creator = creator;
            setState(CachedState.DIRTY);
        }
    }

    @Override
    public Map<UUID, Member> getMembers() {
        checkStateOrThrow();
        return members.values()
                .stream()
                .filter(v -> v.getState() != CachedState.DELETED)
                .collect(Collectors.toMap(m -> m.getUUID(), m -> m));
//        return Collections.unmodifiableMap(members);
    }

    @Override
    public boolean hasMember(UUID member) {
        checkStateOrThrow();
        if (members.containsKey(member)) {
            LOG.info("M: " + members.get(member) + " " + members.get(member).getState());
        } else {
            LOG.info("No member for " + member);
        }
        return members.containsKey(member) && members.get(member).getState() != CachedState.DELETED;
    }

    @Override
    public boolean hasMemberWithRank(UUID member, Rank rank) {
        checkStateOrThrow();
        return members.containsKey(member)
                && members.get(member).getState() != CachedState.DELETED
                && members.get(member).getRank() == rank;
    }

    @Override
    public boolean hasMemberWithRankGE(UUID member, Rank rank) {
        checkStateOrThrow();

        return creator.equals(member)
                ||
                (members.containsKey(member)
                        && members.get(member).getState() != CachedState.DELETED
                        && members.get(member).getRank().ge(rank));
    }

    @Override
    public void delete() {
        checkStateOrThrow();
        setState(CachedState.DELETED);
        GroupIO.getInstance().persist(this, true);
    }

    @Override
    public boolean addMember(UUID member, Rank rank, UUID invitedBy, boolean hidden) {
        checkStateOrThrow();
        ManagedMember modify;
        if (members.containsKey(member) && members.get(member).getState() == CachedState.DELETED) {
            LOG.info("Member modification starting.");
            modify = members.get(member);

            switch (modify.getLastState()) {
                case NEW:
                case DIRTY:
                    modify.setState(modify.getLastState());
                    break;
                case CLEAN:
                case DELETED:
                    modify.setState(CachedState.DIRTY);
                    break;
                case UNDEFINED:
                    modify.setState(CachedState.NEW);
                    break;
                default:
                    LOG.severe("Unkown state for member resurrection: " + getState() + " " + toString());
            }

            modify.setHidden(hidden);
            modify.setRank(rank);
            modify.setInvitedBy(invitedBy);
            modify.setJoinTime(System.currentTimeMillis());
            GroupIO.getInstance().persist(this, true);
            setState(CachedState.DIRTY);
            return true;
        } else if (members.containsKey(member)) {
            return false;
        } else {
            LOG.info("Member simple put join.");
            modify = new ManagedMember(this, member, rank, hidden, invitedBy);
            modify.setState(CachedState.NEW);
            GroupIO.getInstance().persist(this, true);
            members.put(member, modify);
            setState(CachedState.DIRTY);
            return true;
        }

    }

    @Override
    public boolean removeMember(UUID member) {
        checkStateOrThrow();
        if (members.containsKey(member)) {
            ManagedMember mem = members.get(member);
            mem.setState(CachedState.DELETED);
//            members.remove(member);
            setState(CachedState.DIRTY);
            return true;
        } else {
            setState(CachedState.DIRTY);
            return false;
        }
    }

    @Override
    public boolean promoteMember(UUID member, Rank newrank) {
        checkStateOrThrow();
        if (members.containsKey(member)) {
            ManagedMember mem = members.get(member);
            if (mem.getState() == CachedState.DELETED) {
                return false;
            } else if (mem.getRank().ge(newrank)) {
                return false;
            } else {
                mem.setRank(newrank);
                mem.setState(CachedState.DIRTY);
                setState(CachedState.DIRTY);
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean demoteMember(UUID member, Rank newrank) {
        checkStateOrThrow();
        if (members.containsKey(member)) {
            ManagedMember mem = members.get(member);
            if (mem.getState() == CachedState.DELETED) {
                return false;
            } else if (mem.getRank().le(newrank)) {
                return false;
            } else {
                setState(CachedState.DIRTY);
                mem.setRank(newrank);
                mem.setState(CachedState.DIRTY);
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public void addToken(String token, Rank rank, UUID creator, int usages) {
        GroupIO.getInstance().createToken(token, rank, this, creator, usages);
    }

    @Override
    public void invite(UUID playerUUID, Rank rank, UUID invitedBy) {
        GroupIO.getInstance().createInvite(playerUUID, rank, invitedBy, this);
    }

    @Override
    public boolean hasInvite(UUID uniqueId) {
        return GroupIO.getInstance().hasInvite(this, uniqueId);
    }

    @Override
    public void joinPerInvite(UUID uniqueId) {
        IvoryGroupInvitesRecord rec = Objects.requireNonNull(GroupIO.getInstance().getInviteAndInvalidate(this, uniqueId));
        addMember(uniqueId, rec.getRank(), rec.getInviterPuuid(), false);
    }

    @Override
    public void joinPerToken(UUID uniqueId, String token) {
        IvoryTokensRecord rec = GroupIO.getInstance().getTokenAndInvalidate(this, uniqueId, token);
        addMember(uniqueId, rec.getRank(), rec.getCreator(), false);
    }

    @Override
    public boolean hasToken(String token) {
        return GroupIO.getInstance().hasToken(this, token);
    }

    @Override
    public void persistChanges() {
        GroupIO.getInstance().persist(this);
    }

    private void checkStateOrThrow() {
        if (getState() == CachedState.DELETED) {
            throw new IllegalStateException("Deleted state set.");
        }
    }

    IvoryGroupsRecord toRecord() {
        return new IvoryGroupsRecord(
                getGroupIDBypassing(),
                getNameBypassing(),
                getCreator(),
                new Timestamp(getCreation())
        );
    }

    Long getGroupIDBypassing() {
        return groupid;
    }


    public Collection<ManagedMember> getMembersInternal() {
        return members.values();
    }

    String getNameBypassing() {
        return name;
    }

    @Override
    public Optional<Member> getMember(UUID uniqueId) {
        if (members.containsKey(uniqueId) && members.get(uniqueId).getState() != CachedState.DELETED) {
            return Optional.of(members.get(uniqueId));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return "G[" + name + "(" + groupid + ")]";
    }

    @Override
    public void setState(CachedState state) {
        LOG.info(this + " now " + state);
        super.setState(state);
    }
}
