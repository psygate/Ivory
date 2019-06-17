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
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.Optional;

/**
 * Created by psygate (https://github.com/psygate) on 28.01.2016.
 */
public class CreateTokenCommand extends NucleusPlayerCommand {

    public CreateTokenCommand() {
        super(3, 4);
    }

    private final static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    @Override
    protected void subOnCommand(Player player, Command command, String s, String[] args) throws Exception {
        int usages = Integer.MAX_VALUE;
        String groupname = args[0];
        String token = args[1];
        Rank rank;
        try {
            rank = Rank.valueOf(args[2]);
        } catch (Exception e) {
            throw new CommandException("Invalid rank.");
        }

        if (args.length > 3) {
            try {
                usages = Integer.parseInt(args[3]);
            } catch (Exception e) {
                throw new CommandException("Invalid usage number.");
            }
        }

        Optional<? extends Group> group = GroupManager.getInstance().getGroup(groupname);


        if (!group.isPresent()) {
            player.sendMessage(ChatColor.RED + "Group not found.");
        } else {
            if (!group.get().hasMemberWithRankGE(player.getUniqueId(), Rank.ADMIN)
                    || !group.get().hasMemberWithRankGE(player.getUniqueId(), rank)) {
                throw new CommandException("Insufficient permission. (Insufficient rank)");
            } else {
                group.get().addToken(token, rank, player.getUniqueId(), usages);
                player.sendMessage(ChatColor.GREEN + "Token created.");
            }
        }
    }

    @Override
    protected String[] getName() {
        return new String[]{"creategrouptoken"};
    }
}
