package nyonio.integration.wireless;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import vazkii.botania.api.mana.IManaItem;

import java.util.UUID;

/**
 * Ephemeral Botania mana source backed by a player's wireless terminal network.
 */
public final class WirelessManaContainerItem extends Item implements IManaItem {

    private static final String PLAYER_UUID = "player_uuid";
    private static final ThreadLocal<PlayerEntity> CURRENT_PLAYER = new ThreadLocal<>();

    public static final WirelessManaContainerItem INSTANCE = new WirelessManaContainerItem();

    private WirelessManaContainerItem() {
        super(new Item.Properties().maxStackSize(1));
    }

    public static ItemStack create(PlayerEntity player) {
        CURRENT_PLAYER.set(player);
        ItemStack stack = new ItemStack(INSTANCE);
        CompoundNBT tag = new CompoundNBT();
        tag.putUniqueId(PLAYER_UUID, player.getUniqueID());
        stack.setTag(tag);
        return stack;
    }

    @Override
    public int getMana(ItemStack stack) {
        PlayerEntity player = resolvePlayer(stack);
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

        PlayerEntity player = resolvePlayer(stack);
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

    private static PlayerEntity resolvePlayer(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        UUID uuid = tag != null && tag.hasUniqueId(PLAYER_UUID) ? tag.getUniqueId(PLAYER_UUID) : null;
        PlayerEntity current = CURRENT_PLAYER.get();
        if (current != null && (uuid == null || uuid.equals(current.getUniqueID()))) {
            return current;
        }

        if (uuid == null) {
            return null;
        }

        try {
            if (ServerLifecycleHooks.getCurrentServer() != null) {
                return ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByUUID(uuid);
            }
        } catch (Throwable ignored) {
            // Client-side calls are covered by the current-player context.
        }
        return null;
    }
}
