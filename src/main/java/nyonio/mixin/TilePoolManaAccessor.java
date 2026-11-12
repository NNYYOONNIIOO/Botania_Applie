package nyonio.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import vazkii.botania.common.block.tile.mana.TilePool;

@Mixin(value = TilePool.class, remap = false)
public interface TilePoolManaAccessor {
    @Accessor("mana")
    int botaniaApplie$getMana();

    @Accessor("mana")
    void botaniaApplie$setMana(int mana);
}
