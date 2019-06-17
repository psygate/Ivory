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

import com.psygate.cache.state.CachedState;
import com.psygate.minecraft.spigot.sovereignty.ivory.Ivory;
import com.psygate.minecraft.spigot.sovereignty.ivory.db.model.tables.records.IvoryGroupInvitesRecord;
import com.psygate.minecraft.spigot.sovereignty.ivory.db.model.tables.records.IvoryGroupMembersRecord;
import com.psygate.minecraft.spigot.sovereignty.ivory.db.model.tables.records.IvoryGroupsRecord;
import com.psygate.minecraft.spigot.sovereignty.ivory.db.model.tables.records.IvoryTokensRecord;
import com.psygate.minecraft.spigot.sovereignty.ivory.groups.Rank;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.TransactionalRunnable;
import org.jooq.impl.DSL;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.psygate.minecraft.spigot.sovereignty.ivory.db.model.Tables.*;
import static com.psygate.minecraft.spigot.sovereignty.ivory.db.model.Tables.IVORY_GROUP_INVITES;

/**
 * Created by psygate on 05.05.2016.
 */
class GroupIO {
    private final static Logger LOG = Ivory.getLogger(GroupIO.class.getName());
    private static GroupIO instance;
    private boolean async = true;

    private GroupIO() {

    }

    public static GroupIO getInstance() {
        if (instance == null) {
            instance = new GroupIO();
        }

        return instance;
    }

    void setAsync(boolean async) {
        this.async = async;
    }

    void persist(ManagedGroup group) {
        persist(group, async);
    }

    void persist(ManagedGroup group, boolean async) {
        switch (group.getState()) {
            case CLEAN:
                LOG.info("Group " + group + " is clean.");
                break;
            case NEW:
                LOG.info("Group " + group + " is new.");
                insertNewGroup(group, async);
                group.setState(CachedState.CLEAN);
                break;
            case DIRTY:
                LOG.info("Group " + group + " is dirty.");
                updateDirtyGroup(group, async);
                group.setState(CachedState.CLEAN);
                break;
            case DELETED:
                LOG.info("Group " + group + " is marked for deletion.");
                deleteGroup(group, async);
                break;
            default:
                LOG.severe("Group " + group + " has an invalid state. " + group.getState());
        }
    }

    private void deleteGroup(ManagedGroup group, boolean async) {
        executeQuery((conf) -> {
            DSLContext ctx = DSL.using(conf);

            ctx.deleteFrom(IVORY_GROUP_MEMBERS).where(IVORY_GROUP_MEMBERS.GROUP_ID.eq(group.getGroupIDBypassing())).execute();
            ctx.deleteFrom(IVORY_GROUPS).where(IVORY_GROUPS.GROUP_ID.eq(group.getGroupIDBypassing())).execute();
        }, async);
    }

    private void insertNewGroup(ManagedGroup group, boolean async) {
        executeQuery((conf) -> {
            DSLContext ctx = DSL.using(conf);

            long id = ctx.insertInto(IVORY_GROUPS)
                    .set(group.toRecord())
                    .returning(IVORY_GROUPS.GROUP_ID)
                    .fetchOne().value1();

            group.setGroupID(id);

            Collection<IvoryGroupMembersRecord> memberRecs = group.getMembersInternal().stream()
                    .map(ManagedMember::toRecord)
                    .collect(Collectors.toList());
            ctx.batchInsert(memberRecs).execute();
            group.getMembersInternal().forEach(m -> m.setState(CachedState.CLEAN));
        }, async);
    }

