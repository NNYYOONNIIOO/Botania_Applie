package nyonio.item;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import nyonio.BotaniaApplie;
import nyonio.channel.ChannelSparkNetwork;
import nyonio.entity.EntityChannelSpark;
public class ItemChannelSpark extends Item {
    public ItemChannelSpark() { super(BotaniaApplie.properties()); }
    @Override public ActionResultType onItemUseFirst(ItemStack stack,ItemUseContext context) { return placeAt(context.getWorld(),context.getPlayer(),context.getHand(),context.getPos()); }
    @Override public ActionResultType onItemUse(ItemUseContext context) { return placeAt(context.getWorld(),context.getPlayer(),context.getHand(),context.getPos()); }
    @Override public ActionResult<ItemStack> onItemRightClick(World world,PlayerEntity player,Hand hand) {
        if(world.isRemote) return ActionResult.resultSuccess(player.getHeldItem(hand));
        RayTraceResult hit=player.pick(5D,1F,false);
        ActionResultType result;
        if(hit.getType()==RayTraceResult.Type.BLOCK) result=placeAt(world,player,hand,((BlockRayTraceResult)hit).getPos());
        else {
            BlockPos pos=new BlockPos(player.getEyePosition(1F).add(player.getLookVec().scale(3D)));
            if(ChannelSparkNetwork.hasSparkInBlock(world,pos)) return ActionResult.resultFail(player.getHeldItem(hand));
            result=spawn(world,player,hand,createSpark(world,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5));
        }
        return new ActionResult<>(result,player.getHeldItem(hand));
    }
    protected EntityChannelSpark createSpark(World world,double x,double y,double z) { return new EntityChannelSpark(world,x,y,z); }
    private ActionResultType placeAt(World world,PlayerEntity player,Hand hand,BlockPos pos) {
        if(world.isRemote) return ActionResultType.SUCCESS;
        if(player==null || ChannelSparkNetwork.hasSparkInBlock(world,pos.up())) return ActionResultType.FAIL;
        EntityChannelSpark spark=createSpark(world,pos.getX()+0.5,pos.getY()+1.25,pos.getZ()+0.5);
        spark.setTarget(pos);return spawn(world,player,hand,spark);
    }
    private ActionResultType spawn(World world,PlayerEntity player,Hand hand,EntityChannelSpark spark) {
        if(!world.addEntity(spark)) return ActionResultType.FAIL;
        if(!player.abilities.isCreativeMode) player.getHeldItem(hand).shrink(1);
        return ActionResultType.SUCCESS;
    }
}
