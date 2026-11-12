package nyonio.mixin;
import appeng.api.implementations.IUpgradeableHost;
import appeng.fluids.container.FluidConfigurableContainer;
import appeng.fluids.container.FluidInterfaceContainer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=FluidInterfaceContainer.class,remap=false)
public abstract class MixinFluidManaContainer extends FluidConfigurableContainer {
    protected MixinFluidManaContainer(ContainerType<?> type,int id,PlayerInventory inventory,IUpgradeableHost host) { super(type,id,inventory,host); }
    @Inject(method="availableUpgrades",at=@At("HEAD"),cancellable=true)
    private void botaniaApplie$slot(CallbackInfoReturnable<Integer> ci) { ci.setReturnValue(1); }
    @Inject(method="setupConfig",at=@At("TAIL"))
    private void botaniaApplie$setup(CallbackInfo ci) { setupUpgrades(); }
}
