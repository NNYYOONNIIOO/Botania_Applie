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
import vazkii.botania.api.internal.IManaBurst;
import vazkii.botania.api.internal.VanillaPacketDispatcher;
import vazkii.botania.api.item.IManaDissolvable;
import vazkii.botania.api.mana.IKeyLocked;
import vazkii.botania.api.mana.IManaCollector;
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

public class TileFluixManaPool extends TileMod implements IManaPool, IManaCollector, IKeyLocked, ISparkAttachable, IThrottledPacket, net.minecraft.util.ITickable, IGridProxyable, IActionHost {
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
    
    // 网络缓存：避免每次getter都查询网络，同时正确处理容量同步
    private long cachedNetworkMana = 0;
    private long cachedNetworkAvailable = 0;
    private boolean cachedNetworkFull = false;
    private boolean cacheValid = false;


    // 计算用于客户端显示的 mana 值
    // 溢出时 getCurrentMana() 返回 0（为产能花传输优化），但显示需要反映实际填充比例
    private int calculateDisplayMana() {
        if(!cacheValid) return mana;
        if(cachedNetworkFull) return (int) Math.min(cachedNetworkMana, Integer.MAX_VALUE);
        if(cachedNetworkMana + cachedNetworkAvailable <= Integer.MAX_VALUE) {
            return (int) cachedNetworkMana;
        }
        // 溢出情况：用比例计算显示值，使水面显示接近满
        long total = cachedNetworkMana + cachedNetworkAvailable;
        return (int) (getMaxMana() * ((double) cachedNetworkMana / total));
    }

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
            boolean connectionChanged = (wasConnected != isNetworkConnected);
            
            if(connectionChanged) {
                if(isNetworkConnected && mana > 0) {
                    transferLocalManaToNetwork(mana);
                    // 不设 mana = 0，让下面的缓存刷新块来更新 mana
                } else if(!isNetworkConnected) {
                    mana = 0;
                    manaCap = MAX_MANA_FLUIX;
                    cacheValid = false;
                }
                markDispatchable();
            } else if(ticks % 60 == 0) {
                VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
            }
            
