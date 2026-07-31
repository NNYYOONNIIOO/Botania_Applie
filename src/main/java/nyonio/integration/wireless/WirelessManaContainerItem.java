package nyonio.integration.wireless;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.FMLCommonHandler;
import vazkii.botania.api.mana.IManaItem;

import java.util.UUID;

/**
 * Ephemeral Botania mana source backed by a player's wireless terminal network.
 */
public final class WirelessManaContainerItem extends Item implements IManaItem {

    private static final String PLAYER_UUID = "player_uuid";
    private static final ThreadLocal<EntityPlayer> CURRENT_PLAYER = new ThreadLocal<>();

    public static final WirelessManaContainerItem INSTANCE = new WirelessManaContainerItem();

    private WirelessManaContainerItem() {
        setMaxStackSize(1);
    }

    public static ItemStack create(EntityPlayer player) {
        CURRENT_PLAYER.set(player);
        ItemStack stack = new ItemStack(INSTANCE);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setUniqueId(PLAYER_UUID, player.getUniqueID());
        stack.setTagCompound(tag);
        return stack;
    }

    @Override
    public int getMana(ItemStack stack) {
        EntityPlayer player = resolvePlayer(stack);
        long available = WirelessManaAccess.getAvailableMana(player);
        return (int) Math.min(Integer.MAX_VALUE, available);
    }

    @Override
    public int getMaxMana(ItemStack stack) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void addMana(ItemStack stack, int mana) {
        if (mana >= 0) {
            return;
        }

        EntityPlayer player = resolvePlayer(stack);
        WirelessManaAccess.extractMana(player, (int) Math.min(Integer.MAX_VALUE, -(long) mana));
    }

    @Override
    public boolean canReceiveManaFromPool(ItemStack stack, TileEntity pool) {
        return false;
    }

    @Override
    public boolean canReceiveManaFromItem(ItemStack stack, ItemStack otherStack) {
        return false;
    }

    @Override
    public boolean canExportManaToPool(ItemStack stack, TileEntity pool) {
        return false;
    }

    @Override
    public boolean canExportManaToItem(ItemStack stack, ItemStack otherStack) {
        return !otherStack.isEmpty();
    }

    @Override
    public boolean isNoExport(ItemStack stack) {
        return false;
    }

    private static EntityPlayer resolvePlayer(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        UUID uuid = tag != null && tag.hasUniqueId(PLAYER_UUID) ? tag.getUniqueId(PLAYER_UUID) : null;
        EntityPlayer current = CURRENT_PLAYER.get();
        if (current != null && (uuid == null || uuid.equals(current.getUniqueID()))) {
            return current;
        }

        if (uuid == null) {
            return null;
        }

        try {
            if (FMLCommonHandler.instance().getMinecraftServerInstance() != null) {
                return FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayerByUUID(uuid);
            }
        } catch (Throwable ignored) {
            // Client-side calls are covered by the current-player context.
        }
        return null;
    }
}
