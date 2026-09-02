package nyonio.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import nyonio.channel.ChannelSparkNetwork;
import vazkii.botania.common.item.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityChannelSpark extends Entity {
    private static final String TARGET_PRESENT = "ChannelSparkTargetPresent";
    private static final String TARGET_X = "ChannelSparkTargetX";
    private static final String TARGET_Y = "ChannelSparkTargetY";
    private static final String TARGET_Z = "ChannelSparkTargetZ";
    private static final String PLACEMENT_ORDER = "ChannelSparkPlacementOrder";

    private static long placementSequence;

    private BlockPos targetPos;
    private long placementOrder;
    private final Map<UUID, ChannelSparkNetwork.Link> links = new HashMap<>();

    public EntityChannelSpark(World world) {
        super(world);
        isImmuneToFire = true;
        setNoGravity(true);
    }

    public EntityChannelSpark(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
    }

    public boolean hasTarget() {
        return targetPos != null;
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public void setTarget(BlockPos targetPos) {
        this.targetPos = targetPos;
    }

    public BlockPos getContainingBlock() {
        return new BlockPos(MathHelper.floor(posX), MathHelper.floor(posY), MathHelper.floor(posZ));
    }

    public long getPlacementOrder() {
        return placementOrder;
    }

    public Map<UUID, ChannelSparkNetwork.Link> getLinks() {
        return links;
    }

    @Override
    protected void entityInit() {
        setSize(0.1F, 0.5F);
        placementOrder = nextPlacementOrder();
    }

    private static synchronized long nextPlacementOrder() {
        return ++placementSequence;
    }

    private static synchronized void observePlacementOrder(long order) {
        if (order > placementSequence) {
            placementSequence = order;
        }
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasKey(PLACEMENT_ORDER)) {
            placementOrder = compound.getLong(PLACEMENT_ORDER);
            observePlacementOrder(placementOrder);
        }
        if (compound.getBoolean(TARGET_PRESENT)) {
            targetPos = new BlockPos(compound.getInteger(TARGET_X), compound.getInteger(TARGET_Y), compound.getInteger(TARGET_Z));
        } else {
            targetPos = null;
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setLong(PLACEMENT_ORDER, placementOrder);
        compound.setBoolean(TARGET_PRESENT, targetPos != null);
        if (targetPos != null) {
            compound.setInteger(TARGET_X, targetPos.getX());
            compound.setInteger(TARGET_Y, targetPos.getY());
            compound.setInteger(TARGET_Z, targetPos.getZ());
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote) {
            ChannelSparkNetwork.tick(this);
        }
    }

    @Override
    public void setDead() {
        if (!world.isRemote) {
            ChannelSparkNetwork.clear(this);
        }
        super.setDead();
    }

    @Override
    public ItemStack getPickedResult(net.minecraft.util.math.RayTraceResult target) {
        return new ItemStack(nyonio.BotaniaApplie.channelSpark);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.isEmpty() || stack.getItem() != ModItems.twigWand) {
            return false;
        }

        if (world.isRemote) {
            player.swingArm(hand);
            return true;
        }

        if (player.isSneaking()) {
            entityDropItem(new ItemStack(nyonio.BotaniaApplie.channelSpark), 0.0F);
            setDead();
        } else {
            ChannelSparkNetwork.showNetwork(player, this);
        }
        return true;
    }

}
