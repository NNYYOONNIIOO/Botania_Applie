package nyonio.item;
import net.minecraft.world.World;
import nyonio.entity.EntityChannelSpark;
import nyonio.entity.EntityMainChannelSpark;
public final class ItemMainChannelSpark extends ItemChannelSpark {
    @Override protected EntityChannelSpark createSpark(World world,double x,double y,double z) { return new EntityMainChannelSpark(world,x,y,z); }
}
