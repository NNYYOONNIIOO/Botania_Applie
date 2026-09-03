package nyonio.item;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import nyonio.entity.EntityChannelSpark;
import nyonio.entity.EntityMainChannelSpark;

public class ItemMainChannelSpark extends ItemChannelSpark {
    public ItemMainChannelSpark() {
        super();
        setUnlocalizedName("botania_applie.main_channel_spark");
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "item.botania_applie.main_channel_spark";
    }

    @Override
    protected EntityChannelSpark createSpark(World world, double x, double y, double z) {
        return new EntityMainChannelSpark(world, x, y, z);
    }
}
