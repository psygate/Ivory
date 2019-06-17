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

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.psygate.cache.state.CachedState;
import com.psygate.collections.Pair;
import com.psygate.collections.Triplet;
import com.psygate.minecraft.spigot.sovereignty.ivory.Ivory;
import com.psygate.minecraft.spigot.sovereignty.ivory.db.model.tables.records.IvoryGroupMutesRecord;
import com.psygate.minecraft.spigot.sovereignty.ivory.db.model.tables.records.IvoryPlayerMutesRecord;
import com.psygate.minecraft.spigot.sovereignty.ivory.db.model.tables.records.IvoryPlayerSettingsRecord;
import com.psygate.minecraft.spigot.sovereignty.ivory.groups.Group;
import com.psygate.minecraft.spigot.sovereignty.ivory.groups.Member;
import com.psygate.minecraft.spigot.sovereignty.ivory.groups.Rank;
import com.psygate.minecraft.spigot.sovereignty.nucleus.util.WorkerPool;
import com.psygate.minecraft.spigot.sovereignty.nucleus.util.player.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.impl.DSL;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.psygate.minecraft.spigot.sovereignty.ivory.db.model.Tables.*;

/**
 * Created by psygate (https://github.com/psygate) on 28.01.2016.
 */
@SuppressWarnings("unchecked")
public class GroupManager {
    private static GroupManager instance;
    private final static Logger LOG = Ivory.getLogger(GroupManager.class.getName());

    private GroupManager() {

    }

    public static GroupManager getInstance() {
        if (instance == null) {
            instance = new GroupManager();
        }

        return instance;
    }

    private final LoadingCache<Long, Optional<ManagedGroup>> groupCache = Ivory.getInstance().getConf().getCacheBuilder()
            .removalListener(new RemovalListener<Long, Optional<ManagedGroup>>() {
                @Override
                public void onRemoval(RemovalNotification<Long, Optional<ManagedGroup>> removalNotification) {
                    removalNotification.getValue().ifPresent(ManagedGroup::persistChanges);
                }
            })
            .build(new CacheLoader<Long, Optional<ManagedGroup>>() {
                @Override
                public Optional<ManagedGroup> load(Long id) throws Exception {
                    return GroupIO.getInstance().loadGroupByID(id);
                }
            });

    private final LoadingCache<String, OptionalLong> nameCache = Ivory.getInstance().getConf().getCacheBuilder()
            .build(new CacheLoader<String, OptionalLong>() {
                @Override
                public OptionalLong load(String s) throws Exception {
                    return GroupIO.getInstance().nameToID(s);
                }
            });

    private final LoadingCache<UUID, Set<Long>> ignoredGroups = Ivory.getInstance().getConf().getCacheBuilder()
            .removalListener((RemovalListener<UUID, Set<Long>>) removalNotification -> {
                Ivory.DBI().asyncSubmit((conf) -> {
                    DSLContext ctx = DSL.using(conf);
                    ctx.deleteFrom(IVORY_GROUP_MUTES).where(IVORY_GROUP_MUTES.PUUID.eq(removalNotification.getKey())).execute();
                    List<IvoryGroupMutesRecord> mutes = removalNotification.getValue()
                            .stream()
                            .map(l -> new IvoryGroupMutesRecord(l, removalNotification.getKey()))
                            .collect(Collectors.toList());
                    ctx.batchInsert(mutes).execute();
                });
            })
            .build(new CacheLoader<UUID, Set<Long>>() {
                @Override
                public Set<Long> load(UUID uuid) throws Exception {
                    return PlayerIO.getInstance().getIgnoredGroups(uuid);
                }
            });

    private final LoadingCache<UUID, Set<UUID>> ignoredPlayers = Ivory.getInstance().getConf().getCacheBuilder()
            .removalListener((RemovalListener<UUID, Set<UUID>>) removalNotification -> {
                HashSet<UUID> copy = new HashSet<UUID>(removalNotification.getValue());
                Ivory.DBI().asyncSubmit((conf) -> {
                    DSLContext ctx = DSL.using(conf);
                    ctx.deleteFrom(IVORY_PLAYER_MUTES).where(IVORY_PLAYER_MUTES.PUUID.eq(removalNotification.getKey())).execute();
                    List<IvoryPlayerMutesRecord> mutes = copy
                            .stream()
                            .map(l -> new IvoryPlayerMutesRecord(l, removalNotification.getKey()))
                            .collect(Collectors.toList());
                    ctx.batchInsert(mutes).execute();
                });
            })
            .build(new CacheLoader<UUID, Set<UUID>>() {
                @Override
                public Set<UUID> load(UUID uuid) throws Exception {
                    return PlayerIO.getInstance().getIgnoredPlayers(uuid);
                }
            });

