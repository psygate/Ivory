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

package com.psygate.minecraft.spigot.sovereignty.ivory.groups;

import java.util.UUID;

/**
 * Created by psygate (https://github.com/psygate) on 29.01.2016.
 */
public interface Member {
    boolean isHidden();

    Rank getRank();

    boolean setRank(Rank rank);

    UUID getUUID();

    UUID getInvitedBy();

    Group getGroup();

    void setHidden(boolean hidden);

    long getJoinTime();
}
