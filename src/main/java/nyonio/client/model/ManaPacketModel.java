package nyonio.client.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import nyonio.BotaniaApplie;
import nyonio.item.ItemManaPacket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.lwjgl.util.vector.Vector3f;
import org.apache.commons.lang3.tuple.Pair;
import javax.vecmath.Matrix4f;

public class ManaPacketModel implements IModel {

    private static final ResourceLocation MODEL_LOCATION = new ResourceLocation(BotaniaApplie.MODID, "models/item/mana_packet");

    @SuppressWarnings("deprecation")
    protected static final ItemCameraTransforms CAMERA_TRANSFORMS = new ItemCameraTransforms(
            new ItemTransformVec3f(new Vector3f(0F, 0F, 0F), new Vector3f(0F, 0.1875F, 0.0625F), new Vector3f(0.55F, 0.55F, 0.55F)),
            new ItemTransformVec3f(new Vector3f(0F, 0F, 0F), new Vector3f(0F, 0.1875F, 0.0625F), new Vector3f(0.55F, 0.55F, 0.55F)),
            new ItemTransformVec3f(new Vector3f(0F, -90F, 25F), new Vector3f(0.070625F, 0.2F, 0.070625F), new Vector3f(0.68F, 0.68F, 0.68F)),
            new ItemTransformVec3f(new Vector3f(0F, -90F, 25F), new Vector3f(0.070625F, 0.2F, 0.070625F), new Vector3f(0.68F, 0.68F, 0.68F)),
            new ItemTransformVec3f(new Vector3f(0F, 180F, 0F), new Vector3f(0F, 0.8125F, 0.4375F), new Vector3f(1F, 1F, 1F)),
            ItemTransformVec3f.DEFAULT,
            new ItemTransformVec3f(new Vector3f(0F, 0F, 0F), new Vector3f(0F, 0.125F, 0F), new Vector3f(0.5F, 0.5F, 0.5F)),
            new ItemTransformVec3f(new Vector3f(0F, 180F, 0F), new Vector3f(0F, 0F, 0F), new Vector3f(1F, 1F, 1F)));

    @Override
    @Nonnull
    public IBakedModel bake(@Nonnull final IModelState state, @Nonnull final VertexFormat format, @Nonnull final Function<ResourceLocation, TextureAtlasSprite> textureBakery) {
        return new BakedManaPacketModel(state, format);
    }

    public static class Loader implements ICustomModelLoader {

        @Override
        public void onResourceManagerReload(@Nonnull final IResourceManager resourceManager) {
        }

        @Override
        public boolean accepts(final ResourceLocation modelLocation) {
            return modelLocation.compareTo(MODEL_LOCATION) == 0;
        }

        @Override
        @Nonnull
        public IModel loadModel(@Nonnull final ResourceLocation modelLocation) {
            return new ManaPacketModel();
        }
    }

    protected static class BakedManaPacketModel implements IBakedModel {

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        protected final Optional<TRSRTransformation> modelTransform;
        protected final VertexFormat vertexFormat;
        protected final ItemOverrideList overrides;
        private final IBakedModel defaultOverride;

        public BakedManaPacketModel(final IModelState modelState, final VertexFormat vertexFormat) {
            this.modelTransform = modelState.apply(Optional.empty());
            this.vertexFormat = vertexFormat;
            this.overrides = this.genOverrides();
            this.defaultOverride = this.genDefaultOverrides();
        }

        @Override
        @Nonnull
        public List<BakedQuad> getQuads(@Nullable final IBlockState state, @Nullable final EnumFacing side, final long rand) {
            return defaultOverride.getQuads(state, side, rand);
        }

        @Override
        public boolean isAmbientOcclusion() {
            return defaultOverride.isAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return defaultOverride.isGui3d();
        }

        @Override
        public boolean isBuiltInRenderer() {
            return defaultOverride.isBuiltInRenderer();
        }

        @Override
        @Nonnull
        public TextureAtlasSprite getParticleTexture() {
            return defaultOverride.getParticleTexture();
        }

        @Override
        public boolean isAmbientOcclusion(@Nonnull final IBlockState state) {
            return defaultOverride.isAmbientOcclusion(state);
        }

        @SuppressWarnings("deprecation")
        @Override
        @Nonnull
        public ItemCameraTransforms getItemCameraTransforms() {
            return defaultOverride.getItemCameraTransforms();
        }

        @Override
        @Nonnull
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(@Nonnull final ItemCameraTransforms.TransformType cameraTransformType) {
            return defaultOverride.handlePerspective(cameraTransformType);
        }

        @Override
        @Nonnull
        public ItemOverrideList getOverrides() {
            return overrides;
        }

        protected ItemOverrideList genOverrides() {
            return new OverrideCache();
        }

        IBakedModel genDefaultOverrides() {
            TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
                    .getAtlasSprite(BotaniaApplie.MODID + ":blocks/mana_packet");
            return new OverrideCache().createDefaultModel(sprite);
        }

        private class OverrideCache extends ItemOverrideList {

            private final TextureAtlasSprite defaultSprite;

            OverrideCache() {
                super(Collections.emptyList());
                this.defaultSprite = Minecraft.getMinecraft().getTextureMapBlocks()
                        .getAtlasSprite(BotaniaApplie.MODID + ":blocks/mana_packet");
            }

            IBakedModel createDefaultModel(TextureAtlasSprite sprite) {
                return new OverrideModel(sprite);
            }

            @Override
            @Nonnull
            public IBakedModel handleItemState(@Nonnull final IBakedModel originalModel, final ItemStack stack,
                                               @Nullable final World world, @Nullable final EntityLivingBase entity) {
                if (!ItemManaPacket.isManaPacket(stack)) {
                    return originalModel;
                }
                TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
                        .getAtlasSprite(BotaniaApplie.MODID + ":blocks/mana_packet");
                return new OverrideModel(sprite);
            }

            class OverrideModel implements IBakedModel {

                private final TextureAtlasSprite texture;
                private final List<BakedQuad> quads;

                OverrideModel(final TextureAtlasSprite texture) {
                    this.texture = texture;
                    this.quads = ItemLayerModel.getQuadsForSprite(1, texture, vertexFormat, modelTransform);
                }

                @Override
                @Nonnull
                public List<BakedQuad> getQuads(@Nullable final IBlockState state, @Nullable final EnumFacing side, final long rand) {
                    return quads;
                }

                @Override
                public boolean isAmbientOcclusion() {
                    return false;
                }

                @Override
                public boolean isGui3d() {
                    return false;
                }

                @Override
                public boolean isBuiltInRenderer() {
                    return false;
                }

                @Override
                @Nonnull
                public TextureAtlasSprite getParticleTexture() {
                    return texture;
                }

                @Override
                @Nonnull
                @SuppressWarnings("deprecation")
                public ItemCameraTransforms getItemCameraTransforms() {
                    return CAMERA_TRANSFORMS;
                }

                @Override
                @Nonnull
                public ItemOverrideList getOverrides() {
                    return OverrideCache.this;
                }
            }
        }
    }
}