    private void updateDirtyGroup(ManagedGroup group, boolean async) {
        if (group.getGroupIDBypassing() == null) {
            LOG.severe("Unable to update " + group + ", state is DIRTY but group doesn't have an id.");
        }

        executeQuery((conf) -> {
            DSLContext ctx = DSL.using(conf);

            ctx.update(IVORY_GROUPS)
                    .set(group.toRecord())
                    .where(IVORY_GROUPS.GROUP_ID.eq(group.getGroupID()))
                    .execute();

            for (ManagedMember member : group.getMembersInternal()) {
                switch (member.getState()) {
                    case DELETED:
                        ctx.executeDelete(member.toRecord());
                        LOG.info("Deleted member " + member + " from " + group);
                        group.getMembers().remove(member.getUUID());
                        break;
                    case DIRTY:
                        ctx.executeUpdate(member.toRecord());
                        LOG.info("Updated member " + member + " from " + group);
                        member.setState(CachedState.CLEAN);
                        break;
                    case NEW:
                        ctx.executeInsert(member.toRecord());
                        LOG.info("Inserted member " + member + " from " + group);
                        member.setState(CachedState.CLEAN);
                        break;
                    case CLEAN:
                        LOG.info("Nothing to do for member " + member + " from " + group);
                        break;
                    default:
                        LOG.severe("Group member " + member + " " + group + " has an invalid state. " + member.getState());
                }
            }

            group.setState(CachedState.CLEAN);
            LOG.fine("Cleaned group " + group);
        }, async);
    }

    void executeQuery(TransactionalRunnable run, boolean async) {
        if (async) {
            Ivory.DBI().asyncSubmit(run);
        } else {
            Ivory.DBI().submit(run);
        }
    }

    Optional<ManagedGroup> loadGroupByID(Long id) {
        Objects.requireNonNull(id, () -> "Group ID cannot be null.");
        LOG.info("Loading group id: " + id);
        return Ivory.DBI().submit((conf) -> {
            DSLContext ctx = DSL.using(conf);
            Optional<IvoryGroupsRecord> grecopt = ctx
                    .selectFrom(IVORY_GROUPS)
                    .where(IVORY_GROUPS.GROUP_ID.eq(id))
                    .fetchOptional();

            if (!grecopt.isPresent()) {
                return Optional.empty();
            } else {
                List<IvoryGroupMembersRecord> memrecs = ctx
                        .selectFrom(IVORY_GROUP_MEMBERS)
                        .where(IVORY_GROUP_MEMBERS.GROUP_ID.eq(id))
                        .fetch();
                ManagedGroup group = new ManagedGroup(grecopt.get(), memrecs);
                LOG.info("Loaded group " + group);
                return Optional.of(group);
            }
        });
    }

    private Optional<ManagedGroup> loadGroupByName(String name) {
        Objects.requireNonNull(name, () -> "Group ID cannot be null.");

        return Ivory.DBI().submit((conf) -> {
            DSLContext ctx = DSL.using(conf);
            Optional<IvoryGroupsRecord> grecopt = ctx
                    .selectFrom(IVORY_GROUPS)
                    .where(IVORY_GROUPS.GROUP_NAME.eq(name))
                    .fetchOptional();

            if (grecopt.isPresent()) {
                return Optional.empty();
            } else {
                IvoryGroupsRecord grouprec = grecopt.get();
                List<IvoryGroupMembersRecord> memrecs = ctx
                        .selectFrom(IVORY_GROUP_MEMBERS)
                        .where(IVORY_GROUP_MEMBERS.GROUP_ID.eq(grouprec.getGroupId()))
                        .fetch();
                ManagedGroup group = new ManagedGroup(grouprec, memrecs);
                LOG.info("Loaded group " + group);
                return Optional.of(group);
            }
        });
    }

    void createToken(String token, Rank rank, ManagedGroup managedGroup, UUID creator, int usages) {
        Ivory.DBI().asyncSubmit((conf) -> {
            DSL.using(conf).insertInto(IVORY_TOKENS)
                    .set(IVORY_TOKENS.GROUP_ID, managedGroup.getGroupIDBypassing())
                    .set(IVORY_TOKENS.CREATOR, creator)
                    .set(IVORY_TOKENS.TOKEN, token)
                    .set(IVORY_TOKENS.USAGES, usages)
                    .set(IVORY_TOKENS.RANK, rank)
                    .onDuplicateKeyUpdate()
                    .set(IVORY_TOKENS.USAGES, usages)
                    .set(IVORY_TOKENS.RANK, rank)
                    .set(IVORY_TOKENS.CREATOR, creator)
                    .execute();
        });
    }

