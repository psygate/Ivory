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
import com.psygate.minecraft.spigot.sovereignty.ivory.db.model.tables.records.IvoryGroupMembersRecord;
import com.psygate.minecraft.spigot.sovereignty.ivory.groups.Group;
import com.psygate.minecraft.spigot.sovereignty.ivory.groups.Member;
import com.psygate.minecraft.spigot.sovereignty.ivory.groups.Rank;
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.jooq.DSLContext;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created by psygate (https://github.com/psygate) on 07.04.2016.
 */
class ManagedMember extends ACachedObject implements Member {
    private final static Logger LOG = Ivory.getLogger(ManagedMember.class.getName());
    private boolean hidden;
    private UUID uuid;
    private Rank rank;
    private final ManagedGroup group;
    private UUID invitedBy;
    private long joinTime;

    public ManagedMember(ManagedGroup group, UUID uuid, Rank rank, boolean hidden, UUID invitedBy) {
        super(CachedState.NEW);
        this.uuid = Objects.requireNonNull(uuid);
        this.hidden = hidden;
        this.rank = Objects.requireNonNull(rank);
        this.group = Objects.requireNonNull(group);
        this.invitedBy = Objects.requireNonNull(invitedBy);
        this.joinTime = System.currentTimeMillis();
    }


    public ManagedMember(IvoryGroupMembersRecord rec, ManagedGroup group) {
        super(CachedState.CLEAN);
        this.uuid = rec.getPuuid();
        this.hidden = rec.getHiddenBool();
        this.rank = rec.getRank();
        this.group = group;
        this.invitedBy = rec.getInvitedByPuuid();
        this.joinTime = rec.getJointime().getTime();
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    @Override
    public Rank getRank() {
        return rank;
    }

    @Override
    public boolean setRank(Rank rank) {
        if (rank != this.rank) {
            setState(CachedState.DIRTY);
            this.rank = rank;
        }
        return true;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public UUID getInvitedBy() {
        return invitedBy;
    }

    @Override
    public Group getGroup() {
        return group;
    }

    ManagedGroup getManagedGroup() {
        return group;
    }

    @Override
    public void setHidden(boolean hidden) {
        if (hidden != this.hidden) {
            setState(CachedState.DIRTY);
        }

        this.hidden = hidden;
    }

//    public void revertState() {
//        this.state = Objects.requireNonNull(lastState, () -> "No last state to revert to.");
//        lastState = null;
//    }

    public void setInvitedBy(UUID invitedBy) {
        this.invitedBy = invitedBy;
        setState(CachedState.DIRTY);
    }

    public void delete() {
        setState(CachedState.DELETED);
    }

    @Override
    public long getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(long joinTime) {
        setState(CachedState.DIRTY);
        this.joinTime = joinTime;
    }

    @Override
    public void persistChanges() {

    }

    IvoryGroupMembersRecord toRecord() {
        return new IvoryGroupMembersRecord(
                Objects.requireNonNull(group.getGroupIDBypassing(), () -> "Group ID not set, unable to turn member into record."),
                getUUID(),
                getRank(),
                new Timestamp(getJoinTime()),
                getInvitedBy(),
                isHidden()
        );
    }

    @Override
    public String toString() {
        return "M[" + uuid + ", (" + rank + ") " + group + "]";
    }

    @Override
    public void setState(CachedState state) {
        super.setState(state);
        if (group.getState() != CachedState.NEW && state != CachedState.CLEAN) {
            group.setState(CachedState.DIRTY);
        }
        LOG.info(this + " now " + state);
    }
}
