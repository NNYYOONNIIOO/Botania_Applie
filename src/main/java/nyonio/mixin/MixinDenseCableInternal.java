package nyonio.mixin;

import appeng.api.util.AEPartLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps wireless INTERNAL links out of dense-cable side geometry. */
@Mixin(targets = "appeng.parts.networking.PartDenseCable", remap = false)
public abstract class MixinDenseCableInternal {
    @Inject(method = "isDense", at = @At("HEAD"), cancellable = true)
    private void botaniaApplie$ignoreInternal(AEPartLocation location,
                                                CallbackInfoReturnable<Boolean> callback) {
        if (location == AEPartLocation.INTERNAL) {
            callback.setReturnValue(false);
        }
    }
}
