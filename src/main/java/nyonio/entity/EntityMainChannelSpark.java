package nyonio.entity;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import nyonio.BotaniaApplie;
public class EntityMainChannelSpark extends EntityChannelSpark {
    public EntityMainChannelSpark(EntityType<? extends EntityMainChannelSpark> type,World world) { super(type,world); }
    public EntityMainChannelSpark(World world,double x,double y,double z) { this(BotaniaApplie.MAIN_CHANNEL_SPARK,world);setPosition(x,y,z); }
    @Override public boolean isMainChannelSpark() { return true; }
}
