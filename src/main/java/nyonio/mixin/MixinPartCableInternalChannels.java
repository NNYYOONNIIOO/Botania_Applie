package nyonio.mixin;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import appeng.api.util.AEPartLocation;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Includes wireless bridge usage in the channel number shown by smart cables. */
@Mixin(targets = "appeng.parts.networking.PartCable", remap = false)
public abstract class MixinPartCableInternalChannels {
    @Inject(method = "getChannelsOnSide", at = @At("RETURN"), cancellable = true)
    private void botaniaApplie$includeInternalChannels(
            EnumFacing side, CallbackInfoReturnable<Integer> callback) {
        if (side == null) {
            return;
        }

        int channels = callback.getReturnValueI();
        try {
            IGridNode node = ((IPart) (Object) this).getGridNode();
            if (node == null) {
                return;
            }
            for (IGridConnection connection : node.getConnections()) {
                if (connection == null || connection.getDirection(node) != AEPartLocation.INTERNAL) {
                    continue;
                }
                channels = Math.max(channels, connection.getUsedChannels());
            }
            callback.setReturnValue(channels);
        } catch (Throwable ignored) {
            // AE2 may rebuild the grid while the cable is being rendered.
        }
    }
}
