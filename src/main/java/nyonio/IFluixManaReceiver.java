package nyonio;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.util.math.AxisAlignedBB;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IManaCollector;
import vazkii.botania.api.mana.spark.ISparkAttachable;
import vazkii.botania.api.mana.spark.ISparkEntity;

import java.util.List;

public interface IFluixManaReceiver extends IManaPool, IManaCollector, ISparkAttachable {
    boolean hasFluixPoolCard();
    Object getFluixManaTarget();

    @Override
    default int getCurrentMana() {
        if (!hasFluixPoolCard()) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, FluixPoolManaHelper.getMana(getFluixManaTarget()));
    }

    @Override
    default int getMaxMana() {
        return hasFluixPoolCard() ? Integer.MAX_VALUE : 0;
    }

    @Override
    default boolean isFull() {
        return !hasFluixPoolCard() || FluixPoolManaHelper.getSpace(getFluixManaTarget()) <= 0;
    }

    @Override
    default void receiveMana(int amount) {
        if (!hasFluixPoolCard()) {
            return;
        }
        if (amount > 0) {
            FluixPoolManaHelper.insert(getFluixManaTarget(), amount);
        } else if (amount < 0) {
            FluixPoolManaHelper.extract(getFluixManaTarget(), (int) Math.min(Integer.MAX_VALUE, -(long) amount));
        }
    }

    @Override
    default void onClientDisplayTick() {
    }

    @Override
    default float getManaYieldMultiplier(vazkii.botania.api.internal.IManaBurst burst) {
        return 1F;
    }

    @Override
    default boolean canReceiveManaFromBursts() {
        return hasFluixPoolCard();
    }

    @Override
    default boolean isOutputtingPower() {
        return hasFluixPoolCard();
    }

    @Override
    default DyeColor getColor() {
        return DyeColor.WHITE;
    }

    @Override
    default void setColor(DyeColor color) {
    }

    @Override
    default boolean canAttachSpark(ItemStack stack) {
        return hasFluixPoolCard();
    }

    @Override
    default void attachSpark(ISparkEntity entity) {
    }

    @Override
    default ISparkEntity getAttachedSpark() {
        if (!hasFluixPoolCard()) return null;
        Object target = getFluixManaTarget();
        if (!(target instanceof TileEntity)) return null;
        TileEntity tile = (TileEntity) target;
        List<Entity> sparks = tile.getWorld().getEntitiesWithinAABB(Entity.class,
                new AxisAlignedBB(tile.getPos().up(), tile.getPos().up().add(1, 1, 1)));
        for (Entity entity : sparks) {
            if (entity instanceof ISparkEntity) return (ISparkEntity) entity;
        }
        return null;
    }

    @Override
    default int getAvailableSpaceForMana() {
        if (!hasFluixPoolCard()) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, FluixPoolManaHelper.getSpace(getFluixManaTarget()));
    }

    @Override
    default boolean areIncomingTranfersDone() {
        return !hasFluixPoolCard() || isFull();
    }
}
