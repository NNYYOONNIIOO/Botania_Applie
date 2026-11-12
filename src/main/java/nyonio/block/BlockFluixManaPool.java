package nyonio.block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import nyonio.tile.TileFluixManaPool;
import vazkii.botania.common.block.mana.BlockPool;
public class BlockFluixManaPool extends BlockPool {
    public BlockFluixManaPool() { super(Variant.DEFAULT,Properties.create(Material.ROCK).hardnessAndResistance(2F)); }
    @Override public TileEntity createNewTileEntity(IBlockReader world) { return new TileFluixManaPool(); }
    @Override public boolean hasTileEntity(BlockState state) { return true; }
    @Override public TileEntity createTileEntity(BlockState state,IBlockReader world) { return new TileFluixManaPool(); }
    @Override public int getComparatorInputOverride(BlockState state,World world,BlockPos pos) {
        TileEntity tile=world.getTileEntity(pos);
        return tile instanceof TileFluixManaPool?((TileFluixManaPool)tile).getComparatorLevel():0;
    }
}
