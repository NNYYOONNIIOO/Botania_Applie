package nyonio.recipe;

import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import nyonio.BotaniaApplie;
import nyonio.ModConfig;

public final class ConfigCondition implements ICondition {
    private final String option;
    public ConfigCondition(String option) { this.option=option; }
    @Override public ResourceLocation getID() { return BotaniaApplie.id("config"); }
    @Override public boolean test() { return "components".equals(option)?ModConfig.ENABLE_COMPONENTS.get():ModConfig.ENABLE_HOUSING.get(); }
    public static class Serializer implements IConditionSerializer<ConfigCondition> {
        @Override public void write(JsonObject json,ConfigCondition value) { json.addProperty("option",value.option); }
        @Override public ConfigCondition read(JsonObject json) { return new ConfigCondition(json.get("option").getAsString()); }
        @Override public ResourceLocation getID() { return BotaniaApplie.id("config"); }
    }
}
