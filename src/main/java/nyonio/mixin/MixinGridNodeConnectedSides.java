package nyonio.mixin;

import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.util.AEPartLocation;
import nyonio.channel.ChannelSparkNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;

/**
 * Keeps wireless bridge directions out of AE2 cable geometry. The underlying
 * connection remains on the grid node, so pathing and channel accounting are
 * unchanged.
 */
@Mixin(targets = "appeng.me.GridNode", remap = false)
public abstract class MixinGridNodeConnectedSides {
    @Inject(method = "getConnectedSides", at = @At("RETURN"), cancellable = true)
    private void botaniaApplie$hideWirelessDirections(
            CallbackInfoReturnable<EnumSet<AEPartLocation>> callback) {
        EnumSet<AEPartLocation> sides = callback.getReturnValue();
        if (sides == null) {
            return;
        }

        try {
            IGridNode node = (IGridNode) (Object) this;
            for (IGridConnection connection : node.getConnections()) {
                if (!ChannelSparkNetwork.isWirelessConnection(connection)) {
                    continue;
                }
                AEPartLocation direction = connection.getDirection(node);
                if (direction != null) {
                    sides.remove(direction);
                }
            }
            callback.setReturnValue(sides);
        } catch (Throwable ignored) {
            // AE2 can rebuild a grid while the cable is being queried.
        }
    }
}
