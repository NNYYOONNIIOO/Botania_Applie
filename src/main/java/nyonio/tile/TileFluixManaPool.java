package nyonio.tile;

import appeng.api.config.Actionable;
import appeng.api.networking.*;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEInventory;
import appeng.api.util.*;
import appeng.me.helpers.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.Direction;
import nyonio.BotaniaApplie;
import nyonio.ae2.ManaStack;
import nyonio.ae2.ManaStorageChannel;
import vazkii.botania.api.internal.IManaBurst;
import vazkii.botania.api.internal.VanillaPacketDispatcher;
import vazkii.botania.api.mana.IManaCollector;
import vazkii.botania.api.mana.ManaNetworkEvent;
import vazkii.botania.common.block.tile.mana.TilePool;
import java.util.EnumSet;

/** A Botania pool whose online value is network localMana plus the local buffer. */
public class TileFluixManaPool extends TilePool implements IManaCollector, IGridProxyable, IActionHost {
    public static final int MAX_MANA_FLUIX = Integer.MAX_VALUE;
    private final AENetworkProxy proxy = new AENetworkProxy(this,"grid",new ItemStack(BotaniaApplie.fluixManaPoolItem),true);
    private final MachineSource source = new MachineSource(this);
    private boolean ready;
    private boolean networkConnected;
    private int ticks;
    private int localMana() { return ((nyonio.mixin.TilePoolManaAccessor)(Object)this).botaniaApplie$getMana(); }
    private void localMana(int value) { ((nyonio.mixin.TilePoolManaAccessor)(Object)this).botaniaApplie$setMana(Math.max(0,Math.min(MAX_MANA_FLUIX,value))); }
    private int displayMana;
    private long displayTotal;
    private long displayCapacity=MAX_MANA_FLUIX;
    public TileFluixManaPool() {
        super();
        manaCap=MAX_MANA_FLUIX;
        proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
        proxy.setValidSides(EnumSet.allOf(Direction.class));
    }
    @Override public net.minecraft.tileentity.TileEntityType<?> getType() { return BotaniaApplie.FLUIX_POOL_TILE; }
    @Override public void onLoad() { super.onLoad(); proxy.validate(); }
    @Override public void tick() {
        if (!world.isRemote) {
            if(!ready) { ready=true; proxy.onReady(); ManaNetworkEvent.addCollector(this); }
            networkConnected=proxy.isActive();
            if(networkConnected && localMana()>0) {
                long inserted=insertNetwork(localMana());
                if(inserted>0) { localMana(localMana()-(int)inserted); markDirty(); markDispatchable(); }
            }
            if(ticks++%10==0) updateDisplay();
        }
        super.tick();
    }
    private IMEInventory<ManaStack> inventory() {
        try {
            IGridNode node=proxy.getNode();
            if(node==null || !node.isActive()) return null;
            IStorageGrid storage=node.getGrid().getCache(IStorageGrid.class);
            return storage==null?null:storage.getInventory(ManaStorageChannel.INSTANCE);
        } catch(RuntimeException e) { return null; }
    }
    private long networkMana() {
        IMEInventory<ManaStack> inv=inventory();
        if(inv==null) return 0;
        ManaStack result=inv.extractItems(new ManaStack(Long.MAX_VALUE),Actionable.SIMULATE,source);
        return result==null?0:Math.max(0,result.getStackSize());
    }
    private long networkSpace() {
        IMEInventory<ManaStack> inv=inventory();
        if(inv==null) return 0;
        ManaStack result=inv.injectItems(new ManaStack(Long.MAX_VALUE),Actionable.SIMULATE,source);
        return Math.max(0,Long.MAX_VALUE-(result==null?0:result.getStackSize()));
    }
    private long insertNetwork(long amount) {
        IMEInventory<ManaStack> inv=inventory();
        if(inv==null || amount<=0) return 0;
        ManaStack remainder=inv.injectItems(new ManaStack(amount),Actionable.MODULATE,source);
        return Math.max(0,amount-(remainder==null?0:remainder.getStackSize()));
    }
    private long extractNetwork(long amount) {
        IMEInventory<ManaStack> inv=inventory();
        if(inv==null || amount<=0) return 0;
        ManaStack result=inv.extractItems(new ManaStack(amount),Actionable.MODULATE,source);
        return result==null?0:Math.min(amount,result.getStackSize());
    }
    @Override public int getCurrentMana() { return world!=null && world.isRemote?displayMana:clamp(add(localMana(),networkMana())); }
    @Override public int getMaxMana() { return MAX_MANA_FLUIX; }
    @Override public boolean isFull() { return localMana()>=MAX_MANA_FLUIX && networkSpace()==0; }
    @Override public int getAvailableSpaceForMana() { return clamp(add((long)MAX_MANA_FLUIX-localMana(),networkSpace())); }
    @Override public void receiveMana(int amount) {
        if(world==null || world.isRemote || amount==0) return;
        if(amount>0) {
            long remaining=(long)amount-insertNetwork(amount);
            localMana(localMana()+(int)Math.min(remaining,(long)MAX_MANA_FLUIX-localMana()));
        } else {
            long requested=-(long)amount;
            int local=(int)Math.min(requested,Math.max(0,localMana()));
            localMana(localMana()-local);
            extractNetwork(requested-local);
        }
        markDirty(); markDispatchable();
        world.updateComparatorOutputLevel(pos,getBlockState().getBlock());
    }
    private void updateDisplay() {
        long net=networkMana();
        displayTotal=add(Math.max(0,localMana()),net);
        displayCapacity=add(MAX_MANA_FLUIX,add(net,networkSpace()));
        displayMana=clamp(displayTotal);
        VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
    }
    @Override public void writePacketNBT(CompoundNBT tag) {
        super.writePacketNBT(tag);
        proxy.writeToNBT(tag);
        tag.putLong("displayTotal",displayTotal);
        tag.putLong("displayCapacity",displayCapacity);
        tag.putInt("displayMana",displayMana);
    }
    @Override public void readPacketNBT(CompoundNBT tag) {
        super.readPacketNBT(tag); manaCap=MAX_MANA_FLUIX;
        proxy.readFromNBT(tag);
        displayTotal=tag.getLong("displayTotal");
        displayCapacity=tag.contains("displayCapacity")?tag.getLong("displayCapacity"):MAX_MANA_FLUIX;
        displayMana=tag.getInt("displayMana");
    }
    public long getProbeCurrentMana() { return world!=null && world.isRemote?displayTotal:add(Math.max(0,localMana()),networkMana()); }
    public long getProbeMaxMana() { long net=networkMana();return world!=null && world.isRemote?displayCapacity:add(MAX_MANA_FLUIX,add(net,networkSpace())); }
    public int getRenderMana() { return displayMana; }
    public int getRenderManaCap() { return MAX_MANA_FLUIX; }
    public int getComparatorLevel() { return (int)Math.min(15,Math.ceil(15.0*getProbeCurrentMana()/Math.max(1,getProbeMaxMana()))); }
    public boolean isNetworkConnected() { return networkConnected; }
    @Override public void onClientDisplayTick() {}
    @Override public float getManaYieldMultiplier(IManaBurst burst) { return 1F; }
    @Override public AENetworkProxy getProxy() { return proxy; }
    @Override public IGridNode getGridNode(AEPartLocation side) { return proxy.getNode(); }
    @Override public IGridNode getActionableNode() { return proxy.getNode(); }
    @Override public AECableType getCableConnectionType(AEPartLocation side) { return AECableType.SMART; }
    @Override public DimensionalCoord getLocation() { return new DimensionalCoord(this); }
    @Override public void gridChanged() { markDispatchable(); }
    @Override public void saveChanges() { markDirty(); }
    @Override public void securityBreak() { if(world!=null) world.destroyBlock(pos,true); }
    @Override public void remove() { proxy.remove();ManaNetworkEvent.removeCollector(this);super.remove(); }
    @Override public void onChunkUnloaded() { proxy.onChunkUnloaded();ManaNetworkEvent.removeCollector(this);super.onChunkUnloaded(); }
    private static int clamp(long value) { return (int)Math.min(MAX_MANA_FLUIX,Math.max(0,value)); }
    private static long add(long a,long b) { a=Math.max(0,a);b=Math.max(0,b);return a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b; }
}


