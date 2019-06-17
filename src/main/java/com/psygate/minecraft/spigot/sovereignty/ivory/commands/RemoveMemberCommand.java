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

import com.psygate.minecraft.spigot.sovereignty.ivory.groups.Group;
import com.psygate.minecraft.spigot.sovereignty.ivory.groups.Rank;
import com.psygate.minecraft.spigot.sovereignty.ivory.managment.GroupManager;
import com.psygate.minecraft.spigot.sovereignty.nucleus.commands.util.CommandException;
import com.psygate.minecraft.spigot.sovereignty.nucleus.commands.util.NucleusPlayerCommand;
import com.psygate.minecraft.spigot.sovereignty.nucleus.util.player.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by psygate (https://github.com/psygate) on 28.01.2016.
 */
public class RemoveMemberCommand extends NucleusPlayerCommand {

    public RemoveMemberCommand() {
        super(2, 2);
    }

    private final static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    @Override
    protected void subOnCommand(Player player, Command command, String s, String[] args) throws Exception {
        String groupname = args[0];
        String membername = args[1];

        if (membername == player.getName()) {
            throw new CommandException("Cannot remove self. Use leavegroup.");
        }

        Optional<? extends Group> groupopt = GroupManager.getInstance().getGroup(groupname);
        UUID memuuid;
        try {
            memuuid = PlayerManager.getInstance().toUUID(membername);
        } catch (NoSuchElementException e) {
            throw new CommandException("Player \"" + membername + "\" not found.");
        }
        if (!groupopt.isPresent()) {
            throw new CommandException("Group not found.");
        } else {
            Group group = groupopt.get();

            if (group.getCreator().equals(memuuid)) {
                throw new CommandException("Unable to remove creator.");
            } else if (!group.hasMemberWithRankGE(player.getUniqueId(), Rank.MODERATOR)) {
                throw new CommandException("Insufficient permission (Insufficient rank)");
            } else if (!group.hasMember(memuuid)) {
                throw new CommandException("Group member not found.");
            } else if (group.getMembers().get(memuuid).getRank().ge(group.getMembers().get(player.getUniqueId()).getRank())) {
                throw new CommandException("Insufficient permission (Member outrank)");
            } else if (group.getCreator().equals(memuuid)) {
                throw new CommandException("Insufficient permission (Creator cannot be removed)");
            } else {
                group.removeMember(memuuid);//getMembers().(memuuid).setRank(newrank);
                player.sendMessage(ChatColor.GREEN + "Member removed.");

                Player p = Bukkit.getPlayer(memuuid);
                if (p != null) {
                    p.sendMessage(ChatColor.YELLOW + player.getName() + " removed you from " + group.getName());
                }
            }
        }
    }

    @Override
    protected String[] getName() {
        return new String[]{"removemember"};
    }
}
