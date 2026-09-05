package nyonio.mixin;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import net.minecraft.nbt.NBTTagCompound;
import nyonio.channel.ChannelSparkNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Includes wireless bridge usage in the channel number shown by smart cables. */
@Mixin(targets = "appeng.parts.networking.PartCable", remap = false)
public abstract class MixinPartCableInternalChannels {
    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void botaniaApplie$includeInternalChannels(
            NBTTagCompound data, CallbackInfo callback) {
        try {
            int wireless = botaniaApplie$getWirelessChannels();
            if (wireless > data.getInteger("usedChannels")) {
                data.setInteger("usedChannels", wireless);
            }
        } catch (Throwable ignored) {
            // AE2 may rebuild the grid while TOP is probing a cable.
        }
    }

    @ModifyArg(
            method = "writeToStream",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/netty/buffer/ByteBuf;writeByte(I)Lio/netty/buffer/ByteBuf;",
                    ordinal = 1),
            index = 0,
            require = 0)
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
