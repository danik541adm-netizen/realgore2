package com.realgore;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record MaskPacket(int entityId, int mask) {

    public static void encode(MaskPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId());
        buf.writeVarInt(msg.mask());
    }

    public static MaskPacket decode(FriendlyByteBuf buf) {
        return new MaskPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(MaskPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> GoreClientState.set(msg.entityId(), msg.mask()));
        ctx.get().setPacketHandled(true);
    }
}
