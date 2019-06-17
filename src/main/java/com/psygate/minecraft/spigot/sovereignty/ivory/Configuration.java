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

package com.psygate.minecraft.spigot.sovereignty.ivory;

import com.google.common.cache.CacheBuilder;
import com.psygate.minecraft.spigot.sovereignty.nucleus.sql.util.TimeUtil;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by psygate on 03.05.2016.
 */
public class Configuration {
    /*
    group_cache:
  expire_after_write: 20m
  expire_after_access: 20m
  initial_capacity: 100
  maximum_size: 1000
     */
    private long expireAfterWrite;
    private long expireAfterAccess;
    private int initialCapacity;
    private int maximumSize;
    private Set<String> reservedPrefixes = new HashSet<>();
    private int maxGroups;

    public Configuration(FileConfiguration conf) {
        reservedPrefixes.addAll(conf.getStringList("reserved_prefixes"));
        maxGroups = conf.getInt("group_limit");
        expireAfterWrite = TimeUtil.parseTimeStringToMillis(conf.getString("group_cache.expire_after_write"));
        expireAfterAccess = TimeUtil.parseTimeStringToMillis(conf.getString("group_cache.expire_after_access"));
        initialCapacity = conf.getInt("group_cache.expire_after_access");
        maximumSize = conf.getInt("group_cache.maximum_size");
    }

    public long getExpireAfterWrite() {
        return expireAfterWrite;
    }

    public void setExpireAfterWrite(long expireAfterWrite) {
        this.expireAfterWrite = expireAfterWrite;
    }

    public long getExpireAfterAccess() {
        return expireAfterAccess;
    }

    public void setExpireAfterAccess(long expireAfterAccess) {
        this.expireAfterAccess = expireAfterAccess;
    }

    public int getInitialCapacity() {
        return initialCapacity;
    }

    public void setInitialCapacity(int initialCapacity) {
        this.initialCapacity = initialCapacity;
    }

    public int getMaximumSize() {
        return maximumSize;
    }

    public void setMaximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
    }

    public Set<String> getReservedPrefixes() {
        return reservedPrefixes;
    }

    public void setReservedPrefixes(Set<String> reservedPrefixes) {
        this.reservedPrefixes = reservedPrefixes;
    }

    public int getMaxGroups() {
        return maxGroups;
    }

    public void setMaxGroups(int maxGroups) {
        this.maxGroups = maxGroups;
    }

    @SuppressWarnings("unchecked")
    public <K, V> CacheBuilder getCacheBuilder() {
        return (CacheBuilder<K, V>) CacheBuilder.newBuilder()
                .expireAfterAccess(expireAfterAccess, TimeUnit.MILLISECONDS)
                .expireAfterWrite(expireAfterWrite, TimeUnit.MILLISECONDS)
                .initialCapacity(initialCapacity)
                .maximumSize(maximumSize);
    }
}
