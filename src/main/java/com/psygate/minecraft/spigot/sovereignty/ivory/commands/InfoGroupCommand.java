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
import com.psygate.minecraft.spigot.sovereignty.ivory.groups.Member;
import com.psygate.minecraft.spigot.sovereignty.ivory.groups.Rank;
import com.psygate.minecraft.spigot.sovereignty.ivory.managment.GroupManager;
import com.psygate.minecraft.spigot.sovereignty.nucleus.commands.util.CommandException;
import com.psygate.minecraft.spigot.sovereignty.nucleus.commands.util.NucleusPlayerCommand;
import com.psygate.minecraft.spigot.sovereignty.nucleus.util.player.PlayerManager;
import com.psygate.text.Alignment;
import com.psygate.text.TextTable;
import com.psygate.text.TextTableSettings;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

/**
 * Created by psygate (https://github.com/psygate) on 28.01.2016.
 */
public class InfoGroupCommand extends NucleusPlayerCommand {

    public InfoGroupCommand() {
        super(1, 1);
    }

    private final static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    @Override
    protected void subOnCommand(Player player, Command command, String s, String[] args) throws Exception {
        Optional<? extends Group> groupopt = GroupManager.getInstance().getGroup(args[0]);

        if (!groupopt.isPresent()) {
            throw new CommandException("Group not found.");
        } else {
            Group group = groupopt.get();
            if (!group.hasMemberWithRankGE(player.getUniqueId(), Rank.MEMBER) && !player.isOp()) {
                throw new CommandException("Insufficient permission. (Insufficient rank.)");
            } else {
                TextTable message = TextTableSettings.newBuilder().setRenderBorder(false).setAlignment(Alignment.CENTER)
                        .setPad(' ').setColumnSeperator('|').setExpandHeaders(true)
                        .addColumn("Member").addColumn("Rank").addColumn("Joined")
                        .build();

                for (Member member : group.getMembers().values()) {
                    if (member.isHidden()) {
                        continue;
                    }
                    message.add(PlayerManager.getInstance().toName(member.getUUID()));
                    message.add(member.getRank().name());
                    message.add(new Date(member.getJoinTime()).toString());
                }
                player.sendMessage(message.render());
            }
        }
    }

    @Override
    protected String[] getName() {
        return new String[]{"infogroup"};
    }
}
