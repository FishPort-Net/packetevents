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

import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTFloat;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.biome.Biome;
import com.github.retrooper.packetevents.protocol.world.biome.BiomeEffects;
import com.github.retrooper.packetevents.protocol.world.biome.Biomes;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.test.base.BaseDummyAPITest;
import com.github.retrooper.packetevents.util.mappings.SimpleRegistry;
import com.github.retrooper.packetevents.util.mappings.SynchronizedRegistriesHandler.RegistryEntry;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerRegistryData.RegistryElement;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BiomeCodecTest extends BaseDummyAPITest {

    @Test
    public void preserveCustomGrassColorModifier() {
        PacketWrapper<?> wrapper = PacketWrapper.createDummyWrapper(ClientVersion.V_1_20);

        NBTCompound effectsTag = new NBTCompound();
        effectsTag.setTag("fog_color", new NBTInt(0xC0D8FF));
        effectsTag.setTag("water_color", new NBTInt(0x3F76E4));
        effectsTag.setTag("water_fog_color", new NBTInt(0x050533));
        effectsTag.setTag("sky_color", new NBTInt(0x78A7FF));
        effectsTag.setTag("grass_color_modifier", new NBTString("twilightforest:dark_forest"));

        NBTCompound biomeTag = new NBTCompound();
        biomeTag.setTag("temperature", new NBTFloat(0.7f));
        biomeTag.setTag("downfall", new NBTFloat(0.8f));
        biomeTag.setTag("has_precipitation", new NBTByte(true));
        biomeTag.setTag("effects", effectsTag);

        Biome biome = Biome.CODEC.decode(biomeTag, wrapper);

        assertEquals(BiomeEffects.GrassColorModifier.NONE, biome.getEffects().getGrassColorModifier());
        assertEquals("twilightforest:dark_forest", biome.getEffects().getGrassColorModifierName());
        assertEquals(biomeTag, Biome.CODEC.encode(wrapper, biome));
    }

    @Test
    public void fallbackForUnsupportedCustomBiomeData() {
        PacketWrapper<?> wrapper = PacketWrapper.createDummyWrapper(ClientVersion.V_1_20);
        ResourceLocation biomeName = new ResourceLocation("example:custom_biome");

        NBTCompound effectsTag = new NBTCompound();
        effectsTag.setTag("fog_color", new NBTInt(0xC0D8FF));
        effectsTag.setTag("water_color", new NBTInt(0x3F76E4));
        effectsTag.setTag("water_fog_color", new NBTInt(0x050533));
        effectsTag.setTag("sky_color", new NBTInt(0x78A7FF));

        NBTCompound biomeTag = new NBTCompound();
        biomeTag.setTag("temperature", new NBTFloat(0.7f));
        biomeTag.setTag("temperature_modifier", new NBTString("example:custom_modifier"));
        biomeTag.setTag("downfall", new NBTFloat(0.8f));
        biomeTag.setTag("has_precipitation", new NBTByte(true));
        biomeTag.setTag("effects", effectsTag);

        RegistryEntry<Biome> entry = new RegistryEntry<>(Biomes.getRegistry(), Biome.CODEC);
        SimpleRegistry<Biome> registry = entry.createFromElements(Collections.singletonList(
                new RegistryElement(biomeName, biomeTag)), wrapper);

        Biome fallback = registry.getById(ClientVersion.V_1_20, 0);
        assertNotNull(fallback);
        assertEquals(biomeName, fallback.getName());
        assertEquals(0, fallback.getId(ClientVersion.V_1_20));
    }
}
