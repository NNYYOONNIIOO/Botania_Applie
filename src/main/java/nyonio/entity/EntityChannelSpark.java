package nyonio.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import nyonio.channel.ChannelSparkNetwork;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityChannelSpark extends Entity {
    private static final String TARGET_PRESENT = "ChannelSparkTargetPresent";
    private static final String TARGET_X = "ChannelSparkTargetX";
    private static final String TARGET_Y = "ChannelSparkTargetY";
    private static final String TARGET_Z = "ChannelSparkTargetZ";

    private BlockPos targetPos;
    private final Map<UUID, ChannelSparkNetwork.Link> links = new HashMap<>();

    public EntityChannelSpark(World world) {
        super(world);
        setSize(0.25F, 0.25F);
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

    public Map<UUID, ChannelSparkNetwork.Link> getLinks() {
        return links;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.getBoolean(TARGET_PRESENT)) {
            targetPos = new BlockPos(compound.getInteger(TARGET_X), compound.getInteger(TARGET_Y), compound.getInteger(TARGET_Z));
        } else {
            targetPos = null;
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
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
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        return false;
    }

}