    private final LoadingCache<UUID, IvoryPlayerSettingsRecord> settingsCache = Ivory.getInstance().getConf().getCacheBuilder()
            .removalListener((RemovalListener<UUID, IvoryPlayerSettingsRecord>) removalNotification -> {
                Ivory.DBI().asyncSubmit((conf) -> {
                    DSLContext ctx = DSL.using(conf);
                    ctx.insertInto(IVORY_PLAYER_SETTINGS)
                            .set(new IvoryPlayerSettingsRecord(removalNotification.getKey(), removalNotification.getValue().getAutoacceptBool()))
                            .onDuplicateKeyUpdate()
                            .set(new IvoryPlayerSettingsRecord(removalNotification.getKey(), removalNotification.getValue().getAutoacceptBool()))
                            .execute();
                });
            })
            .build(new CacheLoader<UUID, IvoryPlayerSettingsRecord>() {
                @Override
                public IvoryPlayerSettingsRecord load(UUID uuid) throws Exception {
                    return PlayerIO.getInstance().getPlayerSettings(uuid);
                }
            });


    public Optional<? extends Group> getGroup(String name) {
        OptionalLong id = nameCache.getUnchecked(name);

        if (id.isPresent()) {
            return groupCache.getUnchecked(Long.valueOf(id.getAsLong()));
        } else {
            return Optional.empty();
        }
    }

    public Optional<? extends Group> getGroup(Long id) {
        Optional<ManagedGroup> opt = groupCache.getUnchecked(id);
        if (opt.isPresent() && opt.get().getState() == CachedState.DELETED) {
            return Optional.empty();
        } else {
            return opt;
        }
    }

