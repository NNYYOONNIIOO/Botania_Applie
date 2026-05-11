package nyonio.tile;

import appeng.api.networking.*;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IStorageChannel;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.me.helpers.MachineSource;
import com.google.common.base.Predicates;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.internal.VanillaPacketDispatcher;
import vazkii.botania.api.item.IManaDissolvable;
import vazkii.botania.api.mana.IKeyLocked;
import vazkii.botania.api.mana.IManaItem;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.IThrottledPacket;
import vazkii.botania.api.mana.ManaNetworkEvent;
import vazkii.botania.api.mana.spark.ISparkAttachable;
import vazkii.botania.api.mana.spark.ISparkEntity;
import vazkii.botania.api.recipe.RecipeManaInfusion;
import vazkii.botania.api.state.BotaniaStateProps;
import vazkii.botania.api.state.enums.PoolVariant;
import vazkii.botania.client.core.handler.HUDHandler;
import vazkii.botania.client.core.helper.RenderHelper;
import vazkii.botania.common.Botania;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.tile.TileMod;
import vazkii.botania.common.core.handler.ConfigHandler;
import vazkii.botania.common.core.handler.ManaNetworkHandler;
import vazkii.botania.common.core.handler.ModSounds;
import vazkii.botania.common.core.helper.Vector3;
import vazkii.botania.common.item.ItemManaTablet;
import vazkii.botania.common.item.ModItems;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class TileFluixManaPool extends TileMod implements IManaPool, IKeyLocked, ISparkAttachable, IThrottledPacket, net.minecraft.util.ITickable, IGridProxyable, IActionHost {
    public static final int MAX_MANA_FLUIX = Integer.MAX_VALUE;
    public static final Color PARTICLE_COLOR = new Color(0x00C6FF);
    
    private static final String TAG_MANA = "mana";
    private static final String TAG_KNOWN_MANA = "knownMana";
    private static final String TAG_OUTPUTTING = "outputting";
    private static final String TAG_COLOR = "color";
    private static final String TAG_MANA_CAP = "manaCap";
    private static final String TAG_INPUT_KEY = "inputKey";
    private static final String TAG_OUTPUT_KEY = "outputKey";
    private static final String TAG_NETWORK_CONNECTED = "networkConnected";
    private static final int CRAFT_EFFECT_EVENT = 0;
    private static final int CHARGE_EFFECT_EVENT = 1;
    
    private boolean outputting = false;
    public EnumDyeColor color = EnumDyeColor.WHITE;
    public int mana;
    private int knownMana = -1;
    public int manaCap = -1;
    private int ticks = 0;
    private int soundTicks = 0;
    boolean isDoingTransfer = false;
    int ticksDoingTransfer = 0;
    private boolean sendPacket = false;
    private String inputKey = "";
    private String outputKey = "";
    
    private AENetworkProxy gridProxy;
    private boolean firstTick = true;
    private boolean isNetworkConnected = false;
    private int lastNetworkCheckTick = 0;
    private MachineSource actionSource;
    
    @Override
    public AENetworkProxy getProxy() {
        if (this.gridProxy == null) {
            this.gridProxy = new AENetworkProxy(this, "proxy", new ItemStack(nyonio.BotaniaApplie.fluixManaPool), true);
            this.gridProxy.setValidSides(EnumSet.allOf(net.minecraft.util.EnumFacing.class));
            this.gridProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            this.actionSource = new MachineSource(this);
        }
        return this.gridProxy;
    }
    
    @Override
    public void validate() {
        super.validate();
        this.getProxy().validate();
    }
    
    private void onReady() {
        this.getProxy().onReady();
    }
    
    @Override
    public void update() {
        if (!world.isRemote && firstTick) {
            firstTick = false;
            onReady();
        }
        
        if(manaCap == -1) {
            manaCap = MAX_MANA_FLUIX;
        }
        
        if(!world.isRemote && ticks - lastNetworkCheckTick >= 20) {
            lastNetworkCheckTick = ticks;
            boolean wasConnected = isNetworkConnected;
            isNetworkConnected = checkNetworkConnection();
            
            if(wasConnected != isNetworkConnected) {
                if(isNetworkConnected && mana > 0) {
                    transferLocalManaToNetwork(mana);
                    mana = 0;
                } else if(!isNetworkConnected) {
                    mana = 0;
                }
                VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
                markDispatchable();
            } else if(ticks % 60 == 0) {
                VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
            }
            
            if(isNetworkConnected) {
                long networkMana = getManaFromNetwork();
                int newMana = (int) Math.min(networkMana, Integer.MAX_VALUE);
                if(mana != newMana) {
                    mana = newMana;
                    VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
                }
            }
        }
        
        if(!ManaNetworkHandler.instance.isPoolIn(this) && !isInvalid()) {
            ManaNetworkEvent.addPool(this);
        }
        
        if(world.isRemote) {
            double particleChance = 1F - (double) getCurrentMana() / (double) manaCap * 0.1;
            if(Math.random() > particleChance) {
                Botania.proxy.wispFX(pos.getX() + 0.3 + Math.random() * 0.5, pos.getY() + 0.6 + Math.random() * 0.25, pos.getZ() + Math.random(), PARTICLE_COLOR.getRed() / 255F, PARTICLE_COLOR.getGreen() / 255F, PARTICLE_COLOR.getBlue() / 255F, (float) Math.random() / 3F, (float) -Math.random() / 25F, 2F);
            }
            return;
        }
        
        boolean wasDoingTransfer = isDoingTransfer;
        isDoingTransfer = false;
        
        if(soundTicks > 0) {
            soundTicks--;
        }
        
        if(sendPacket && ticks % 10 == 0) {
            VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
            sendPacket = false;
        }
        
        List<EntityItem> items = world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(pos, pos.add(1, 1, 1)));
        for(EntityItem item : items) {
            if(item.isDead) {
                continue;
            }
            
            ItemStack stack = item.getItem();
            if(!stack.isEmpty() && stack.getItem() instanceof IManaItem) {
                IManaItem manaItem = (IManaItem) stack.getItem();
                if(outputting && manaItem.canReceiveManaFromPool(stack, this) || !outputting && manaItem.canExportManaToPool(stack, this)) {
                    boolean didSomething = false;
                    
                    int transfRate = 1000;
                    
                    if(outputting) {
                        if(getCurrentMana() > 0 && manaItem.getMana(stack) < manaItem.getMaxMana(stack)) {
                            didSomething = true;
                            int manaVal = Math.min(transfRate, Math.min(getCurrentMana(), manaItem.getMaxMana(stack) - manaItem.getMana(stack)));
                            manaItem.addMana(stack, manaVal);
                            recieveMana(-manaVal);
                        }
                    } else {
                        if(manaItem.getMana(stack) > 0 && !isFull()) {
                            didSomething = true;
                            int manaVal = Math.min(transfRate, Math.min(manaCap - getCurrentMana(), manaItem.getMana(stack)));
                            manaItem.addMana(stack, -manaVal);
                            recieveMana(manaVal);
                        }
                    }
                    
                    if(didSomething) {
                        if(ConfigHandler.chargingAnimationEnabled && world.rand.nextInt(20) == 0) {
                            world.addBlockEvent(getPos(), getBlockType(), CHARGE_EFFECT_EVENT, outputting ? 1 : 0);
                        }
                        isDoingTransfer = outputting;
                    }
                }
            }
        }
        
        if(isDoingTransfer) {
            ticksDoingTransfer++;
        } else {
            ticksDoingTransfer = 0;
            if(wasDoingTransfer) {
                VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
            }
        }
        
        ticks++;
    }
    
    @Override
    public boolean receiveClientEvent(int event, int param) {
        switch(event) {
            case CRAFT_EFFECT_EVENT: {
                if(world.isRemote) {
                    for(int i = 0; i < 25; i++) {
                        float red = (float) Math.random();
                        float green = (float) Math.random();
                        float blue = (float) Math.random();
                        Botania.proxy.sparkleFX(pos.getX() + 0.5 + Math.random() * 0.4 - 0.2, pos.getY() + 0.75, pos.getZ() + 0.5 + Math.random() * 0.4 - 0.2,
                                    red, green, blue, (float) Math.random(), 10);
                    }
                }
                return true;
            }
            case CHARGE_EFFECT_EVENT: {
                if(world.isRemote) {
                    if(ConfigHandler.chargingAnimationEnabled) {
                        boolean outputting = param == 1;
                        Vector3 itemVec = Vector3.fromBlockPos(pos).add(0.5, 0.5 + Math.random() * 0.3, 0.5);
                        Vector3 tileVec = Vector3.fromBlockPos(pos).add(0.2 + Math.random() * 0.6, 0, 0.2 + Math.random() * 0.6);
                        Botania.proxy.lightningFX(outputting ? tileVec : itemVec,
                                outputting ? itemVec : tileVec, 80, world.rand.nextLong(), 0x4400799c, 0x4400C6FF);
                    }
                }
                return true;
            }
            default: return super.receiveClientEvent(event, param);
        }
    }
    
    public static RecipeManaInfusion getMatchingRecipe(@Nonnull ItemStack stack, @Nonnull IBlockState state) {
        List<RecipeManaInfusion> matchingNonCatRecipes = new ArrayList<>();
        List<RecipeManaInfusion> matchingCatRecipes = new ArrayList<>();

        for (RecipeManaInfusion recipe : BotaniaAPI.manaInfusionRecipes) {
            if (recipe.matches(stack)) {
                if(recipe.getCatalyst() == null)
                    matchingNonCatRecipes.add(recipe);
                else if (recipe.getCatalyst() == state)
                    matchingCatRecipes.add(recipe);
            }
        }

        return !matchingCatRecipes.isEmpty() ? matchingCatRecipes.get(0) :
            !matchingNonCatRecipes.isEmpty() ? matchingNonCatRecipes.get(0) :
                null;
    }
    
    public boolean collideEntityItem(EntityItem item) {
        if(world.isRemote || item.isDead || item.getItem().isEmpty())
            return false;

        ItemStack stack = item.getItem();

        if(stack.getItem() instanceof IManaDissolvable) {
            ((IManaDissolvable) stack.getItem()).onDissolveTick(this, stack, item);
        }

        if(item.age > 100 && item.age < 130)
            return false;

        RecipeManaInfusion recipe = getMatchingRecipe(stack, world.getBlockState(pos.down()));

        if(recipe != null) {
            int mana = recipe.getManaToConsume();
            if(getCurrentMana() >= mana) {
                recieveMana(-mana);

                stack.shrink(1);

                ItemStack output = recipe.getOutput().copy();
                EntityItem outputItem = new EntityItem(world, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, output);
                outputItem.age = 105;
                world.spawnEntity(outputItem);

                craftingFanciness();
                return true;
            }
        }

        return false;
    }
    
    private void craftingFanciness() {
        if(soundTicks == 0) {
            world.playSound(null, pos, ModSounds.manaPoolCraft, SoundCategory.BLOCKS, 0.4F, 4F);
            soundTicks = 6;
        }

        world.addBlockEvent(getPos(), getBlockType(), CRAFT_EFFECT_EVENT, 0);
    }
    
    @Override
    public int getCurrentMana() {
        if(isNetworkConnected && !world.isRemote) {
            long networkMana = getManaFromNetwork();
            return (int) Math.min(networkMana, Integer.MAX_VALUE);
        }
        return mana;
    }
    
    @Override
    public boolean isFull() {
        if(isNetworkConnected && !world.isRemote) {
            try {
                if(gridProxy == null || gridProxy.getNode() == null) {
                    return true;
                }
                IGridNode node = gridProxy.getNode();
                if(!node.isActive() || node.getGrid() == null) {
                    return true;
                }
                
                IGrid grid = node.getGrid();
                IStorageGrid storage = grid.getCache(IStorageGrid.class);
                IMEInventory<nyonio.ae2.ManaStack> inventory = storage.getInventory(nyonio.ae2.ManaStorageChannel.INSTANCE);
                
                nyonio.ae2.ManaStack toInsert = new nyonio.ae2.ManaStack(1);
                nyonio.ae2.ManaStack remaining = inventory.injectItems(toInsert, appeng.api.config.Actionable.SIMULATE, actionSource);
                
                return remaining != null && remaining.getStackSize() > 0;
            } catch(Exception e) {
                return true;
            }
        }
        
        Block blockBelow = world.getBlockState(pos.down()).getBlock();
        return blockBelow != ModBlocks.manaVoid && getCurrentMana() >= manaCap;
    }
    
    @Override
    public void recieveMana(int mana) {
        if(isNetworkConnected && !world.isRemote) {
            int transferred = transferManaToNetwork(mana);
            if(transferred > 0) {
                world.updateComparatorOutputLevel(pos, world.getBlockState(pos).getBlock());
                markDispatchable();
            }
            return;
        }
        
        int old = this.mana;
        this.mana = Math.max(0, Math.min(getCurrentMana() + mana, manaCap));
        if(old != this.mana) {
            world.updateComparatorOutputLevel(pos, world.getBlockState(pos).getBlock());
            markDispatchable();
        }
    }
    
    @Override
    public void invalidate() {
        super.invalidate();
        ManaNetworkEvent.removePool(this);
        if (this.gridProxy != null) {
            this.gridProxy.invalidate();
        }
    }
    
    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        ManaNetworkEvent.removePool(this);
        if (this.gridProxy != null) {
            this.gridProxy.onChunkUnload();
        }
    }
    
    public static int calculateComparatorLevel(int mana, int max) {
        int val = (int) ((double) mana / (double) max * 15.0);
        if(mana > 0)
            val = Math.max(val, 1);
        return val;
    }
    
    public void onWanded(EntityPlayer player, ItemStack wand) {
        if(player == null)
            return;

        if(player.isSneaking()) {
            outputting = !outputting;
            VanillaPacketDispatcher.dispatchTEToNearbyPlayers(world, pos);
        }

        if(!world.isRemote) {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            writePacketNBT(nbttagcompound);
            nbttagcompound.setInteger(TAG_KNOWN_MANA, getCurrentMana());
            if(player instanceof EntityPlayerMP)
                ((EntityPlayerMP) player).connection.sendPacket(new SPacketUpdateTileEntity(pos, -999, nbttagcompound));
        }

        world.playSound(null, player.posX, player.posY, player.posZ, ModSounds.ding, SoundCategory.PLAYERS, 0.11F, 1F);
    }
    
    @SideOnly(Side.CLIENT)
    public void renderHUD(Minecraft mc, ScaledResolution res) {
        String name = I18n.format("tile.botania_applie.fluix_mana_pool.name");
        int color = 0x4444FF;
        
        if(isNetworkConnected) {
            name += " [\u00a7a" + I18n.format("botania_applie.status.online") + "\u00a7r]";
            color = 0x00FF00;
        } else {
            name += " [\u00a77" + I18n.format("botania_applie.status.offline") + "\u00a7r]";
        }
        
        HUDHandler.drawSimpleManaHUD(color, knownMana, manaCap, name, res);

        int x = res.getScaledWidth() / 2 - 11;
        int y = res.getScaledHeight() / 2 + 30;

        int u = outputting ? 22 : 0;
        int v = 38;

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        mc.renderEngine.bindTexture(HUDHandler.manaBar);
        RenderHelper.drawTexturedModalRect(x, y, 0, u, v, 22, 15);
        GlStateManager.color(1F, 1F, 1F, 1F);

        ItemStack tablet = new ItemStack(ModItems.manaTablet);
        ItemManaTablet.setStackCreative(tablet);

        net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(tablet, x - 20, y);
        mc.getRenderItem().renderItemAndEffectIntoGUI(new ItemStack(nyonio.BotaniaApplie.fluixManaPool), x + 26, y);
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();

        GlStateManager.disableLighting();
        GlStateManager.disableBlend();
    }
    
    @Override
    public boolean canRecieveManaFromBursts() {
        return true;
    }
    
    @Override
    public boolean isOutputtingPower() {
        return outputting;
    }
    
    @Override
    public String getInputKey() {
        return inputKey;
    }
    
    @Override
    public String getOutputKey() {
        return outputKey;
    }
    
    @Override
    public boolean canAttachSpark(ItemStack stack) {
        return true;
    }
    
    @Override
    public void attachSpark(ISparkEntity entity) {}
    
    @Override
    public ISparkEntity getAttachedSpark() {
        List sparks = world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos.up(), pos.up().add(1, 1, 1)), Predicates.instanceOf(ISparkEntity.class));
        if(sparks.size() == 1) {
            Entity e = (Entity) sparks.get(0);
            return (ISparkEntity) e;
        }

        return null;
    }
    
    @Override
    public boolean areIncomingTranfersDone() {
        return false;
    }
    
    @Override
    public int getAvailableSpaceForMana() {
        if(isNetworkConnected && !world.isRemote) {
            try {
                if(gridProxy == null || gridProxy.getNode() == null) {
                    return 0;
                }
                IGridNode node = gridProxy.getNode();
                if(!node.isActive() || node.getGrid() == null) {
                    return 0;
                }
                
                IGrid grid = node.getGrid();
                IStorageGrid storage = grid.getCache(IStorageGrid.class);
                IMEInventory<nyonio.ae2.ManaStack> inventory = storage.getInventory(nyonio.ae2.ManaStorageChannel.INSTANCE);
                
                nyonio.ae2.ManaStack toInsert = new nyonio.ae2.ManaStack(Integer.MAX_VALUE);
                nyonio.ae2.ManaStack remaining = inventory.injectItems(toInsert, appeng.api.config.Actionable.SIMULATE, actionSource);
                
                if (remaining == null) {
                    return Integer.MAX_VALUE;
                }
                
                long remainingAmount = remaining.getStackSize();
                long availableSpace = (long) Integer.MAX_VALUE - remainingAmount;
                
                return availableSpace > 0 ? (int) Math.min(availableSpace, Integer.MAX_VALUE) : 0;
            } catch(Exception e) {
                return 0;
            }
        }
        
        int space = Math.max(0, manaCap - getCurrentMana());
        if(space > 0)
            return space;
        else if(world.getBlockState(pos.down()).getBlock() == ModBlocks.manaVoid)
            return manaCap;
        else return 0;
    }
    
    @Override
    public EnumDyeColor getColor() {
        return color;
    }
    
    @Override
    public void setColor(EnumDyeColor color) {
        this.color = color;
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 0b1011);
    }
    
    @Override
    public void markDispatchable() {
        sendPacket = true;
    }
    
    @Override
    public void writePacketNBT(NBTTagCompound tag) {
        tag.setInteger(TAG_MANA, mana);
        tag.setBoolean(TAG_OUTPUTTING, outputting);
        tag.setInteger(TAG_COLOR, color.getMetadata());
        tag.setInteger(TAG_MANA_CAP, manaCap);
        tag.setString(TAG_INPUT_KEY, inputKey);
        tag.setString(TAG_OUTPUT_KEY, outputKey);
        tag.setBoolean(TAG_NETWORK_CONNECTED, isNetworkConnected);
        if (this.gridProxy != null) {
            this.gridProxy.writeToNBT(tag);
        }
    }
    
    @Override
    public void readPacketNBT(NBTTagCompound tag) {
        mana = tag.getInteger(TAG_MANA);
        outputting = tag.getBoolean(TAG_OUTPUTTING);
        color = EnumDyeColor.byMetadata(tag.getInteger(TAG_COLOR));
        manaCap = tag.getInteger(TAG_MANA_CAP);
        if(manaCap <= 0) {
            manaCap = MAX_MANA_FLUIX;
        }
        if(tag.hasKey(TAG_INPUT_KEY))
            inputKey = tag.getString(TAG_INPUT_KEY);
        if(tag.hasKey(TAG_OUTPUT_KEY))
            outputKey = tag.getString(TAG_OUTPUT_KEY);
        if(tag.hasKey(TAG_KNOWN_MANA))
            knownMana = tag.getInteger(TAG_KNOWN_MANA);
        if(tag.hasKey(TAG_NETWORK_CONNECTED))
            isNetworkConnected = tag.getBoolean(TAG_NETWORK_CONNECTED);
        this.getProxy().readFromNBT(tag);
    }
    
    @Nullable
    @Override
    public IGridNode getGridNode(@Nonnull AEPartLocation dir) {
        return this.getProxy().getNode();
    }
    
    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull AEPartLocation dir) {
        return AECableType.SMART;
    }
    
    @Override
    public void gridChanged() {
    }
    
    @Nonnull
    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }
    
    @Nonnull
    @Override
    public IGridNode getActionableNode() {
        return this.getProxy().getNode();
    }
    
    @Override
    public void securityBreak() {
        this.world.destroyBlock(this.pos, true);
    }
    
    private boolean checkNetworkConnection() {
        if(gridProxy == null || gridProxy.getNode() == null) {
            return false;
        }
        IGridNode node = gridProxy.getNode();
        return node.isActive() && node.getGrid() != null;
    }
    
    private int getManaFromNetwork() {
        try {
            if(gridProxy == null || gridProxy.getNode() == null) {
                return 0;
            }
            IGridNode node = gridProxy.getNode();
            if(!node.isActive() || node.getGrid() == null) {
                return 0;
            }
            
            IGrid grid = node.getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            IMEInventory<nyonio.ae2.ManaStack> inventory = storage.getInventory(nyonio.ae2.ManaStorageChannel.INSTANCE);
            
            nyonio.ae2.ManaStack request = new nyonio.ae2.ManaStack(Integer.MAX_VALUE);
            nyonio.ae2.ManaStack extracted = inventory.extractItems(request, appeng.api.config.Actionable.SIMULATE, actionSource);
            
            return extracted != null ? (int) extracted.getStackSize() : 0;
        } catch(Exception e) {
            return mana;
        }
    }
    
    private int transferManaToNetwork(int amount) {
        try {
            if(gridProxy == null || gridProxy.getNode() == null) {
                return 0;
            }
            IGridNode node = gridProxy.getNode();
            if(!node.isActive() || node.getGrid() == null) {
                return 0;
            }
            
            IGrid grid = node.getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            IMEInventory<nyonio.ae2.ManaStack> inventory = storage.getInventory(nyonio.ae2.ManaStorageChannel.INSTANCE);
            
            nyonio.ae2.ManaStack toInsert = new nyonio.ae2.ManaStack(amount);
            nyonio.ae2.ManaStack remaining = inventory.injectItems(toInsert, appeng.api.config.Actionable.MODULATE, actionSource);
            
            int transferred = amount - (remaining != null ? (int) remaining.getStackSize() : 0);
            return transferred;
        } catch(Exception e) {
            return 0;
        }
    }
    
    private void transferLocalManaToNetwork(int amount) {
        try {
            if(gridProxy == null || gridProxy.getNode() == null) {
                return;
            }
            IGridNode node = gridProxy.getNode();
            if(!node.isActive() || node.getGrid() == null) {
                return;
            }
            
            IGrid grid = node.getGrid();
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            IMEInventory<nyonio.ae2.ManaStack> inventory = storage.getInventory(nyonio.ae2.ManaStorageChannel.INSTANCE);
            
            nyonio.ae2.ManaStack toInsert = new nyonio.ae2.ManaStack(amount);
            inventory.injectItems(toInsert, appeng.api.config.Actionable.MODULATE, actionSource);
        } catch(Exception e) {
            // Ignore errors during network transfer
        }
    }
    
    public boolean isNetworkConnected() {
        return isNetworkConnected;
    }
}
