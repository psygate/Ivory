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

package com.psygate.minecraft.spigot.sovereignty.ivory.commands;

import com.psygate.minecraft.spigot.sovereignty.ivory.managment.GroupManager;
import com.psygate.minecraft.spigot.sovereignty.nucleus.commands.util.CommandException;
import com.psygate.minecraft.spigot.sovereignty.nucleus.commands.util.NucleusPlayerCommand;
import com.psygate.minecraft.spigot.sovereignty.nucleus.util.player.PlayerManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.NoSuchElementException;

/**
 * Created by psygate (https://github.com/psygate) on 28.01.2016.
 */
public class IgnorePlayerCommand extends NucleusPlayerCommand {

    public IgnorePlayerCommand() {
        super(1, 1);
    }

    private final static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    @Override
    protected void subOnCommand(Player player, Command command, String s, String[] args) throws Exception {
        String name = args[0];
        try {
            GroupManager.getInstance().ignorePlayer(player.getUniqueId(), PlayerManager.getInstance().toUUID(name));
            player.sendMessage(ChatColor.GREEN + "Player ignored.");
        } catch (NoSuchElementException e) {
            throw new CommandException("Player \"" + name + "\" not found.");
        }
    }

    @Override
    protected String[] getName() {
        return new String[]{"ignoreplayer"};
    }
}
