package nyonio.client.render;

import java.awt.Dimension;
import mcp.mobius.waila.api.ICommonAccessor;
import mcp.mobius.waila.api.ITooltipRenderer;
import mcp.mobius.waila.api.RenderContext;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nyonio.integration.jade.ManaPoolData;

@OnlyIn(Dist.CLIENT)
public final class ManaBarTooltipRenderer implements ITooltipRenderer {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 7;

    @Override
    public Dimension getSize(CompoundNBT data, ICommonAccessor accessor) {
        return new Dimension(WIDTH, HEIGHT + 2);
    }

    @Override
    public void draw(CompoundNBT data, ICommonAccessor accessor, int x, int y) {
        AbstractGui.fill(RenderContext.matrixStack, x, y, x + WIDTH, y + HEIGHT, 0xFF606060);
        AbstractGui.fill(RenderContext.matrixStack, x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, 0xFF161B20);
        int filled = ManaPoolData.read(data).filledWidth(WIDTH - 2);
        if (filled > 0) {
            AbstractGui.fill(RenderContext.matrixStack, x + 1, y + 1, x + 1 + filled, y + HEIGHT - 1, 0xFF0088CC);
            AbstractGui.fill(RenderContext.matrixStack, x + 1, y + 1, x + 1 + filled, y + 3, 0xFF00C6FF);
        }
    }
}
