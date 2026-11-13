package nyonio.mixin;

import appeng.fluids.helper.DualityFluidInterface;
import appeng.fluids.helper.IFluidInterfaceHost;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import nyonio.ae2.FluixPoolCardInventory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** AE2 8 has no fluid-interface upgrade inventory; provide one persistent card slot. */
@Mixin(value=DualityFluidInterface.class,remap=false)
public abstract class MixinFluidManaUpgrades {
    @Shadow @Final private IFluidInterfaceHost iHost;
    @Unique private ItemStackHandler botaniaApplie$upgrades;
    @Unique private ItemStackHandler botaniaApplie$inventory() {
        if(botaniaApplie$upgrades==null) botaniaApplie$upgrades=new FluixPoolCardInventory(iHost);
        return botaniaApplie$upgrades;
    }
    @Inject(method="getInventoryByName",at=@At("HEAD"),cancellable=true)
    private void botaniaApplie$upgrades(String name,CallbackInfoReturnable<IItemHandler> ci) { if("upgrades".equals(name))ci.setReturnValue(botaniaApplie$inventory()); }
    @Inject(method="writeToNBT",at=@At("TAIL"))
    private void botaniaApplie$write(CompoundNBT tag,CallbackInfo ci) { tag.put("BotaniaApplieUpgrades",botaniaApplie$inventory().serializeNBT()); }
    @Inject(method="readFromNBT",at=@At("TAIL"))
    private void botaniaApplie$read(CompoundNBT tag,CallbackInfo ci) { botaniaApplie$inventory().deserializeNBT(tag.getCompound("BotaniaApplieUpgrades")); }
}
