package nyonio.handler;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import vazkii.botania.api.mana.IManaCollector;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.subtile.ISubTileContainer;
import vazkii.botania.api.subtile.SubTileEntity;
import vazkii.botania.api.subtile.SubTileGenerating;
import vazkii.botania.common.core.helper.Vector3;
import vazkii.botania.common.item.ItemObedienceStick;
import vazkii.botania.common.item.ItemTwigWand;
import nyonio.tile.TileFluixManaPool;

public class ObedienceStickHandler {

    private static final int LINK_RANGE = 6;

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isRemote) return;

        if (!(event.getItemStack().getItem() instanceof ItemObedienceStick)) return;

        World world = event.getWorld();
        BlockPos pos = event.getPos();
        TileEntity tile = world.getTileEntity(pos);

        if (!(tile instanceof TileFluixManaPool)) return;

        for (BlockPos pos_ : BlockPos.getAllInBox(pos.add(-LINK_RANGE, -LINK_RANGE, -LINK_RANGE), pos.add(LINK_RANGE, LINK_RANGE, LINK_RANGE))) {
            if (pos_.distanceSq(pos) > LINK_RANGE * LINK_RANGE) continue;

            TileEntity flowerTile = world.getTileEntity(pos_);
            if (flowerTile instanceof ISubTileContainer) {
                SubTileEntity subtile = ((ISubTileContainer) flowerTile).getSubTile();
                if (subtile instanceof SubTileGenerating) {
                    ((SubTileGenerating) subtile).linkToForcefully(tile);
                    Vector3 orig = new Vector3(pos_.getX() + 0.5, pos_.getY() + 0.5, pos_.getZ() + 0.5);
                    Vector3 end = new Vector3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    ItemTwigWand.doParticleBeam(world, orig, end);
                }
            }
        }
    }
}
