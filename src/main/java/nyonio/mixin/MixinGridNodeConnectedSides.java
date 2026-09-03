package nyonio.mixin;

import appeng.api.util.AEPartLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;

/**
 * INTERNAL AE connections carry a wireless path but have no physical cable
 * side. Keep them in GridNode.getConnections() for pathing/channel usage, but
 * do not expose them to cable geometry and collision-box code.
 */
@Mixin(targets = "appeng.me.GridNode", remap = false)
public abstract class MixinGridNodeConnectedSides {
    @Inject(method = "getConnectedSides", at = @At("RETURN"), cancellable = true)
    private void botaniaApplie$hideInternalSide(
            CallbackInfoReturnable<EnumSet<AEPartLocation>> callback) {
        EnumSet<AEPartLocation> safeSides = EnumSet.noneOf(AEPartLocation.class);
        EnumSet<AEPartLocation> returned = callback.getReturnValue();
        if (returned != null) {
            safeSides.addAll(returned);
        }
        safeSides.remove(AEPartLocation.INTERNAL);
        callback.setReturnValue(safeSides);
    }
}
