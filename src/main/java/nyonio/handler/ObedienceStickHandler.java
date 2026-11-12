package nyonio.handler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import nyonio.tile.TileFluixManaPool;
import vazkii.botania.api.subtile.TileEntityGeneratingFlower;
import vazkii.botania.common.item.ItemObedienceStick;
import vazkii.botania.common.item.ItemTwigWand;
public class ObedienceStickHandler {
    @SubscribeEvent public void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if(e.getWorld().isRemote || !(e.getItemStack().getItem() instanceof ItemObedienceStick))return;
        TileEntity pool=e.getWorld().getTileEntity(e.getPos());
        if(!(pool instanceof TileFluixManaPool))return;
        for(BlockPos pos:BlockPos.getAllInBoxMutable(e.getPos().add(-6,-6,-6),e.getPos().add(6,6,6))) {
            if(pos.distanceSq(e.getPos())>36)continue;
            TileEntity flower=e.getWorld().getTileEntity(pos);
            if(flower instanceof TileEntityGeneratingFlower) {((TileEntityGeneratingFlower)flower).linkToForcefully(pool);ItemTwigWand.doParticleBeamWithOffset(e.getWorld(),pos,e.getPos());}
        }
    }
}
