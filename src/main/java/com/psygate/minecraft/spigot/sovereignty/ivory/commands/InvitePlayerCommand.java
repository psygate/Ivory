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
public class InvitePlayerCommand extends NucleusPlayerCommand {

    public InvitePlayerCommand() {
        super(2, 2);
    }

    private final static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    @Override
    protected void subOnCommand(Player player, Command command, String s, String[] args) throws Exception {
        String name = args[1];
        String groupname = args[0];
        try {
            UUID playerUUID = PlayerManager.getInstance().toUUID(name);
            if (!GroupManager.getInstance().isIgnoring(playerUUID, player.getUniqueId())) {
                Optional<? extends Group> groupopt = GroupManager.getInstance().getGroup(groupname);
                if (!groupopt.isPresent()) {
                    throw new CommandException("Group not found.");
                } else {
                    Group group = groupopt.get();

                    if (GroupManager.getInstance().isIgnoring(playerUUID, group)) {
                        throw new CommandException("Insufficient permission.");
                    }

                    if (!group.hasMemberWithRankGE(player.getUniqueId(), Rank.MODERATOR) && !player.isOp()) {
                        throw new CommandException("Insufficient permission (Insufficient Rank for invite)");
                    } else if (group.hasMember(playerUUID)) {
                        throw new CommandException("Already a member.");
                    } else if (GroupManager.getInstance().isAutoAcceptInvite(playerUUID)) {
                        group.addMember(playerUUID, Rank.GUEST, player.getUniqueId(), false);
                        player.sendMessage(ChatColor.GREEN + "Player added.");
                        Player p = Bukkit.getPlayer(playerUUID);
                        if (p != null) {
                            p.sendMessage(ChatColor.GREEN + "Joined " + group + "");
                        }
                    } else {
                        group.invite(playerUUID, Rank.GUEST, player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + "Player invited.");
                        Player p = Bukkit.getPlayer(playerUUID);
                        if (p != null) {
                            p.sendMessage(ChatColor.GREEN + player.getName() + " invited you to join " + groupname);
                        }
                    }
                }
            } else {
                throw new CommandException("Insufficient permission.");
            }
        } catch (NoSuchElementException e) {
            System.err.println("Unable to resolve player name to UUID: " + name + " (" + e.getMessage() + ")");
            throw new CommandException("Player \"" + name + "\" not found.");
        }
    }

    @Override
    protected String[] getName() {
        return new String[]{"inviteplayer"};
    }
}