    public Optional<? extends Group> createGroup(String name, UUID uuid) {
        try {
            if (nameCache.get(name).isPresent()) {
                LOG.info("Group " + name + " is already in the name cache.");
                return Optional.empty();
            } else {
                ManagedGroup group = new ManagedGroup(
                        name,
                        uuid
                );

                group.addMember(uuid, Rank.CREATOR, uuid, false);
                GroupIO.getInstance().persist(group, false);
                assert group.getState() == CachedState.CLEAN;
                assert group.getMembersInternal().stream().map(ManagedMember::getState).allMatch(v -> v == CachedState.CLEAN);
                Optional<ManagedGroup> opt = groupCache.getIfPresent(group.getGroupID());

                if (opt != null && opt.isPresent()) {
                    LOG.severe("Anomaly on " + group + ", already in cache.");
                    return Optional.empty();
                } else {
                    groupCache.put(group.getGroupID(), Optional.of(group));
                    nameCache.put(group.getName(), OptionalLong.of(group.getGroupID()));
                }
                return groupCache.getUnchecked(group.getGroupID());
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public void getInvitesForPlayer(UUID uniqueId, Consumer<List<Pair<String, String>>> con) {
        Ivory.DBI().asyncSubmit((conf) -> {
            // Load database state.
            List<Pair<String, String>> list = DSL.using(conf)
                    .select(IVORY_GROUPS.GROUP_NAME, IVORY_GROUP_INVITES.INVITER_PUUID)
                    .from(IVORY_GROUP_INVITES)
                    .join(IVORY_GROUPS)
                    .using(IVORY_GROUPS.GROUP_ID)
                    .where(IVORY_GROUP_INVITES.PUUID.eq(uniqueId))
                    .fetch()
                    .map(rec -> new Pair<>(
                            rec.value1(),
                            PlayerManager.getInstance().toName(rec.value2())
                    ));
            con.accept(list);

        });
    }

    public void getGroupsForPlayer(UUID uniqueId, Consumer<List<Pair<String, Rank>>> con) {
        Ivory.DBI().asyncSubmit((conf) -> {
            // Load database state.
            List<Triplet<Long, String, Rank>> list = DSL.using(conf)
                    .select(IVORY_GROUP_MEMBERS.GROUP_ID, IVORY_GROUPS.GROUP_NAME, IVORY_GROUP_MEMBERS.RANK)
                    .from(IVORY_GROUP_MEMBERS)
                    .join(IVORY_GROUPS)
                    .using(IVORY_GROUPS.GROUP_ID)
                    .where(IVORY_GROUP_MEMBERS.PUUID.eq(uniqueId))
                    .fetch(record -> new Triplet<>(
                            record.getValue(IVORY_GROUP_MEMBERS.GROUP_ID),
                            record.getValue(IVORY_GROUPS.GROUP_NAME),
                            record.getValue(IVORY_GROUP_MEMBERS.RANK)
                    ));
            LOG.info("Database state for " + uniqueId + " " + list);

            WorkerPool.submit(() -> {
                Map<String, Rank> actualList = new HashMap<>();

                for (Triplet<Long, String, Rank> trip : list) {
                    Optional<ManagedGroup> gopt = groupCache.getUnchecked(trip.getKey());

                    if (gopt.isPresent()) {
                        ManagedGroup group = gopt.get();
                        if (group.hasMember(uniqueId)) {
                            Optional<Member> memopt = group.getMember(uniqueId);

                            if (memopt.isPresent()) {
                                actualList.put(trip.getValue1(), memopt.get().getRank());
                            }
                        }
                    }
                }

                LOG.info("State after cache: " + actualList);
                List<Optional<ManagedGroup>> keys = new LinkedList<>(groupCache.asMap().values());

                LOG.info("Querying cache: " + keys);

                for (Optional<ManagedGroup> gopt : keys) {
                    if (gopt != null && gopt.isPresent()) {
                        ManagedGroup group = gopt.get();
                        Optional<Member> memopt = group.getMember(uniqueId);

                        if (memopt.isPresent()) {
                            actualList.put(group.getName(), memopt.get().getRank());
                            LOG.info("Updating with: " + memopt.get());
                        }
                    }
                }
                con.accept(actualList.entrySet().stream().map(en -> new Pair<>(en.getKey(), en.getValue())).collect(Collectors.toList()));
            });
        });
    }

    public void ignoreGroup(UUID uniqueId, String name) {
        getGroup(name).ifPresent(g -> ignoredGroups.getUnchecked(uniqueId).add(g.getGroupID()));
    }

    public void ignorePlayer(UUID uniqueId, UUID ignored) {
        ignoredPlayers.getUnchecked(uniqueId).add(ignored);
    }

    public boolean isIgnoring(UUID playerUUID, UUID uniqueId) {
        return ignoredPlayers.getUnchecked(playerUUID).contains(uniqueId);
    }


    public boolean isIgnoring(UUID playerUUID, Group group) {
        return ignoredGroups.getUnchecked(playerUUID).contains(group.getGroupID());
    }

    public boolean isAutoAcceptInvite(UUID playerUUID) {
        return settingsCache.getUnchecked(playerUUID).getAutoacceptBool();
    }

    public Set<Long> getGroupMutes(UUID player) {
        return ignoredGroups.getUnchecked(player);
    }

    public Set<UUID> getPlayerMutes(UUID player) {
        return ignoredPlayers.getUnchecked(player);
    }

    public void togglePlayerAutoAccept(UUID player) {
        IvoryPlayerSettingsRecord rec = settingsCache.getUnchecked(player);
        rec.setAutoacceptBool(!rec.getAutoacceptBool());
    }

    public int getOwnedGroupCount(UUID player) {
        return Ivory.DBI().submit(conf -> {
            return DSL.using(conf)
                    .selectCount()
                    .from(IVORY_GROUPS)
                    .where(IVORY_GROUPS.CREATOR_PUUID.eq(player))
                    .fetchOne(Record1<Integer>::value1);
        });
    }

    public void flush() {
        Bukkit.getOnlinePlayers().stream().filter(Player::isOp)
                .forEach(p -> p.sendMessage(ChatColor.YELLOW + "Ivory cache flush initiated."));
        GroupIO.getInstance().setAsync(false);
        groupCache.invalidateAll();
        nameCache.invalidateAll();
        ignoredGroups.invalidateAll();
        ignoredPlayers.invalidateAll();
        settingsCache.invalidateAll();
        LOG.info("Ivory cache flush done.");
        GroupIO.getInstance().setAsync(true);
        Bukkit.getOnlinePlayers().stream().filter(Player::isOp)
                .forEach(p -> p.sendMessage(ChatColor.GREEN + "Ivory cache flush done."));
    }

    public void unignoreGroup(UUID uniqueId, Long id) {
        ignoredGroups.getUnchecked(uniqueId).remove(id);
    }

    public void unignorePlayer(UUID uniqueId, UUID uuid) {
        ignoredPlayers.getUnchecked(uniqueId).remove(uuid);
    }

    public void cleanup() {
        Ivory.DBI().asyncSubmit((conf) -> {
            DSLContext ctx = DSL.using(conf);

            try (Cursor<Record1<Long>> groups = ctx.select(IVORY_GROUPS.GROUP_ID).from(IVORY_GROUPS).fetchLazy()) {
                while (groups.hasNext()) {
                    Long id = groups.fetchOne(Record1<Long>::value1);
                    int count = ctx.selectCount().from(IVORY_GROUP_MEMBERS).where(IVORY_GROUP_MEMBERS.GROUP_ID.eq(id)).fetchOne(Record1<Integer>::value1);
                    if (count <= 0) {
                        ctx.deleteFrom(IVORY_GROUPS).where(IVORY_GROUPS.GROUP_ID.eq(id)).execute();
                    }
                }
            }
            System.out.println("Ivory database cleanup done.");
        });
    }
}