    void createInvite(UUID playerUUID, Rank rank, UUID invitedBy, ManagedGroup group) {
        Ivory.DBI().asyncSubmit((conf) -> {
            DSL.using(conf).insertInto(IVORY_GROUP_INVITES)
                    .set(IVORY_GROUP_INVITES.GROUP_ID, group.getGroupIDBypassing())
                    .set(IVORY_GROUP_INVITES.INVITETIME, new Timestamp(System.currentTimeMillis()))
                    .set(IVORY_GROUP_INVITES.RANK, rank)
                    .set(IVORY_GROUP_INVITES.PUUID, playerUUID)
                    .set(IVORY_GROUP_INVITES.INVITER_PUUID, invitedBy)
                    .set(IVORY_GROUP_INVITES.EXPIRES, new Timestamp(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)))
                    .onDuplicateKeyIgnore()
                    .execute();
        });
    }

    boolean hasInvite(ManagedGroup managedGroup, UUID uniqueId) {
        return Ivory.DBI().submit((conf) -> {
            return DSL.using(conf).selectCount()
                    .from(IVORY_GROUP_INVITES)
                    .where(IVORY_GROUP_INVITES.GROUP_ID.eq(managedGroup.getGroupIDBypassing()))
                    .and(IVORY_GROUP_INVITES.PUUID.eq(uniqueId))
                    .fetchOne(Record1<Integer>::value1);
        }) > 0;
    }

    IvoryGroupInvitesRecord getInviteAndInvalidate(ManagedGroup managedGroup, UUID uniqueId) {
        return Ivory.DBI().submit((conf) -> {
            IvoryGroupInvitesRecord rec = DSL.using(conf)
                    .selectFrom(IVORY_GROUP_INVITES)
                    .where(IVORY_GROUP_INVITES.GROUP_ID.eq(managedGroup.getGroupIDBypassing()))
                    .and(IVORY_GROUP_INVITES.PUUID.eq(uniqueId))
                    .fetchOne();
            DSL.using(conf).executeDelete(rec);
            return rec;
        });
    }

    public IvoryTokensRecord getTokenAndInvalidate(ManagedGroup managedGroup, UUID uniqueId, String token) {
        return Ivory.DBI().submit((conf) -> {
            IvoryTokensRecord rec = DSL.using(conf)
                    .selectFrom(IVORY_TOKENS)
                    .where(IVORY_TOKENS.TOKEN.eq(token))
                    .and(IVORY_TOKENS.GROUP_ID.eq(managedGroup.getGroupIDBypassing()))
                    .fetchOne();

            if (rec.getUsages() > 1) {
                rec.setUsages(rec.getUsages() - 1);
                DSL.using(conf).executeUpdate(rec);
//                rec.update();
            } else {
                DSL.using(conf).executeDelete(rec);
            }

            return rec;
        });
    }

    public boolean hasToken(ManagedGroup managedGroup, String token) {
        return Ivory.DBI().submit((conf) -> {
            return DSL.using(conf).selectCount()
                    .from(IVORY_TOKENS)
                    .where(IVORY_TOKENS.GROUP_ID.eq(managedGroup.getGroupIDBypassing()))
                    .and(IVORY_TOKENS.TOKEN.eq(token))
                    .and(IVORY_TOKENS.USAGES.gt(0))
                    .fetchOne(Record1<Integer>::value1);
        }) > 0;
    }

    OptionalLong nameToID(String name) {
        return Ivory.DBI().submit((conf) -> {
            Optional<Long> idopt = DSL.using(conf)
                    .select(IVORY_GROUPS.GROUP_ID)
                    .from(IVORY_GROUPS)
                    .where(IVORY_GROUPS.GROUP_NAME.eq(name))
                    .fetchOptional()
                    .map(Record1::value1);

            LOG.info("Name " + name + " to ID " + idopt);
            if (idopt.isPresent()) {
                return OptionalLong.of(idopt.get());
            } else {
                return OptionalLong.empty();
            }
        });
    }
}
