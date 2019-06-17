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
import java.util.Optional;

/**
 * Created by psygate (https://github.com/psygate) on 28.01.2016.
 */
public class JoinGroupCommand extends NucleusPlayerCommand {

    public JoinGroupCommand() {
        super(1, 2);
    }

    private final static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    @Override
    protected void subOnCommand(Player player, Command command, String s, String[] args) throws Exception {
        String groupname = args[0];
        String token = (args.length == 2) ? args[1] : null;

        Optional<? extends Group> groupopt = GroupManager.getInstance().getGroup(groupname);

        if (!groupopt.isPresent()) {
            throw new CommandException("Group not found.");
        } else {
            Group group = groupopt.get();
            if (group.hasMember(player.getUniqueId())) {
                throw new CommandException(ChatColor.YELLOW + "Already member of group.");
            } else if (player.isOp()) {
                group.addMember(player.getUniqueId(), Rank.ADMIN, player.getUniqueId(), true);
                player.sendMessage(ChatColor.GREEN + "Group " + group.getName() + " joined by op override.");
            } else if (group.hasInvite(player.getUniqueId())) {
                group.joinPerInvite(player.getUniqueId());
                player.sendMessage(ChatColor.GREEN + "Group " + group.getName() + " joined per invite.");
                group.getMembers().values().forEach(mem -> {
                    if (mem.getRank().ge(Rank.MODERATOR)) {
                        Player p = Bukkit.getPlayer(mem.getUUID());
                        p.sendMessage(ChatColor.GREEN + player.getName()
                                + " joined "
                                + group.getName()
                                + " by invite by "
                                + PlayerManager.getInstance().toName(mem.getInvitedBy()));
                    }
                });
            } else if (token == null) {
                throw new CommandException("No token supplied.");
            } else if (group.hasToken(token)) {
                group.joinPerToken(player.getUniqueId(), token);
                player.sendMessage(ChatColor.GREEN + "Group " + group.getName() + " joined per token.");
                group.getMembers().values().forEach(mem -> {
                    if (mem.getRank().ge(Rank.MODERATOR)) {
                        Player p = Bukkit.getPlayer(mem.getUUID());
                        p.sendMessage(ChatColor.GREEN + player.getName()
                                + " joined "
                                + group.getName()
                                + " by token by "
                                + PlayerManager.getInstance().toName(mem.getInvitedBy()));
                    }
                });
            } else {
                throw new CommandException("Invalid token.");
            }
        }

    }

    @Override
    protected String[] getName() {
        return new String[]{"joingroup"};
    }
}
