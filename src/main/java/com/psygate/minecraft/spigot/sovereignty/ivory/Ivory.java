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

import com.psygate.minecraft.spigot.sovereignty.ivory.managment.GroupManager;
import com.psygate.minecraft.spigot.sovereignty.nucleus.Nucleus;
import com.psygate.minecraft.spigot.sovereignty.nucleus.managment.NucleusPlugin;
import com.psygate.minecraft.spigot.sovereignty.nucleus.sql.DatabaseInterface;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.*;

/**
 * Created by psygate (https://github.com/psygate) on 28.01.2016.
 */
public class Ivory extends JavaPlugin implements NucleusPlugin {
    private final static Logger LOG = Logger.getLogger(Ivory.class.getName());
    private static Ivory instance;
    private DatabaseInterface DBI;
    private Configuration conf;

    static {
        LOG.setUseParentHandlers(false);
        LOG.setLevel(Level.ALL);
        List<Handler> handlers = Arrays.asList(LOG.getHandlers());

        if (handlers.stream().noneMatch(h -> h instanceof FileHandler)) {
            try {
                File logdir = new File("logs/nucleus_logs/ivory/");
                if (!logdir.exists()) {
                    logdir.mkdirs();
                }
                FileHandler fh = new FileHandler(
                        "logs/nucleus_logs/ivory/ivory.%u.%g.log",
                        8 * 1024 * 1024,
                        12,
                        true
                );
                fh.setLevel(Level.ALL);
                fh.setEncoding("UTF-8");
                SimpleFormatter formatter = new SimpleFormatter();
                fh.setFormatter(formatter);
                LOG.addHandler(fh);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Logger getLogger(String name) {
        Logger log = Logger.getLogger(name);
        log.setParent(LOG);
        log.setUseParentHandlers(true);
        log.setLevel(Level.ALL);
        return log;
    }

    public static Ivory getInstance() {
        if (instance == null) {
            throw new IllegalStateException();
        }

        return instance;
    }

    public static DatabaseInterface DBI() {
        return getInstance().DBI;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        conf = new Configuration(getConfig());

        Nucleus.getInstance().register(this);
        GroupManager.getInstance().cleanup();
        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> GroupManager.getInstance().flush(), 20 * 60 * 30, 20 * 60 * 30);
    }

    @Override
    public void onDisable() {
        GroupManager.getInstance().flush();
    }

    public int getMaxGroups() {
        return conf.getMaxGroups();
    }

    public Set<String> getReservedPrefixes() {
        return conf.getReservedPrefixes();
    }

    public Configuration getConf() {
        return conf;
    }

    @Override
    public int getWantedDBVersion() {
        return 1;
    }

    @Override
    public void fail() {
        LOG.severe("Failed to load ivory.");
        Bukkit.shutdown();
    }

    @Override
    public void setLogger(Logger logger) {

    }

    @Override
    public Logger getPluginLogger() {
        return LOG;
    }

    @Override
    public List<Listener> getListeners() {
        return Collections.emptyList();
    }

    @Override
    public void setDatabaseInterface(DatabaseInterface databaseInterface) {
        DBI = databaseInterface;
    }

}
