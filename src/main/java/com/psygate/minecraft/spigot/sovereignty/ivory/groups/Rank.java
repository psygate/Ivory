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

/**
 * Created by psygate (https://github.com/psygate) on 29.01.2016.
 */
public enum Rank {
    GUEST(0),
    MEMBER(10),
    MODERATOR(20),
    ADMIN(30),
    CREATOR(40);

    public final int rankOrdinal;

    Rank(int rankOrdinal) {
        this.rankOrdinal = rankOrdinal;
    }

    public static Rank byOrdinal(Integer integer) {
        for (Rank rank : values()) {
            if (integer.intValue() == rank.rankOrdinal) {
                return rank;
            }
        }

        throw new IllegalArgumentException("No rank for orginal: " + integer);
    }

    public boolean lt(Rank other) {
        return rankOrdinal < other.rankOrdinal;
    }

    public boolean gt(Rank other) {
        return rankOrdinal > other.rankOrdinal;
    }

    public boolean le(Rank other) {
        return rankOrdinal <= other.rankOrdinal;
    }

    public boolean ge(Rank other) {
        return rankOrdinal >= other.rankOrdinal;
    }

    public int compare(Rank other) {
        if (lt(other)) {
            return -1;
        } else if (gt(other)) {
            return 1;
        } else {
            return 0;
        }
    }
}