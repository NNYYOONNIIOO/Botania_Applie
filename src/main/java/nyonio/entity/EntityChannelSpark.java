package nyonio.entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import nyonio.BotaniaApplie;
import nyonio.channel.ChannelSparkNetwork;
import vazkii.botania.common.entity.EntitySparkBase;
import vazkii.botania.common.item.ModItems;
import java.util.*;
public class EntityChannelSpark extends EntitySparkBase {
    private static long sequence;
    private long placementOrder=++sequence;
    private BlockPos target;
    private long lastWandTick=Long.MIN_VALUE;
    private UUID lastPlayer;
    private final Map<UUID,ChannelSparkNetwork.Link> links=new HashMap<>();
    public EntityChannelSpark(EntityType<? extends EntityChannelSpark> type,World world) { super(type,world);setNoGravity(true); }
    public EntityChannelSpark(World world,double x,double y,double z) { this(BotaniaApplie.CHANNEL_SPARK,world);setPosition(x,y,z); }
    public boolean isMainChannelSpark() { return false; }
    public boolean hasTarget() { return target!=null; }
    public BlockPos getTargetPos() { return target; }
    public void setTarget(BlockPos pos) { target=pos.toImmutable(); }
    public BlockPos getContainingBlock() { return getPosition(); }
    public long getPlacementOrder() { return placementOrder; }
    public Map<UUID,ChannelSparkNetwork.Link> getLinks() { return links; }
    @Override public void tick() { super.tick();if(!world.isRemote && ticksExisted==1) ChannelSparkNetwork.registerSpark(this); }
    @Override protected void readAdditional(CompoundNBT tag) { super.readAdditional(tag);if(tag.contains("ChannelSparkPlacementOrder")) placementOrder=tag.getLong("ChannelSparkPlacementOrder");sequence=Math.max(sequence,placementOrder);target=tag.getBoolean("ChannelSparkTargetPresent")?new BlockPos(tag.getInt("ChannelSparkTargetX"),tag.getInt("ChannelSparkTargetY"),tag.getInt("ChannelSparkTargetZ")):null; }
    @Override protected void writeAdditional(CompoundNBT tag) { super.writeAdditional(tag);tag.putLong("ChannelSparkPlacementOrder",placementOrder);tag.putBoolean("ChannelSparkTargetPresent",target!=null);if(target!=null) {tag.putInt("ChannelSparkTargetX",target.getX());tag.putInt("ChannelSparkTargetY",target.getY());tag.putInt("ChannelSparkTargetZ",target.getZ());} }
    @Override public void remove() { if(!world.isRemote) {ChannelSparkNetwork.clear(this);ChannelSparkNetwork.unregisterSpark(this);}super.remove(); }
    @Override public ItemStack getPickedResult(RayTraceResult hit) { return new ItemStack(isMainChannelSpark()?BotaniaApplie.mainChannelSpark:BotaniaApplie.channelSpark); }
    @Override public boolean canBePushed() { return false; }
    @Override public float getCollisionBorderSize() { return 0.2F; }
    @Override public ActionResultType applyPlayerInteraction(PlayerEntity player,Vector3d hit,Hand hand) { return interactWithTwigWand(player,hand)?ActionResultType.SUCCESS:ActionResultType.PASS; }
    @Override public ActionResultType processInitialInteract(PlayerEntity player,Hand hand) { return interactWithTwigWand(player,hand)?ActionResultType.SUCCESS:ActionResultType.PASS; }
    public boolean interactWithTwigWand(PlayerEntity player,Hand hand) {
        if(player.getHeldItem(hand).getItem()!=ModItems.twigWand) return false;
        if(world.isRemote) {player.swingArm(hand);return true;}
        if(lastWandTick==world.getGameTime() && player.getUniqueID().equals(lastPlayer)) return true;
        lastWandTick=world.getGameTime();lastPlayer=player.getUniqueID();
        if(player.isSneaking()) {entityDropItem(getPickedResult(null));remove();} else ChannelSparkNetwork.showNetwork(player,this);
        return true;
    }
    @Override public IPacket<?> createSpawnPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
