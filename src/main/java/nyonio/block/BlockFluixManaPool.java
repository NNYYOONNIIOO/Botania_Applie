package nyonio.block;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import nyonio.BotaniaApplieTab;
import nyonio.tile.TileFluixManaPool;
import vazkii.botania.api.internal.VanillaPacketDispatcher;
import vazkii.botania.api.state.BotaniaStateProps;
import vazkii.botania.api.state.enums.PoolVariant;
import vazkii.botania.api.wand.IWandHUD;
import vazkii.botania.api.wand.IWandable;
import vazkii.botania.client.render.IModelRegister;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BlockFluixManaPool extends net.minecraft.block.Block implements IModelRegister, IWandable, IWandHUD {
    private static final AxisAlignedBB AABB = new AxisAlignedBB(0, 0, 0, 1, 0.5, 1);
    private static final AxisAlignedBB BOTTOM_AABB = new AxisAlignedBB(0, 0, 0, 1, 1/16.0, 1);
    private static final AxisAlignedBB NORTH_AABB = new AxisAlignedBB(0, 0, 15/16.0, 1, 0.5, 1);
    private static final AxisAlignedBB SOUTH_AABB = new AxisAlignedBB(0, 0, 0, 1, 0.5, 1/16.0);
    private static final AxisAlignedBB WEST_AABB = new AxisAlignedBB(0, 0, 0, 1/16.0, 0.5, 1);
    private static final AxisAlignedBB EAST_AABB = new AxisAlignedBB(15/16.0, 0, 0, 1, 0.5, 1);

    public BlockFluixManaPool() {
        super(Material.ROCK);
        setUnlocalizedName("botania_applie.fluix_mana_pool");
        setCreativeTab(BotaniaApplieTab.INSTANCE);
        setHardness(2.0F);
        setResistance(10.0F);
        setSoundType(SoundType.STONE);
        setDefaultState(blockState.getBaseState()
                .withProperty(BotaniaStateProps.POOL_VARIANT, PoolVariant.DEFAULT)
                .withProperty(BotaniaStateProps.COLOR, EnumDyeColor.WHITE));
    }

    @Nonnull
    @Override
    public BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, BotaniaStateProps.POOL_VARIANT, BotaniaStateProps.COLOR);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(BotaniaStateProps.POOL_VARIANT).ordinal();
    }

    @Nonnull
    @Override
    public IBlockState getStateFromMeta(int meta) {
        if (meta > PoolVariant.values().length) {
            meta = 0;
        }
        return getDefaultState().withProperty(BotaniaStateProps.POOL_VARIANT, PoolVariant.values()[meta]);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull World world, @Nonnull IBlockState state) {
        return new TileFluixManaPool();
    }

    @Nonnull
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return AABB;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox, @Nonnull List<AxisAlignedBB> boxes, @Nullable Entity entity, boolean isActualState) {
        addCollisionBoxToList(pos, entityBox, boxes, BOTTOM_AABB);
        addCollisionBoxToList(pos, entityBox, boxes, NORTH_AABB);
        addCollisionBoxToList(pos, entityBox, boxes, SOUTH_AABB);
        addCollisionBoxToList(pos, entityBox, boxes, WEST_AABB);
        addCollisionBoxToList(pos, entityBox, boxes, EAST_AABB);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
        if(entity instanceof EntityItem) {
            TileEntity te = world.getTileEntity(pos);
            if(te instanceof TileFluixManaPool) {
                TileFluixManaPool tile = (TileFluixManaPool) te;
                if(tile.collideEntityItem((EntityItem) entity))
                    VanillaPacketDispatcher.dispatchTEToNearbyPlayers(world, pos);
            }
        }
    }

    @Override
    public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param) {
        super.eventReceived(state, world, pos, id, param);
        TileEntity tileentity = world.getTileEntity(pos);
        return tileentity != null && tileentity.receiveClientEvent(id, param);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileFluixManaPool pool = (TileFluixManaPool) world.getTileEntity(pos);
        return TileFluixManaPool.calculateComparatorLevel(pool.getCurrentMana(), pool.manaCap);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUD(Minecraft mc, ScaledResolution res, World world, BlockPos pos) {
        ((TileFluixManaPool) world.getTileEntity(pos)).renderHUD(mc, res);
    }

    @Override
    public boolean onUsedByWand(EntityPlayer player, ItemStack stack, World world, BlockPos pos, EnumFacing side) {
        ((TileFluixManaPool) world.getTileEntity(pos)).onWanded(player, stack);
        return true;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels() {
        ModelLoader.setCustomStateMapper(this, new StateMap.Builder().ignore(BotaniaStateProps.COLOR).build());
        for (PoolVariant variant : PoolVariant.values()) {
            ModelLoader.setCustomModelResourceLocation(
                net.minecraft.item.Item.getItemFromBlock(this),
                variant.ordinal(),
                new net.minecraft.client.renderer.block.model.ModelResourceLocation(getRegistryName(), "variant=" + variant.getName())
            );
        }
    }
}
