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

package com.github.retrooper.packetevents.test;

import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTFloat;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.biome.BiomeEffects;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.test.base.BaseDummyAPITest;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParticleCodecTest extends BaseDummyAPITest {

    @Test
    public void decodeCustomBiomeParticle() {
        PacketWrapper<?> wrapper = PacketWrapper.createDummyWrapper(ClientVersion.V_1_20);

        NBTCompound particleTag = new NBTCompound();
        particleTag.setTag("type", new NBTString("alexscaves:sugar_flake"));

        NBTCompound settingsTag = new NBTCompound();
        settingsTag.setTag("options", particleTag);
        settingsTag.setTag("probability", new NBTFloat(0.0015f));

        BiomeEffects.ParticleSettings settings = BiomeEffects.ParticleSettings.CODEC.decode(settingsTag, wrapper);

        assertEquals(new ResourceLocation("alexscaves:sugar_flake"), settings.getParticle().getType().getName());
        assertEquals(settingsTag, BiomeEffects.ParticleSettings.CODEC.encode(wrapper, settings));
    }

    @Test
    public void preserveUnknownParticleNbtData() {
        PacketWrapper<?> wrapper = PacketWrapper.createDummyWrapper(ClientVersion.V_1_20);

        NBTCompound particleTag = new NBTCompound();
        particleTag.setTag("type", new NBTString("example:custom_particle"));
        particleTag.setTag("custom_data", new NBTInt(42));

        Particle<?> particle = Particle.CODEC.decode(particleTag, wrapper);

        assertEquals(new ResourceLocation("example:custom_particle"), particle.getType().getName());
        assertEquals(particleTag, Particle.CODEC.encode(wrapper, particle));
    }
}
