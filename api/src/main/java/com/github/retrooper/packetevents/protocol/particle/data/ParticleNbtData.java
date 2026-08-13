/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2026 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.retrooper.packetevents.protocol.particle.data;

import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.Objects;

/**
 * Opaque NBT data belonging to a particle type unknown to PacketEvents.
 */
@NullMarked
public class ParticleNbtData extends ParticleData {

    private NBTCompound tags;

    public ParticleNbtData(NBTCompound tags) {
        this.tags = tags;
    }

    @ApiStatus.Internal
    public static ParticleNbtData decode(NBTCompound compound, ClientVersion version) {
        NBTCompound tags = compound.copy();
        tags.removeTag("type");
        return new ParticleNbtData(tags);
    }

    @ApiStatus.Internal
    public static void encode(ParticleNbtData data, ClientVersion version, NBTCompound compound) {
        for (Map.Entry<String, NBT> entry : data.tags.getTags().entrySet()) {
            compound.setTag(entry.getKey(), entry.getValue().copy());
        }
    }

    public NBTCompound getTags() {
        return this.tags;
    }

    public void setTags(NBTCompound tags) {
        this.tags = tags;
    }

    @Override
    public boolean isEmpty() {
        return this.tags.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ParticleNbtData)) return false;
        ParticleNbtData that = (ParticleNbtData) obj;
        return this.tags.equals(that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.tags);
    }

    @Override
    public String toString() {
        return "ParticleNbtData[" + this.tags + ']';
    }
}
