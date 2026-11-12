package nyonio.handler;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.cells.ICellInventory;
import appeng.api.storage.cells.ICellInventoryHandler;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.me.helpers.MachineSource;
import appeng.me.storage.MEInventoryHandler;
import appeng.tile.storage.DriveTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import nyonio.ae2.ManaStack;
import nyonio.ae2.ManaStorageChannel;
import nyonio.item.ItemManaCard;
import nyonio.item.ItemManaStorageCell;

import java.util.List;

public class ManaCardTickHandler {

    private int tickCounter = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        if (tickCounter % 20 != 0) return;

        for (net.minecraft.world.server.ServerWorld world : net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getWorlds()) {
            for (TileEntity te : world.loadedTileEntityList) {
                if (te instanceof DriveTileEntity) {
                    processDrive((DriveTileEntity) te);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processDrive(DriveTileEntity drive) {
        try {
            if (!drive.getProxy().isActive()) return;

            List<IMEInventoryHandler> handlers = drive.getCellArray(ManaStorageChannel.INSTANCE);
            if (handlers == null || handlers.isEmpty()) return;

            IActionSource source = new MachineSource(drive);

            for (IMEInventoryHandler handler : handlers) {
                if (!(handler instanceof MEInventoryHandler)) continue;

                IMEInventory internal = ((MEInventoryHandler) handler).getInternal();
                if (!(internal instanceof ICellInventoryHandler)) continue;

                ICellInventoryHandler<ManaStack> cellHandler = (ICellInventoryHandler<ManaStack>) internal;
                ICellInventory<ManaStack> cellInv = cellHandler.getCellInv();
                if (cellInv == null) continue;

                ItemStack cellStack = cellInv.getItemStack();
                if (cellStack.isEmpty() || !(cellStack.getItem() instanceof ItemManaStorageCell)) continue;

                int manaRate = ItemManaCard.getTotalManaRate(cellStack);
                if (manaRate <= 0) continue;

                ManaStack toInject = new ManaStack(manaRate);
                ((IMEInventoryHandler<ManaStack>) handler).injectItems(toInject, Actionable.MODULATE, source);
            }
        } catch (Exception e) {
        }
    }
}
