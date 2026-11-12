package nyonio.item;
import appeng.api.config.Upgrades;
import appeng.api.parts.*;
import appeng.api.util.AEPartLocation;
import net.minecraft.item.*;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.items.IItemHandler;
import nyonio.*;
public class ItemFluixPoolCard extends Item implements IFluixUpgradeModule {
    public static final String UPGRADE_ID="fluixpool_card";
    public ItemFluixPoolCard() { super(BotaniaApplie.properties()); }
    @Override public String getUpgradeTypeId() { return UPGRADE_ID; }
    @Override public int getMaxInstalled() { return 1; }
    @Override public Upgrades getType(ItemStack stack) { return Upgrades.CRAFTING; }
    @Override public ActionResultType onItemUseFirst(ItemStack stack,ItemUseContext ctx) {
        if(ctx.getPlayer()==null || !ctx.getPlayer().isSneaking())return ActionResultType.PASS;
        Object target=ctx.getWorld().getTileEntity(ctx.getPos());
        if(target instanceof IPartHost) { Vector3d local=ctx.getHitVec().subtract(ctx.getPos().getX(),ctx.getPos().getY(),ctx.getPos().getZ());IPart part=((IPartHost)target).selectPart(local).part;if(part!=null)target=part; }
        IItemHandler upgrades=FluixPoolManaHelper.getUpgradeInventory(target);
        if(upgrades==null)return ActionResultType.PASS;
        if(ctx.getWorld().isRemote)return ActionResultType.SUCCESS;
        for(int i=0;i<upgrades.getSlots();i++) {
            ItemStack one=stack.copy();one.setCount(1);
            if(upgrades.insertItem(i,one,false).isEmpty()) {stack.shrink(1);return ActionResultType.SUCCESS;}
        }
        return ActionResultType.PASS;
    }
    public static boolean isCard(ItemStack stack) { return !stack.isEmpty() && stack.getItem()==BotaniaApplie.fluixPoolCard; }
}
