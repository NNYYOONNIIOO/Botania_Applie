package nyonio.integration.ae2fluidcraft;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import com.glodblock.github.common.item.fake.FakeItemHandler;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import net.minecraft.item.ItemStack;
import nyonio.BotaniaApplie;
import nyonio.ae2.ManaStack;
import nyonio.item.ItemManaPacket;

import javax.annotation.Nullable;

public class AE2FluidCraftIntegration {

    public static void init() {
        try {
            FakeItemRegister.registerHandler(
                ItemManaPacket.class,
                new FakeItemHandler<Long, ManaStack>() {
                    
                    @Override
                    public Long getStack(@Nullable ItemStack stack) {
                        if (stack == null || stack.isEmpty() || !ItemManaPacket.isManaPacket(stack)) {
                            return null;
                        }
                        return ItemManaPacket.getMana(stack);
                    }

                    @Override
                    public Long getStack(@Nullable IAEItemStack stack) {
                        if (stack == null) {
                            return null;
                        }
                        return getStack(stack.createItemStack());
                    }

                    @Override
                    public ManaStack getAEStack(@Nullable ItemStack stack) {
                        Long mana = getStack(stack);
                        if (mana == null || mana <= 0) {
                            return null;
                        }
                        return new ManaStack(mana);
                    }

                    @Override
                    public ManaStack getAEStack(@Nullable IAEItemStack stack) {
                        if (stack == null) {
                            return null;
                        }
                        Long mana = getStack(stack);
                        if (mana == null || mana <= 0) {
                            return null;
                        }
                        ManaStack manaStack = new ManaStack(mana);
                        manaStack.setStackSize(stack.getStackSize());
                        return manaStack;
                    }

                    @Override
                    public ItemStack packStack(@Nullable Long mana) {
                        if (mana == null || mana <= 0) {
                            return ItemStack.EMPTY;
                        }
                        return ItemManaPacket.create(mana);
                    }

                    @Override
                    public ItemStack displayStack(@Nullable Long mana) {
                        if (mana == null || mana <= 0) {
                            return ItemStack.EMPTY;
                        }
                        ItemStack stack = ItemManaPacket.create(mana);
                        if (stack.hasTagCompound()) {
                            stack.getTagCompound().setBoolean("DisplayOnly", true);
                        }
                        return stack;
                    }

                    @Override
                    public IAEItemStack packAEStack(@Nullable Long mana) {
                        if (mana == null || mana <= 0) {
                            return null;
                        }
                        ItemStack stack = packStack(mana);
                        IAEItemStack aeStack = AEItemStack.fromItemStack(stack);
                        if (aeStack != null) {
                            aeStack.setStackSize(mana);
                        }
                        return aeStack;
                    }

                    @Override
                    public IAEItemStack packAEStackLong(@Nullable ManaStack manaStack) {
                        if (manaStack == null || manaStack.getStackSize() <= 0) {
                            return null;
                        }
                        return packAEStack(manaStack.getStackSize());
                    }
                }
            );

            BotaniaApplie.getLogger().info("Successfully registered ManaPacket with AE2FluidCraft FakeItemRegister system!");
            
        } catch (Exception e) {
            BotaniaApplie.getLogger().error("Failed to register ManaPacket with AE2FluidCraft", e);
        }
    }
}