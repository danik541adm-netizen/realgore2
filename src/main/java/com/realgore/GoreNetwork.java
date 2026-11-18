package com.realgore;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class GoreNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RealGore.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, MaskPacket.class, MaskPacket::encode, MaskPacket::decode,
                MaskPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** Отправить маску всем игрокам, которые видят сущность. */
    public static void syncTracking(Entity entity, int mask) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new MaskPacket(entity.getId(), mask));
    }

    /** Отправить маску одному игроку (когда он начал видеть сущность). */
    public static void syncTo(ServerPlayer player, Entity entity, int mask) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MaskPacket(entity.getId(), mask));
    }

    private GoreNetwork() {
    }
}
