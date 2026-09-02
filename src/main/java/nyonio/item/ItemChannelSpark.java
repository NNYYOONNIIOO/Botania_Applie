package nyonio.item;

import appeng.api.networking.IGridNode;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import nyonio.channel.ChannelSparkNetwork;
import nyonio.entity.EntityChannelSpark;

public class ItemChannelSpark extends Item {
    public ItemChannelSpark() {
        setMaxStackSize(64);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        IGridNode target = ChannelSparkNetwork.findGridNode(world, pos);
        BlockPos spawnPos = pos.offset(facing);
        EntityChannelSpark spark = new EntityChannelSpark(world,
                spawnPos.getX() + 0.5D, spawnPos.getY() + 0.5D, spawnPos.getZ() + 0.5D);
        if (target != null) {
            spark.setTarget(pos);
        }
        world.spawnEntity(spark);
        consume(player, hand);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        RayTraceResult hit = player.rayTrace(5.0D, 1.0F);
        EntityChannelSpark spark;
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            IGridNode target = ChannelSparkNetwork.findGridNode(world, hit.getBlockPos());
            Vec3d location = hit.hitVec == null
                    ? new Vec3d(hit.getBlockPos()).addVector(0.5D, 0.5D, 0.5D)
                    : hit.hitVec.addVector(hit.sideHit.getFrontOffsetX() * 0.1D,
                    hit.sideHit.getFrontOffsetY() * 0.1D, hit.sideHit.getFrontOffsetZ() * 0.1D);
            spark = new EntityChannelSpark(world, location.x, location.y, location.z);
            if (target != null) {
                spark.setTarget(hit.getBlockPos());
            }
        } else {
            Vec3d eyes = player.getPositionEyes(1.0F);
            Vec3d look = player.getLookVec();
            Vec3d location = eyes.addVector(look.x * 3.0D, look.y * 3.0D, look.z * 3.0D);
            spark = new EntityChannelSpark(world, location.x, location.y, location.z);
        }

        world.spawnEntity(spark);
        consume(player, hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    private static void consume(EntityPlayer player, EnumHand hand) {
        if (!player.capabilities.isCreativeMode) {
            player.getHeldItem(hand).shrink(1);
        }
    }
}
