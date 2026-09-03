package nyonio.entity;

import net.minecraft.world.World;

public class EntityMainChannelSpark extends EntityChannelSpark {
    public EntityMainChannelSpark(World world) {
        super(world);
    }

    public EntityMainChannelSpark(World world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    public boolean isMainChannelSpark() {
        return true;
    }
}
