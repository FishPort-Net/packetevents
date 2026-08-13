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

package io.github.retrooper.packetevents.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.item.type.StaticItemType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.util.mappings.IRegistry;
import com.github.retrooper.packetevents.util.mappings.IRegistryHolder;
import com.github.retrooper.packetevents.util.mappings.SimpleRegistry;
import com.github.retrooper.packetevents.util.mappings.SimpleTypesBuilderData;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

/**
 * Supplies the live server item IDs used by hybrid Bukkit/modded servers. Item
 * registries were not synchronized over the protocol before 1.20.5, so the
 * normal versioned mappings cannot know IDs allocated by Forge at runtime.
 */
@ApiStatus.Internal
public final class SpigotItemRegistry {

    private static final ResourceLocation ITEM_REGISTRY_KEY = ItemTypes.getRegistry().getRegistryKey();
    private static volatile @Nullable IRegistry<ItemType> runtimeRegistry;
    private static final IRegistryHolder RUNTIME_REGISTRY_HOLDER = (registryKey, version) ->
            ITEM_REGISTRY_KEY.equals(registryKey) ? runtimeRegistry : null;

    private SpigotItemRegistry() {
    }

    public static void init() {
        ServerVersion serverVersion = SpigotReflectionUtil.VERSION;
        if (serverVersion.isOlderThan(ServerVersion.V_1_13_2)
                || serverVersion.isNewerThan(ServerVersion.V_1_20_4)) {
            return;
        }

        ClientVersion clientVersion = serverVersion.toClientVersion();
        SimpleRegistry<ItemType> registry = new SimpleRegistry<>(ITEM_REGISTRY_KEY);
        int customItems = 0;
        int skippedItems = 0;

        for (Material material : Material.values()) {
            if (material.name().startsWith("LEGACY_") || !material.isItem()) {
                continue;
            }
            try {
                ResourceLocation name = new ResourceLocation(material.getKey().toString());
                Object nmsStack = SpigotReflectionUtil.toNMSItemStack(new ItemStack(material));
                int id = SpigotReflectionUtil.getNMSItemStackId(nmsStack);
                ItemType baseType = ItemTypes.getRegistry().getByName(clientVersion, name);
                if (baseType == null) {
                    customItems++;
                }

                ItemType existing = registry.getById(clientVersion, id);
                if (existing != null) {
                    registry.define(name, id, existing);
                    continue;
                }
                registry.define(name, id, createRuntimeType(name, id, material, baseType));
            } catch (RuntimeException exception) {
                skippedItems++;
                PacketEvents.getAPI().getLogManager().debug("Could not map Bukkit material "
                        + material + " into the live item registry: " + exception.getMessage());
            }
        }

        // Leave normal Spigot/Paper behavior completely unchanged.
        if (customItems == 0) {
            return;
        }

        runtimeRegistry = registry;
        PacketEvents.getAPI().getLogManager().info("Loaded " + registry.size()
                + " live item mappings, including " + customItems + " modded items"
                + (skippedItems == 0 ? "." : " (skipped " + skippedItems + ")."));
    }

    public static void applyTo(User user) {
        IRegistry<ItemType> registry = runtimeRegistry;
        if (registry != null) {
            user.putRegistry(registry);
        }
    }

    public static void configureItemStackWrapper(
            PacketWrapper<?> wrapper, ItemStack bukkitStack, Object nmsStack
    ) {
        ServerVersion serverVersion = wrapper.getServerVersion();
        if (serverVersion.isOlderThan(ServerVersion.V_1_13_2)
                || serverVersion.isNewerThan(ServerVersion.V_1_20_4)
                || bukkitStack.getType() == Material.AIR || bukkitStack.getAmount() <= 0) {
            return;
        }

        ClientVersion version = serverVersion.toClientVersion();
        int id = SpigotReflectionUtil.getNMSItemStackId(nmsStack);
        IRegistry<ItemType> registry = runtimeRegistry;
        if (registry != null && registry.getById(version, id) != null) {
            wrapper.setRegistryHolder(RUNTIME_REGISTRY_HOLDER);
            return;
        }

        ResourceLocation name = new ResourceLocation(bukkitStack.getType().getKey().toString());
        ItemType staticType = ItemTypes.getRegistry().getById(version, id);
        if (staticType != null && staticType.getName().equals(name)) {
            return;
        }

        ItemType baseType = ItemTypes.getRegistry().getByName(version, name);
        SimpleRegistry<ItemType> localRegistry = new SimpleRegistry<>(ITEM_REGISTRY_KEY);
        localRegistry.define(name, id, createRuntimeType(name, id, bukkitStack.getType(), baseType));
        wrapper.setRegistryHolder(holderFor(localRegistry));
    }

    private static IRegistryHolder holderFor(IRegistry<ItemType> registry) {
        return (registryKey, version) -> ITEM_REGISTRY_KEY.equals(registryKey) ? registry : null;
    }

    private static ItemType createRuntimeType(
            ResourceLocation name, int id, Material material, @Nullable ItemType baseType
    ) {
        if (baseType != null && baseType.getId(SpigotReflectionUtil.VERSION.toClientVersion()) == id) {
            return baseType;
        }
        return new StaticItemType(
                new SimpleTypesBuilderData(name, id),
                baseType != null ? baseType.getMaxAmount() : material.getMaxStackSize(),
                baseType != null ? baseType.getMaxDurability() : material.getMaxDurability(),
                baseType != null ? baseType.getCraftRemainder() : ItemTypes.AIR,
                baseType != null ? baseType.getPlacedType() : null,
                baseType != null ? baseType.getAttributes() : Collections.emptySet()
        );
    }
}
