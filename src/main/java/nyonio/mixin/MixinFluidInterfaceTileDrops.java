package nyonio.mixin;

import appeng.fluids.helper.IFluidInterfaceHost;
import appeng.fluids.tile.FluidInterfaceTileEntity;
import appeng.tile.grid.AENetworkTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;

/** Fluid interfaces in AE2 8 do not have a native upgrade-drop implementation. */
@Mixin(value = FluidInterfaceTileEntity.class, remap = false)
public abstract class MixinFluidInterfaceTileDrops extends AENetworkTileEntity {
    protected MixinFluidInterfaceTileDrops(TileEntityType<?> type) { super(type); }

    @Override
    public void getDrops(World world, BlockPos pos, List<ItemStack> drops) {
        super.getDrops(world, pos, drops);
        IItemHandler cards = ((IFluidInterfaceHost) this).getInventoryByName("upgrades");
        if (cards != null) {
            for (int slot = 0; slot < cards.getSlots(); slot++) {
                ItemStack card = cards.getStackInSlot(slot);
                if (!card.isEmpty()) drops.add(card.copy());
            }
        }
    }
}
