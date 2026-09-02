package nyonio.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import nyonio.channel.ChannelSparkNetwork;
import nyonio.entity.EntityChannelSpark;

public class ItemChannelSpark extends Item {
    public ItemChannelSpark() {
        setMaxStackSize(64);
        setUnlocalizedName("botania_applie.channel_spark");
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "item.botania_applie.channel_spark";
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                            float hitX, float hitY, float hitZ, EnumHand hand) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        return placeAt(world, player, hand, pos);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        return placeAt(world, player, hand, pos);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        RayTraceResult hit = player.rayTrace(5.0D, 1.0F);
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        EnumActionResult result = placeAt(world, player, hand, hit.getBlockPos());
        return new ActionResult<>(result, player.getHeldItem(hand));
    }

    private static EnumActionResult placeAt(World world, EntityPlayer player, EnumHand hand, BlockPos targetPos) {
        BlockPos sparkBlock = targetPos.up();
        if (ChannelSparkNetwork.hasSparkInBlock(world, sparkBlock)) {
            return EnumActionResult.FAIL;
        }

        EntityChannelSpark spark = new EntityChannelSpark(world,
                targetPos.getX() + 0.5D, targetPos.getY() + 1.5D, targetPos.getZ() + 0.5D);
        spark.setTarget(targetPos);
        return spawn(world, player, hand, spark);
    }

    private static EnumActionResult spawn(World world, EntityPlayer player, EnumHand hand, EntityChannelSpark spark) {
        if (!world.spawnEntity(spark)) {
            return EnumActionResult.FAIL;
        }
        consume(player, hand);
        return EnumActionResult.SUCCESS;
    }

    private static void consume(EntityPlayer player, EnumHand hand) {
        if (!player.capabilities.isCreativeMode) {
            player.getHeldItem(hand).shrink(1);
        }
    }
}
