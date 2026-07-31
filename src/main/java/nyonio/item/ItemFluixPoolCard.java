package nyonio.item;

import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.api.AEApi;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import nyonio.BotaniaApplie;
import nyonio.IFluixUpgradeModule;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ItemFluixPoolCard extends Item implements IFluixUpgradeModule {
    public static final String UPGRADE_ID = "fluixpool_card";

    public ItemFluixPoolCard() {
        setMaxStackSize(64);
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side,
                                            float hitX, float hitY, float hitZ, net.minecraft.util.EnumHand hand) {
        if (!player.isSneaking()) return EnumActionResult.PASS;
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos);
        if (world.isRemote) return EnumActionResult.SUCCESS;

        net.minecraftforge.items.IItemHandler upgrades;
        if (tile instanceof IPartHost) {
            Vec3d hit = new Vec3d(hitX, hitY, hitZ);
            IPart part = ((IPartHost) tile).selectPart(hit).part;
            if (part == null) return EnumActionResult.PASS;
            upgrades = nyonio.FluixPoolManaHelper.getUpgradeInventory(part);
        } else {
            upgrades = nyonio.FluixPoolManaHelper.getUpgradeInventory(tile);
        }
        if (upgrades == null) return EnumActionResult.PASS;

        for (int i = 0; i < upgrades.getSlots(); i++) {
            if (upgrades.getStackInSlot(i).isEmpty()) {
                ItemStack one = player.getHeldItem(hand).copy();
                one.setCount(1);
                ItemStack remainder = upgrades.insertItem(i, one, false);
                if (remainder.isEmpty()) {
                    player.getHeldItem(hand).shrink(1);
                    return EnumActionResult.SUCCESS;
                }
            }
        }
        return EnumActionResult.PASS;
    }

    @Override
    public String getUpgradeTypeId() {
        return UPGRADE_ID;
    }

    @Override
    public int getMaxInstalled() {
        return 1;
    }

    @Override
    public Upgrades getType(ItemStack stack) {
        return Upgrades.CRAFTING;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        Set<String> supported = new LinkedHashSet<>();
        addSupported(supported, AEApi.instance().definitions().blocks().iface().maybeStack(1).orElse(ItemStack.EMPTY));
        addSupported(supported, AEApi.instance().definitions().parts().iface().maybeStack(1).orElse(ItemStack.EMPTY));
        addSupported(supported, AEApi.instance().definitions().blocks().fluidIface().maybeStack(1).orElse(ItemStack.EMPTY));
        addSupported(supported, AEApi.instance().definitions().parts().fluidIface().maybeStack(1).orElse(ItemStack.EMPTY));

        if (Loader.isModLoaded("ae2fc")) {
            addExternalSupported(supported, "com.glodblock.github.loader.FCItems", "PART_DUAL_INTERFACE");
            addExternalSupported(supported, "com.glodblock.github.integration.mek.FCGasItems", "PART_TRIO_INTERFACE");
            addExternalSupported(supported, "com.glodblock.github.loader.FCBlocks", "DUAL_INTERFACE");
            addExternalSupported(supported, "com.glodblock.github.integration.mek.FCGasBlocks", "TRIO_INTERFACE");
        }
        if (Loader.isModLoaded("mekeng")) {
            addExternalSupported(supported, "com.mekeng.github.common.ItemAndBlocks", "GAS_INTERFACE_PART");
            addExternalSupported(supported, "com.mekeng.github.common.ItemAndBlocks", "GAS_INTERFACE");
        }

        if (!supported.isEmpty()) {
            tooltip.add("");
            tooltip.addAll(supported);
        }
    }

    private static void addSupported(Set<String> supported, ItemStack stack) {
        if (!stack.isEmpty()) supported.add(stack.getDisplayName());
    }

    private static void addExternalSupported(Set<String> supported, String className, String fieldName) {
        try {
            Field field = Class.forName(className).getField(fieldName);
            Object value = field.get(null);
            if (value instanceof Item) addSupported(supported, new ItemStack((Item) value));
        } catch (Exception ignored) {
        }
    }

    public static boolean isCard(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == BotaniaApplie.fluixPoolCard;
    }
}
