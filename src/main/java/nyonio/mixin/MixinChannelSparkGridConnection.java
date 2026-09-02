package nyonio.mixin;

import appeng.api.networking.IGridConnection;
import nyonio.channel.ChannelSparkNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "appeng.me.GridConnection", remap = false)
public abstract class MixinChannelSparkGridConnection {
    @Shadow
    private int lastUsedChannels;

    @Inject(method = "canSupportMoreChannels", at = @At("HEAD"), cancellable = true)
    private void botaniaApplie$limitChannelSparkCapacity(CallbackInfoReturnable<Boolean> callback) {
        IGridConnection connection = (IGridConnection) (Object) this;
        Integer capacity = ChannelSparkNetwork.getCapacity(connection);
        if (capacity != null) {
            callback.setReturnValue(lastUsedChannels < capacity);
        }
    }
}
