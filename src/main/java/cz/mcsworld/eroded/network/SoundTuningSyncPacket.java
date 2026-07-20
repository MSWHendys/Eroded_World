package cz.mcsworld.eroded.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public record SoundTuningSyncPacket(Float volumeMul, Float delayMul)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<@NotNull SoundTuningSyncPacket> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("eroded", "sound_tuning"));

    public static final StreamCodec<@NotNull RegistryFriendlyByteBuf, @NotNull SoundTuningSyncPacket> CODEC =
            StreamCodec.ofMember(
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
    public CustomPacketPayload.@NotNull Type<? extends @NotNull CustomPacketPayload> type() {
        return ID;
    }

    public static void sendTo(ServerPlayer player, Float volume, Float delay) {
        ServerPlayNetworking.send(player,
                new SoundTuningSyncPacket(volume, delay));
    }
}