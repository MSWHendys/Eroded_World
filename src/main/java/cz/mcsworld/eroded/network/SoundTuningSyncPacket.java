package cz.mcsworld.eroded.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public record SoundTuningSyncPacket(Float volumeMul, Float delayMul)
        implements CustomPayload {

    public static final CustomPayload.Id<SoundTuningSyncPacket> ID =
            new CustomPayload.Id<>(Identifier.of("eroded", "sound_tuning"));

    public static final PacketCodec<RegistryByteBuf, SoundTuningSyncPacket> CODEC =
            PacketCodec.of(
                    (packet, buf) -> {
                        buf.writeBoolean(packet.volumeMul() != null);
                        if (packet.volumeMul() != null) {
                            buf.writeFloat(packet.volumeMul());
                        }

                        buf.writeBoolean(packet.delayMul() != null);
                        if (packet.delayMul() != null) {
                            buf.writeFloat(packet.delayMul());
                        }
                    },
                    buf -> {
                        Float volume = null;
                        if (buf.readBoolean()) {
                            volume = buf.readFloat();
                        }

                        Float delay = null;
                        if (buf.readBoolean()) {
                            delay = buf.readFloat();
                        }

                        return new SoundTuningSyncPacket(volume, delay);
                    }
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void sendTo(ServerPlayerEntity player, Float volume, Float delay) {
        ServerPlayNetworking.send(player,
                new SoundTuningSyncPacket(volume, delay));
    }
}