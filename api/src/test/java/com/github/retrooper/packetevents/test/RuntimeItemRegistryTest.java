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

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.item.type.StaticItemType;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.data.ParticleItemStackData;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.test.base.BaseDummyAPITest;
import com.github.retrooper.packetevents.util.mappings.IRegistryHolder;
import com.github.retrooper.packetevents.util.mappings.SimpleRegistry;
import com.github.retrooper.packetevents.util.mappings.SimpleTypesBuilderData;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.github.retrooper.packetevents.util.SpigotItemRegistry;
import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RuntimeItemRegistryTest extends BaseDummyAPITest {

    @Test
    public void itemParticleNbtUsesWrapperRegistry() {
        int runtimeId = 9539;
        ResourceLocation name = new ResourceLocation("kaleidoscope_tavern:test_item");
        SimpleRegistry<ItemType> registry = createRegistry(name, runtimeId);
        IRegistryHolder holder = holderFor(registry);

        NBTCompound itemData = new NBTCompound();
        itemData.setTag("ketting_data", new NBTString("preserved"));
        NBTCompound item = new NBTCompound();
        item.setTag("id", new NBTString(name.toString()));
        item.setTag("Count", new NBTInt(7));
        item.setTag("tag", itemData);
        NBTCompound particleNbt = new NBTCompound();
        particleNbt.setTag("type", new NBTString("minecraft:item"));
        particleNbt.setTag("value", item);

        PacketWrapper<?> wrapper = PacketWrapper.createDummyWrapper(ClientVersion.V_1_20);
        wrapper.setRegistryHolder(holder);
        Particle<?> particle = Particle.CODEC.decode(particleNbt, wrapper);
        ItemStack decoded = ((ParticleItemStackData) particle.getData()).getItemStack();

        assertEquals(name, decoded.getType().getName());
        assertEquals(runtimeId, decoded.getType().getId(ClientVersion.V_1_20));
        assertEquals(7, decoded.getAmount());
        assertEquals(itemData, decoded.getNBT());
        assertEquals(particleNbt, Particle.CODEC.encode(wrapper, particle));
    }

    @Test
    public void legacyItemStackUsesWrapperRegistryAndPreservesData() {
        int runtimeId = 9539;
        ResourceLocation name = new ResourceLocation("kaleidoscope_tavern:test_item");
        SimpleRegistry<ItemType> registry = createRegistry(name, runtimeId);
        IRegistryHolder holder = holderFor(registry);

        NBTCompound nbt = new NBTCompound();
        nbt.setTag("ketting_data", new NBTString("preserved"));

        Object input = PooledByteBufAllocator.DEFAULT.buffer();
        Object output = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            PacketWrapper<?> inputWriter = PacketWrapper.createUniversalPacketWrapper(input, ServerVersion.V_1_20_1);
            inputWriter.writeBoolean(true);
            inputWriter.writeVarInt(runtimeId);
            inputWriter.writeByte(7);
            inputWriter.writeNBT(nbt);

            assertEquals(runtimeId, SpigotItemRegistry.peekItemStackId(inputWriter));
            assertEquals(0, ByteBufHelper.readerIndex(input));

            ByteBufHelper.readerIndex(input, 0);
            PacketWrapper<?> strictReader = PacketWrapper.createUniversalPacketWrapper(input, ServerVersion.V_1_20_1);
            assertThrows(RuntimeException.class, strictReader::readItemStack);

            ByteBufHelper.readerIndex(input, 0);
            PacketWrapper<?> runtimeReader = PacketWrapper.createUniversalPacketWrapper(input, ServerVersion.V_1_20_1);
            runtimeReader.setRegistryHolder(holder);
            ItemStack decoded = runtimeReader.readItemStack();

            assertEquals(name, decoded.getType().getName());
            assertEquals(runtimeId, decoded.getType().getId(ClientVersion.V_1_20));
            assertEquals(7, decoded.getAmount());
            assertEquals(nbt, decoded.getNBT());

            PacketWrapper<?> outputWriter = PacketWrapper.createUniversalPacketWrapper(output, ServerVersion.V_1_20_1);
            outputWriter.setRegistryHolder(holder);
            outputWriter.writeItemStack(decoded);

            ByteBufHelper.readerIndex(output, 0);
            PacketWrapper<?> outputReader = PacketWrapper.createUniversalPacketWrapper(output, ServerVersion.V_1_20_1);
            assertTrue(outputReader.readBoolean());
            assertEquals(runtimeId, outputReader.readVarInt());
            assertEquals(7, outputReader.readByte());
            assertEquals(nbt, outputReader.readNBT());
        } finally {
            ByteBufHelper.release(input);
            ByteBufHelper.release(output);
        }
    }

    private static SimpleRegistry<ItemType> createRegistry(ResourceLocation name, int runtimeId) {
        ItemType runtimeType = new StaticItemType(
                new SimpleTypesBuilderData(name, runtimeId),
                16, 123, ItemTypes.AIR, null, Collections.emptySet());
        SimpleRegistry<ItemType> registry = new SimpleRegistry<>(ItemTypes.getRegistry().getRegistryKey());
        registry.define(name, runtimeId, runtimeType);
        return registry;
    }

    private static IRegistryHolder holderFor(SimpleRegistry<ItemType> registry) {
        return (registryKey, version) -> registry.getRegistryKey().equals(registryKey) ? registry : null;
    }
}
