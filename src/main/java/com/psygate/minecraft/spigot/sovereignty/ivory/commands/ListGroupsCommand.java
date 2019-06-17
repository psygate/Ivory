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

import com.psygate.collections.Pair;
import com.psygate.minecraft.spigot.sovereignty.ivory.Ivory;
import com.psygate.minecraft.spigot.sovereignty.ivory.groups.Rank;
import com.psygate.minecraft.spigot.sovereignty.ivory.managment.GroupManager;
import com.psygate.minecraft.spigot.sovereignty.nucleus.commands.util.NucleusPlayerCommand;
import com.psygate.minecraft.spigot.sovereignty.nucleus.util.WorkerPool;
import com.psygate.text.Alignment;
import com.psygate.text.TextTable;
import com.psygate.text.TextTableSettings;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.jooq.impl.DSL;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static com.psygate.minecraft.spigot.sovereignty.ivory.db.model.Tables.IVORY_GROUPS;
import static com.psygate.minecraft.spigot.sovereignty.ivory.db.model.Tables.IVORY_GROUP_MEMBERS;

/**
 * Created by psygate (https://github.com/psygate) on 28.01.2016.
 */
public class ListGroupsCommand extends NucleusPlayerCommand {

    public ListGroupsCommand() {
        super(0, 0);
    }

    private final static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    @Override
    protected void subOnCommand(Player player, Command command, String s, String[] args) throws Exception {
        GroupManager.getInstance().getGroupsForPlayer(player.getUniqueId(), (list) -> {
            TextTable message = TextTableSettings.newBuilder().setRenderBorder(false).setAlignment(Alignment.CENTER)
                    .setPad(' ').setColumnSeperator('|').setExpandHeaders(true)
                    .addColumn("Group").addColumn("Rank")
                    .build();
            for (Pair<String, Rank> p : list) {
                message.add(p.getKey()).add(p.getValue().name());
            }

            String[] lines = message.render();
            lines[0] = ChatColor.YELLOW + lines[0];

            player.sendMessage(lines);
        });

    }

    @Override
    protected String[] getName() {
        return new String[]{"listgroups"};
    }
}