            if(isNetworkConnected) {
                refreshNetworkCache();
                int newMana = calculateDisplayMana();
                int newCap = getMaxMana();
                // 连接状态变化时强制发包，确保客户端收到正确的 mana 和 manaCap
                if(mana != newMana || manaCap != newCap || connectionChanged) {
                    mana = newMana;
                    manaCap = newCap;
                    VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
                }
            }
        }
        
        if(!ManaNetworkHandler.instance.isPoolIn(this) && !isInvalid()) {
            ManaNetworkEvent.addPool(this);
        }
        if(!ManaNetworkHandler.instance.isCollectorIn(this) && !isInvalid()) {
            ManaNetworkEvent.addCollector(this);
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
            if(isNetworkConnected && !world.isRemote) {
                if(!cacheValid) refreshNetworkCache();
                if(cacheValid) {
                    int newMana = calculateDisplayMana();
                    int newCap = getMaxMana();
                    if(mana != newMana || manaCap != newCap) {
                        mana = newMana;
                        manaCap = newCap;
                    }
                }
            }
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
                        // 网络模式下使用缓存判断是否有魔力可用（溢出时 getCurrentMana() 返回 0）
                        boolean hasMana = isNetworkConnected ? (cacheValid && cachedNetworkMana > 0) : (getCurrentMana() > 0);
                        int availableMana = isNetworkConnected ? (int) Math.min(cachedNetworkMana, Integer.MAX_VALUE) : getCurrentMana();
                        if(hasMana && manaItem.getMana(stack) < manaItem.getMaxMana(stack)) {
                            didSomething = true;
                            int manaVal = Math.min(transfRate, Math.min(availableMana, manaItem.getMaxMana(stack) - manaItem.getMana(stack)));
                            manaItem.addMana(stack, manaVal);
                            recieveMana(-manaVal);
                        }
                    } else {
                        if(manaItem.getMana(stack) > 0 && !isFull()) {
                            didSomething = true;
                            int manaVal = Math.min(transfRate, Math.min(getAvailableSpaceForMana(), manaItem.getMana(stack)));
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
            boolean canCraft;
            if(isNetworkConnected && !world.isRemote) {
                // 网络模式下直接查询缓存，不依赖 getCurrentMana()（溢出时 getCurrentMana() 返回 0）
                if(!cacheValid) refreshNetworkCache();
                canCraft = cacheValid && cachedNetworkMana >= mana;
            } else {
                canCraft = getCurrentMana() >= mana;
            }
            if(canCraft) {
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
            if(!cacheValid) refreshNetworkCache();
            if(!cacheValid) return mana;
            if(cachedNetworkFull) {
                return (int) Math.min(cachedNetworkMana, Integer.MAX_VALUE);
            }
            // 精确计算：当 mana + available <= MAX_INT 时可以直接返回
            if(cachedNetworkMana + cachedNetworkAvailable <= Integer.MAX_VALUE) {
                return (int) cachedNetworkMana;
            }
            // 溢出情况：mana + available > MAX_INT
            // 用比例计算，使产能花输入和火花/发射器输出都能正常工作
            // getMaxMana() - getCurrentMana() 按比例反映可用空间
            long total = cachedNetworkMana + cachedNetworkAvailable;
            return (int) (getMaxMana() * ((double) cachedNetworkMana / total));
        }
        return mana;
    }
    
    @Override
    public boolean isFull() {
        if(isNetworkConnected && !world.isRemote) {
            if(!cacheValid) refreshNetworkCache();
            if(cacheValid) return cachedNetworkFull;
            // 缓存无效时使用本地缓存值判断，避免阻止产能花传输
            return getCurrentMana() >= getMaxMana();
        }
        
        Block blockBelow = world.getBlockState(pos.down()).getBlock();
        return blockBelow != ModBlocks.manaVoid && getCurrentMana() >= manaCap;
    }
    
    @Override
    public void recieveMana(int mana) {
        if(isNetworkConnected && !world.isRemote) {
            if(!cacheValid) refreshNetworkCache();
            if(mana >= 0) {
                // 正值：向网络注入魔力
                int transferred = transferManaToNetwork(mana);
                if(transferred > 0) {
                    if(cacheValid) {
                        cachedNetworkMana += transferred;
                        cachedNetworkAvailable = Math.max(0, cachedNetworkAvailable - transferred);
                        cachedNetworkFull = (cachedNetworkAvailable == 0);
                    } else {
                        refreshNetworkCache();
                    }
                    this.mana = calculateDisplayMana();
                    world.updateComparatorOutputLevel(pos, world.getBlockState(pos).getBlock());
                    markDispatchable();
                }
            } else {
                // 负值：从网络提取魔力（合成消耗等场景）
                int toExtract = -mana;
                int extracted = extractManaFromNetwork(toExtract);
                if(extracted > 0) {
                    if(cacheValid) {
                        cachedNetworkMana = Math.max(0, cachedNetworkMana - extracted);
                        cachedNetworkAvailable += extracted;
                        cachedNetworkFull = false;
                    } else {
                        refreshNetworkCache();
                    }
                    this.mana = calculateDisplayMana();
                    world.updateComparatorOutputLevel(pos, world.getBlockState(pos).getBlock());
                    markDispatchable();
                }
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
        ManaNetworkEvent.removeCollector(this);
        if (this.gridProxy != null) {
            this.gridProxy.invalidate();
        }
    }
    
    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        ManaNetworkEvent.removePool(this);
        ManaNetworkEvent.removeCollector(this);
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
            nbttagcompound.setInteger(TAG_KNOWN_MANA, calculateDisplayMana());
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
    public void onClientDisplayTick() {
    }

    @Override
    public float getManaYieldMultiplier(IManaBurst burst) {
        return 1.0F;
    }

    @Override
    public int getMaxMana() {
        if(isNetworkConnected && !world.isRemote) {
            if(!cacheValid) refreshNetworkCache();
            if(!cacheValid) return manaCap;
            if(cachedNetworkFull) {
                return (int) Math.min(cachedNetworkMana, Integer.MAX_VALUE);
            }
            // 实际容量 = 当前魔力 + 可用空间，同步内部存储容量
            long total = cachedNetworkMana + cachedNetworkAvailable;
            return (int) Math.min(total, Integer.MAX_VALUE);
        }
        return manaCap;
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
            if(!cacheValid) refreshNetworkCache();
            if(cacheValid) {
                return (int) Math.min(cachedNetworkAvailable, Integer.MAX_VALUE);
            }
            return 0;
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
    
    private void refreshNetworkCache() {
        cacheValid = false;
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
            
            // 查询当前魔力（extractItems SIMULATE，受Integer.MAX_VALUE上限）
            nyonio.ae2.ManaStack request = new nyonio.ae2.ManaStack(Integer.MAX_VALUE);
            nyonio.ae2.ManaStack extracted = inventory.extractItems(request, appeng.api.config.Actionable.SIMULATE, actionSource);
            cachedNetworkMana = extracted != null ? extracted.getStackSize() : 0;
            
            // 查询可用空间（injectItems SIMULATE）
            nyonio.ae2.ManaStack toInsert = new nyonio.ae2.ManaStack(Integer.MAX_VALUE);
            nyonio.ae2.ManaStack remaining = inventory.injectItems(toInsert, appeng.api.config.Actionable.SIMULATE, actionSource);
            long rejected = remaining != null ? remaining.getStackSize() : 0;
            cachedNetworkAvailable = Math.max(0, (long) Integer.MAX_VALUE - rejected);

            cachedNetworkFull = (cachedNetworkAvailable == 0);
            cacheValid = true;
        } catch(Exception e) {
            cacheValid = false;
        }
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
            
            int transferred = amount - (remaining != null ? (int) Math.min(remaining.getStackSize(), amount) : 0);
            return transferred;
        } catch(Exception e) {
            return 0;
        }
    }
    
    private int extractManaFromNetwork(int amount) {
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
            
            nyonio.ae2.ManaStack request = new nyonio.ae2.ManaStack(amount);
            nyonio.ae2.ManaStack extracted = inventory.extractItems(request, appeng.api.config.Actionable.MODULATE, actionSource);
            
            return extracted != null ? (int) Math.min(extracted.getStackSize(), amount) : 0;
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
