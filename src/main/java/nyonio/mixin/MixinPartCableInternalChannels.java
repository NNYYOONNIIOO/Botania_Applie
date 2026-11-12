package nyonio.mixin;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import nyonio.channel.ChannelSparkNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Includes wireless bridge usage in the channel number shown by smart cables. */
@Mixin(value = appeng.parts.networking.CablePart.class, remap = false)
public abstract class MixinPartCableInternalChannels {
    @ModifyArg(
            method = "writeToStream",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/PacketBuffer;writeByte(I)Lio/netty/buffer/ByteBuf;",
                    ordinal = 1),
            index = 0,
            require = 1)
    private int botaniaApplie$includeInternalChannelsInStream(int channels) {
        return Math.max(channels, botaniaApplie$getWirelessChannels());
    }

    private int botaniaApplie$getWirelessChannels() {
        try {
            IGridNode node = ((IPart) (Object) this).getGridNode();
            if (node == null) {
                return 0;
            }

            int channels = 0;
            for (IGridConnection connection : node.getConnections()) {
                if (connection == null
                        || !ChannelSparkNetwork.isWirelessConnection(connection)) {
                    continue;
                }
                channels = Math.max(channels, connection.getUsedChannels());
            }
            return channels;
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
