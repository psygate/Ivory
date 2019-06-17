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
import com.psygate.minecraft.spigot.sovereignty.ivory.db.model.tables.records.*;
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

/**
 * Created by psygate on 05.05.2016.
 */
class PlayerIO {
    private final static Logger LOG = Ivory.getLogger(PlayerIO.class.getName());
    private static PlayerIO instance;
    private boolean async = true;

    private PlayerIO() {

    }

    public static PlayerIO getInstance() {
        if (instance == null) {
            instance = new PlayerIO();
        }

        return instance;
    }

    void setAsync(boolean async) {
        this.async = async;
    }


    IvoryPlayerSettingsRecord getPlayerSettings(UUID uuid) {
        return Ivory.DBI().submit((conf) -> {
            return DSL.using(conf).selectFrom(IVORY_PLAYER_SETTINGS)
                    .where(IVORY_PLAYER_SETTINGS.PUUID.eq(uuid))
                    .fetchOptional()
                    .orElseGet(() -> new IvoryPlayerSettingsRecord(uuid, false));
        });
    }


    Set<UUID> getIgnoredPlayers(UUID uuid) {
        return Ivory.DBI().submit((conf) -> {
            return DSL.using(conf).selectFrom(IVORY_PLAYER_MUTES)
                    .where(IVORY_PLAYER_MUTES.PUUID.eq(uuid))
                    .fetchSet(IVORY_PLAYER_MUTES.MUTED_PUUID);
        });
    }

    Set<Long> getIgnoredGroups(UUID uuid) {
        return Ivory.DBI().submit((conf) -> {
            return DSL.using(conf).selectFrom(IVORY_GROUP_MUTES)
                    .where(IVORY_GROUP_MUTES.PUUID.eq(uuid))
                    .fetchSet(IVORY_GROUP_MUTES.GROUP_ID);
        });
    }
}
